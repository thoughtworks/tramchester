package com.tramchester.graph.graphbuild;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.HasGraphDBConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.metrics.TimedTransaction;
import com.tramchester.metrics.Timing;
import com.tramchester.repository.TransportData;
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

import static com.tramchester.domain.reference.GTFSPickupDropoffType.Regular;
import static com.tramchester.graph.TransportRelationshipTypes.*;
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

    // force contsruction via guide to generate ready token, needed where no direct code dependency on this class
    public Ready getReady() {
        return new Ready();
    }

    @Inject
    public StationsAndLinksGraphBuilder(GraphDatabase graphDatabase, HasGraphDBConfig config, GraphFilter graphFilter,
                                        TransportData transportData, GraphBuilderCache builderCache) {
        super(graphDatabase, graphFilter, config, builderCache);
        this.transportData = transportData;
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        logger.info("Data source name " + transportData.getSourceName());
        if (graphDatabase.isCleanDB()) {
            logger.info("Rebuild of Stations, RouteStations and Links graph DB for " + graphDBConfig.getDbPath());
            if (graphFilter.isFiltered()) {
                logger.warn("Graph is filtered " + graphFilter);
            }
            buildGraphwithFilter(graphDatabase, builderCache);
            logger.info("Graph rebuild is finished for " + graphDBConfig.getDbPath());
        } else {
            logger.info("No rebuild of graph, using existing data");
            graphDatabase.waitForIndexes();
        }
    }

    private void buildGraphwithFilter(GraphDatabase graphDatabase, GraphBuilderCache builderCache) {
        logger.info("Building graph for feedinfo: " + transportData.getDataSourceInfo());
        logMemory("Before graph build");

        graphDatabase.createIndexs();

        try (Timing timing = new Timing(logger, "graph rebuild")) {
            try(TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "Adding stations")) {
                Transaction tx = timedTransaction.transaction();
                for(Station station : transportData.getStations()) {
                    if (graphFilter.shouldInclude(station)) {
                        Node stationNode = createStationNode(tx, station);
                        createPlatformsForStation(tx, station, builderCache);
                        builderCache.putStation(station.getId(), stationNode);
                    }
                }
                timedTransaction.commit();
            }

            for(Agency agency : transportData.getAgencies()) {
                if (graphFilter.shouldIncludeAgency(agency)) {
                    addRouteStationsAndLinksFor(agency, builderCache);
                }
            }
        } catch (Exception except) {
            logger.error("Exception while rebuilding the graph", except);
            throw new RuntimeException("Unable to build graph " + graphDatabase.getDbPath(), except);
        }

        reportStats();
        System.gc();
        logMemory("After graph build");
    }

    private void addRouteStationsAndLinksFor(Agency agency, GraphBuilderCache builderCache) {

        Set<Route> routes = agency.getRoutes().stream().filter(graphFilter::shouldIncludeRoute).collect(Collectors.toSet());
        if (routes.isEmpty()) {
            return;
        }

        logger.info(format("Adding %s routes for agency %s", routes.size(), agency));

        Set<Station> filteredStations = transportData.getActiveStationStream().
                filter(graphFilter::shouldInclude).
                collect(Collectors.toSet());

        // NOTE:
        // The station.servesRouteDropoff(route) || station.servesRoutePickup(route) filter below means route station
        // nodes will not be created for stations that as 'passed' by services that do not call, which is the
        // case for rail transport data.

        try(TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "Adding routes")){
            Transaction tx = timedTransaction.transaction();
            routes.forEach(route -> {
                IdFor<Route> asId = route.getId();
                logger.info("Adding route " + asId);
                filteredStations.stream().
                        filter(station -> station.servesRouteDropoff(route) || station.servesRoutePickup(route)).
                        map(station -> transportData.getRouteStation(station, route)).
                        forEach(routeStation -> {
                            Node routeStationNode = createRouteStationNode(tx, routeStation, builderCache);
                            linkStationAndRouteStation(tx, routeStation.getStation(), routeStationNode, route.getTransportMode());
                        });

                createLinkRelationships(tx, route, builderCache);

                logger.debug("Route " + asId + " added ");
            });
            timedTransaction.commit();
        }
    }

    private void linkStationAndRouteStation(Transaction txn, Station station, Node routeStationNode, TransportMode transportMode) {
        Node stationNode = builderCache.getStation(txn, station.getId());

        final Relationship stationToRoute = stationNode.createRelationshipTo(routeStationNode, STATION_TO_ROUTE);
        final Relationship routeToStation = routeStationNode.createRelationshipTo(stationNode, ROUTE_TO_STATION);

        final int minimumChangeCost = station.getMinimumChangeCost();
        GraphProps.setCostProp(stationToRoute, minimumChangeCost);
        GraphProps.setCostProp(routeToStation, 0);

        GraphProps.setProperty(routeToStation, transportMode);
        GraphProps.setProperty(stationToRoute, transportMode);

        GraphProps.setMaxCostProp(stationToRoute, minimumChangeCost);
        GraphProps.setMaxCostProp(routeToStation, 0);
    }

    // NOTE: for services that skip some stations, but same stations not skipped by other services
    // this will create multiple links
    private void createLinkRelationships(Transaction tx, Route route, GraphBuilderCache routeBuilderCache) {

        // TODO this uses the first cost we encounter for the link, while this is accurate for tfgm trams it does
        //  not give the correct results for buses and trains where time between station can vary depending upon the
        //  service
        Map<StationIdPair, Integer> pairs = new HashMap<>(); // (start, dest) -> cost
        route.getTrips().forEach(trip -> {
                StopCalls stops = trip.getStopCalls();
                stops.getLegs().forEach(leg -> {
                    if (includeBothStops(graphFilter, leg)) {
                        GTFSPickupDropoffType pickup = leg.getFirst().getPickupType();
                        GTFSPickupDropoffType dropOff = leg.getSecond().getDropoffType();
                        StationIdPair legStations = StationIdPair.of(leg.getFirstStation(), leg.getSecondStation());
                        if (pickup==Regular && dropOff==Regular &&
                                !pairs.containsKey(legStations)) {
                            int cost = leg.getCost();
                            pairs.put(legStations, cost);
                        }
                    }
                });
            });

        pairs.keySet().forEach(pair -> {
            Node startNode = routeBuilderCache.getStation(tx, pair.getBeginId());
            Node endNode = routeBuilderCache.getStation(tx, pair.getEndId());
            createLinkRelationship(startNode, endNode, route.getTransportMode());
        });

    }

    private boolean includeBothStops(GraphFilter filter, StopCalls.StopLeg leg) {
        return filter.shouldInclude(leg.getFirst()) && filter.shouldInclude(leg.getSecond());
    }

    private void createLinkRelationship(Node from, Node to, TransportMode mode) {
        if (from.hasRelationship(OUTGOING, LINKED)) {
            Iterable<Relationship> existings = from.getRelationships(OUTGOING, LINKED);

            // if there is an existing link between stations then update iff the transport mode not already present
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

        Relationship stationsLinked = createRelationship(from, to, LINKED);
        addTransportMode(stationsLinked, mode);
    }

    private void createPlatformsForStation(Transaction txn, Station station, GraphBuilderCache routeBuilderCache) {
        for (Platform platform : station.getPlatforms()) {
            Node platformNode = createGraphNode(txn, GraphLabel.PLATFORM);
            setProperty(platformNode, platform);
            setProperty(platformNode, station);

            setTransportMode(station, platformNode);

            routeBuilderCache.putPlatform(platform.getId(), platformNode);
        }
    }

    private Node createRouteStationNode(Transaction tx, RouteStation routeStation, GraphBuilderCache builderCache) {
        Node existing = graphDatabase.findNode(tx,
                GraphLabel.ROUTE_STATION, GraphPropertyKey.ROUTE_STATION_ID.getText(), routeStation.getId().getGraphId());

        if (existing!=null) {
            final String msg = "Existing route station node for " + routeStation + " with id " + routeStation.getId();
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        Node routeStationNode = createGraphNode(tx, GraphLabel.ROUTE_STATION);

        logger.debug(format("Creating route station %s nodeId %s", routeStation.getId(), routeStationNode.getId()));
        GraphProps.setProperty(routeStationNode, routeStation);
        setProperty(routeStationNode, routeStation.getStation());
        setProperty(routeStationNode, routeStation.getRoute());

        TransportMode mode = routeStation.getRoute().getTransportMode();
        setProperty(routeStationNode, mode);

        builderCache.putRouteStation(routeStation.getId(), routeStationNode);
        return routeStationNode;
    }

    private void setTransportMode(HasTransportModes hasTransportModes, Node node) {
        Set<TransportMode> modes = hasTransportModes.getTransportModes();
        if (modes.size()==1) {
            setProperty(node, modes.iterator().next());
        } else {
            logger.error("Unable to set transportmode property, more than one mode for " + hasTransportModes);
        }
    }


    public static class Ready {
        private Ready() {
            // prevent guice creating this, want to create dependency on the Builder
        }
    }

}
