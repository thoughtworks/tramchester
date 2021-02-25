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
import org.neo4j.graphdb.traversal.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static com.tramchester.graph.graphbuild.GraphBuilder.Labels.ROUTE_STATION;
import static java.lang.String.format;

@LazySingleton
public class RouteReachable {
    private static final Logger logger = LoggerFactory.getLogger(RouteReachable.class);

    private final GraphDatabase graphDatabaseService;
    private final InterchangeRepository interchangeRepository;
    private final StationRepository stationRepository;
    private final boolean warnForMissing;
    private final GraphQuery graphQuery;

    @Inject
    public RouteReachable(GraphDatabase graphDatabaseService,
                          InterchangeRepository interchangeRepository, StationRepository stationRepository, GraphFilter filter,
                          GraphQuery graphQuery) {
        this.graphDatabaseService = graphDatabaseService;
        this.interchangeRepository = interchangeRepository;
        this.stationRepository = stationRepository;
        this.warnForMissing = !filter.isFiltered();
        this.graphQuery = graphQuery;
    }

    // supports building tram station reachability matrix
    public boolean getRouteReachableWithInterchange(RouteStation routeStation, Station endStation) {
        return hasAnyPathsCypher(routeStation, endStation);
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

    private boolean hasAnyPathsCypher(RouteStation routeStation, Station endStation) {
        logger.info(format("Check any paths %s threshhold %s", routeStation, endStation));
        long start = System.currentTimeMillis();

        Map<String, Object> params = new HashMap<>();

        // if no overlap in transport mode then no direct route exists
        if (!endStation.getTransportModes().contains(routeStation.getTransportMode())) {
            return false;
        }

        IdSet<Station> interchanges = interchangeRepository.getInterchangesFor(routeStation.getTransportMode());
        List<String> interchangeIds = interchanges.stream().map(IdFor::getGraphId).collect(Collectors.toList());

        params.put("route_station_id", routeStation.getId().getGraphId());
        params.put("station_id", endStation.getId().getGraphId());
        params.put("route_id", routeStation.getRoute().getId().getGraphId());
        params.put("interchanges", interchangeIds);

        String query = "MATCH (begin:ROUTE_STATION)-[:ON_ROUTE*0..]->(dest:ROUTE_STATION) " +
                "WHERE (begin.route_station_id = $route_station_id) AND (dest.route_id = $route_id) " +
                " AND ( (dest.station_id = $station_id) OR (dest.station_id IN $interchanges) ) " +
                "RETURN DISTINCT dest";

        logger.info("Query: '" + query + '"');

        boolean foundRoute;
        try (Transaction txn  = graphDatabaseService.beginTx()) {
            // TODO Filtered graphs behaviours? Check for first node existing?
            Result result = txn.execute(query, params);
            foundRoute = result.hasNext();
        }
        logger.info(foundRoute?"Found":"Not Found" + format(" route %s to %s", routeStation, endStation));
        long duration = System.currentTimeMillis()-start;
        logger.info("Took " + duration);
        return foundRoute;
    }

}
