package com.tramchester.graph.graphbuild;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.NodeTypeRepository;
import com.tramchester.repository.TransportData;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.GTFSPickupDropoffType.Regular;
import static com.tramchester.graph.TransportRelationshipTypes.LINKED;
import static com.tramchester.graph.graphbuild.GraphProps.*;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.OUTGOING;

@LazySingleton
public class StationsAndLinksGraphBuilder extends GraphBuilder {
    private static final Logger logger = LoggerFactory.getLogger(StationsAndLinksGraphBuilder.class);

    ///
    // Create Station, RouteStations
    //
    // Station-[linked]->Station
    ///

    private final TransportData transportData;

    public Ready getReady() {
        return new Ready();
    }

    @Inject
    public StationsAndLinksGraphBuilder(GraphDatabase graphDatabase, TramchesterConfig config, GraphFilter graphFilter,
                                        NodeTypeRepository nodeIdLabelMap, TransportData transportData,
                                        GraphBuilderCache builderCache) {
        super(graphDatabase, graphFilter, config, builderCache, nodeIdLabelMap);
        this.transportData = transportData;
    }

    public void start() {
        logger.info("start");
        if (graphDatabase.isCleanDB()) {
            logger.info("Rebuild of Stations, RouteStations and Links graph DB for " + config.getGraphName());
            if (graphFilter.isFiltered()) {
                buildGraphwithFilter(graphFilter, graphDatabase, builderCache);
            } else {
                buildGraph(graphDatabase, builderCache);
            }
            logger.info("Graph rebuild is finished for " + config.getGraphName());
        } else {
            logger.info("No rebuild of graph, using existing data");
        }
    }

    @PostConstruct
    public void run() {
        start();
    }

    @Deprecated
    @Override
    protected void buildGraph(GraphDatabase graphDatabase, GraphBuilderCache builderCache) {
        buildGraphwithFilter(new IncludeAllFilter(), graphDatabase, builderCache);
    }

    @Override
    protected void buildGraphwithFilter(GraphFilter graphFilter, GraphDatabase graphDatabase, GraphBuilderCache builderCache) {
        logger.info("Building graph for feedinfo: " + transportData.getDataSourceInfo());
        logMemory("Before graph build");
        long start = System.currentTimeMillis();

        try {
            logger.info("Rebuilding the graph...");

            for(Agency agency : transportData.getAgencies()) {
                logger.info("Adding agency " + agency.getId());
                Stream<Route> routes = agency.getRoutes().stream().filter(graphFilter::shouldInclude);
                buildGraphForRoutes(graphDatabase, graphFilter, routes, builderCache);
                logger.info("Finished agency " + agency.getId());
            }

            long duration = System.currentTimeMillis()-start;
            logger.info("Graph rebuild finished, took " + duration);

        } catch (Exception except) {
            logger.error("Exception while rebuilding the graph", except);
            throw new RuntimeException("Unable to build graph", except);
        }
        reportStats();
        logMemory("After graph build");
        System.gc();
    }

    private void buildGraphForRoutes(GraphDatabase graphDatabase, final GraphFilter filter, Stream<Route> routes,
                                     GraphBuilderCache builderCache) {
        Set<Station> filteredStations = filter.isFiltered() ?
                transportData.getStations().stream().filter(filter::shouldInclude).collect(Collectors.toSet()) :
                transportData.getStations();

        routes.forEach(route -> {
            IdFor<Route> asId = route.getId();
            logger.debug("Adding route " + asId);

            try(Transaction tx = graphDatabase.beginTx()) {
                // create or cache stations and platforms for route, create route stations
                filteredStations.stream().filter(station -> station.servesRoute(route)).
                        forEach(station -> createStationAndRouteStation(tx, route, station, builderCache));

                createLinkRelationships(tx, filter, route, builderCache);
                tx.commit();
            }

            logger.debug("Route " + asId + " added ");
        });
    }

    private Node getStationNode(Transaction txn, Station station) {
        Set<Labels> labels = Labels.forMode(station.getTransportModes());
        // ought to be able find with any of the labels, os use the first one
        Labels label = labels.iterator().next();

        return graphDatabase.findNode(txn, label, station.getProp().getText(), station.getId().getGraphId());
    }

    private void createStationAndRouteStation(Transaction txn, Route route, Station station, GraphBuilderCache builderCache) {

        RouteStation routeStation = transportData.getRouteStation(station, route);
        createRouteStationNode(txn, routeStation, builderCache);

        Node stationNode = getStationNode(txn, station);
        if (stationNode == null) {
            stationNode = createStationNode(txn, station);
            builderCache.putStation(station, stationNode);
        }
    }

    // NOTE: for services that skip some stations, but same stations not skipped by other services
    // this will create multiple links
    private void createLinkRelationships(Transaction tx, GraphFilter filter, Route route, GraphBuilderCache routeBuilderCache) {
        Stream<Service> services = getServices(filter, route);

        // TODO this uses the first cost we encounter for the link, while this is accurate for tfgm trams it does
        //  not give the correct results for buses and trains where time between station can vary depending upon the
        //  service
        Map<Pair<Station, Station>, Integer> pairs = new HashMap<>(); // (start, dest) -> cost
        services.forEach(service -> service.getTrips().forEach(trip -> {
                StopCalls stops = trip.getStops();
                stops.getLegs().forEach(leg -> {
                    if (includeBothStops(filter, leg)) {
                        GTFSPickupDropoffType pickup = leg.getFirst().getPickupType();
                        GTFSPickupDropoffType dropOff = leg.getSecond().getDropoffType();
                        if (pickup==Regular && dropOff==Regular &&
                                !pairs.containsKey(Pair.of(leg.getFirstStation(), leg.getSecondStation()))) {
                            int cost = leg.getCost();
                            pairs.put(Pair.of(leg.getFirstStation(), leg.getSecondStation()), cost);
                        }
                    }
                });
            }));

        pairs.keySet().forEach(pair -> {
            Node startNode = routeBuilderCache.getStation(tx, pair.getLeft());
            Node endNode = routeBuilderCache.getStation(tx, pair.getRight());
            createLinkRelationship(startNode, endNode, route.getTransportMode());
        });

    }

    private boolean includeBothStops(GraphFilter filter, StopCalls.StopLeg leg) {
        return filter.shouldInclude(leg.getFirst()) && filter.shouldInclude(leg.getSecond());
    }

    @NotNull
    private Stream<Service> getServices(GraphFilter filter, Route route) {
        return route.getServices().stream().filter(filter::shouldInclude);
    }

    private void createLinkRelationship(Node from, Node to, TransportMode mode) {
        if (from.hasRelationship(OUTGOING, LINKED)) {
            Iterable<Relationship> existings = from.getRelationships(OUTGOING, LINKED);

            // if there is an existing link between staions then update iff the transport mode not already present
            for (Relationship existing : existings) {
                if (existing.getEndNode().equals(to)) {
                    Set<TransportMode> existingModes = getTransportModes(existing);
                    if (!existingModes.contains(mode)) {
                        addTransportMode(existing, mode);
                    }
                    return;
                }
            }
        }

        Relationship stationsLinked = from.createRelationshipTo(to, LINKED);
        addTransportMode(stationsLinked, mode);
    }

    private void createRouteStationNode(Transaction tx, RouteStation routeStation, GraphBuilderCache builderCache) {
        Node routeStationNode = createGraphNode(tx, Labels.ROUTE_STATION);

        logger.debug(format("Creating route station %s nodeId %s", routeStation.getId(), routeStationNode.getId()));
        GraphProps.setProperty(routeStationNode, routeStation);
        setProperty(routeStationNode, routeStation.getStation());
        setProperty(routeStationNode, routeStation.getRoute());

        Set<TransportMode> modes = routeStation.getTransportModes();
        if (modes.size()==1) {
            setProperty(routeStationNode, modes.iterator().next());
        } else {
            logger.error("Unable to set transportmode property as more than one mode for " + routeStation);
        }

        builderCache.putRouteStation(routeStation.getId(), routeStationNode);
    }

    private Node createStationNode(Transaction tx, Station station) {

        Set<Labels> labels = Labels.forMode(station.getTransportModes());
        logger.debug(format("Creating station node: %s with labels: %s ", station, labels));
        Node stationNode = createGraphNode(tx, labels);
        setProperty(stationNode, station);
        return stationNode;
    }

    public static class Ready {
        private Ready() {
            // prevent guice creating this, want to create dependency on the Builder
        }
    }

}
