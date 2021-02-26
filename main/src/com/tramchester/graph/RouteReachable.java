package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.graphbuild.GraphFilter;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StationRepository;
import org.neo4j.graphdb.*;
import org.neo4j.internal.batchimport.stats.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.graph.TransportRelationshipTypes.ON_ROUTE;
import static java.lang.String.format;

@LazySingleton
public class RouteReachable {
    private static final Logger logger = LoggerFactory.getLogger(RouteReachable.class);

    private final GraphDatabase graphDatabaseService;
    private final InterchangeRepository interchangeRepository;
    private final StationRepository stationRepository;
    private final GraphQuery graphQuery;
    private final boolean debug;

    @Inject
    public RouteReachable(GraphDatabase graphDatabaseService,
                          InterchangeRepository interchangeRepository, StationRepository stationRepository,
                          GraphQuery graphQuery) {
        this.graphDatabaseService = graphDatabaseService;
        this.interchangeRepository = interchangeRepository;
        this.stationRepository = stationRepository;
        this.graphQuery = graphQuery;
        this.debug = logger.isDebugEnabled();
    }

    // supports building tram station reachability matrix
    public IdSet<Station> getRouteReachableWithInterchange(RouteStation start, IdSet<Station> destinations) {
        logger.debug(format("Checking reachability from %s to %s stations", start.getStationId(), destinations.size()));
        IdSet<Station> result;
        try (Transaction txn = graphDatabaseService.beginTx()) {
            // TODO Parallel?
            result = destinations.stream().
                    filter(dest -> hasAnyPaths(txn, start, dest)).
                    collect(IdSet.idCollector());
        }
        logger.info(format("Found reachability from %s to %s of %s stations", start.getStationId(), result.size(), destinations.size()));
        return result;
    }

    private boolean hasAnyPaths(Transaction txn, RouteStation start, IdFor<Station> destId) {
        long startTime = System.currentTimeMillis();

        if (debug) {
            logger.debug(format("Check any paths %s threshhold %s", start, destId));
        }

        IdSet<Station> interchanges = interchangeRepository.getInterchangesFor(start.getTransportMode());
        List<String> interchangeIds = interchanges.stream().map(IdFor::getGraphId).collect(Collectors.toList());

        Map<String, Object> params = new HashMap<>();
        params.put("route_station_id", start.getId().getGraphId());
        params.put("station_id", destId.getGraphId());
        params.put("route_id", start.getRoute().getId().getGraphId());
        params.put("interchanges", interchangeIds);

        String query = "MATCH (begin:ROUTE_STATION)-[:ON_ROUTE*0..]->(dest:ROUTE_STATION) " +
                "WHERE (begin.route_station_id = $route_station_id) AND (dest.route_id = $route_id) " +
                " AND ( (dest.station_id = $station_id) OR (dest.station_id IN $interchanges) ) " +
                "RETURN DISTINCT dest";

        if (debug) {
            logger.debug("Query: '" + query + '"');
        }

        boolean foundRoute;
        Result result = txn.execute(query, params);
        foundRoute = result.hasNext();
        result.close();

        if (debug) {
            logger.debug(foundRoute ? "Found" : "Not Found" + format(" route %s to %s", start, destId));
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Took " + duration);
        }
        return foundRoute;
    }

    // supports position inference on live data
    public List<Route> getRoutesFromStartToNeighbour(Station startStation, Station endStation) {
        List<Route> results = new ArrayList<>();
        Set<Route> firstRoutes = startStation.getRoutes();
        IdFor<Station> endStationId = endStation.getId();

        try (Transaction txn = graphDatabaseService.beginTx()) {
            firstRoutes.forEach(route -> {
                RouteStation routeStation = stationRepository.getRouteStation(startStation, route);
                Node routeStationNode = graphQuery.getRouteStationNode(txn, routeStation);
                if (routeStationNode==null) {
                    logger.warn("Missing route station, graph DB rebuild needed?");
                } else {
                    Iterable<Relationship> edges = routeStationNode.getRelationships(Direction.OUTGOING, ON_ROUTE);
                    for (Relationship edge : edges) {
                        IdFor<Station> endNodeStationId = GraphProps.getStationIdFrom(edge.getEndNode());
                        if (endStationId.equals(endNodeStationId)) {
                            results.add(route);
                        }
                    }
                }
            });
        }
        return results;
    }

}
