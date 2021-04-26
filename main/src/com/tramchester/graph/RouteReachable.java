package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteCallingStations;
import com.tramchester.repository.StationRepository;
import org.neo4j.graphdb.*;
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
    private final StationRepository stationRepository;
    private final GraphQuery graphQuery;
    private final RouteCallingStations routeCallingStations;

    @Inject
    public RouteReachable(GraphDatabase graphDatabaseService, StationRepository stationRepository,
                          GraphQuery graphQuery, RouteCallingStations routeCallingStations,
                          StagedTransportGraphBuilder.Ready ready) {
        this.graphDatabaseService = graphDatabaseService;
        this.stationRepository = stationRepository;
        this.graphQuery = graphQuery;
        this.routeCallingStations = routeCallingStations;
    }

    /***
     * Returns true if start can reach an interchange on the same route
     * @param start starting point
     * @return true if an interchange is reachable
     */
    public boolean isInterchangeReachableOnRoute(RouteStation start) {
        logger.debug("Checking interchange reachability from " + start.getStationId());

        boolean result;
        try (Transaction txn = graphDatabaseService.beginTx()) {
            result = interchangeReachable(txn, start);
        }

        String verb = result? " Can " : " Cannot ";
        logger.debug(start.getId() + verb + "reach interchange");
        return result;
    }

    /***
     * Filters destinations based on whether reachable from start on same route, no interchanges
     * @param start starting point
     * @return destinations reachable from start via same route
     */
    public IdSet<Station> getReachableStationsOnRoute(RouteStation start) {
        IdSet<Station> stationsOnRoute = routeCallingStations.getStationsFor(start.getRoute()).
                stream().collect(IdSet.collector());

        logger.debug(format("Checking reachability from %s to %s stations", start.getStationId(), stationsOnRoute.size()));

        IdSet<Station> result;
        try (Transaction txn = graphDatabaseService.beginTx()) {
                result = stationsOnRoute.stream().
                        filter(dest -> sameRouteReachable(txn, start, dest)).
                        collect(IdSet.idCollector());
        }

        int size = result.size();
        logger.debug(format("Found reachability from %s to %s of %s stations", start.getStationId(), size, stationsOnRoute.size()));

        return result;
    }

    private boolean interchangeReachable(Transaction txn, RouteStation start) {
        Map<String, Object> params = new HashMap<>();
        params.put("route_station_id", start.getId().getGraphId());
        params.put("route_id", start.getRoute().getId().getGraphId());

        String query = "MATCH (begin:ROUTE_STATION)-[:ON_ROUTE*0..]->(dest:INTERCHANGE) " +
                "WHERE (begin.route_station_id = $route_station_id) " +
                "AND (dest.route_id = $route_id) " +
                "RETURN DISTINCT dest";

        boolean foundRoute;
        Result result = txn.execute(query, params);
        foundRoute = result.hasNext();
        result.close();

        return foundRoute;
    }

    private boolean sameRouteReachable(Transaction txn, RouteStation start, IdFor<Station> destId) {
        Map<String, Object> params = new HashMap<>();
        params.put("route_station_id", start.getId().getGraphId());
        params.put("route_id", start.getRoute().getId().getGraphId());
        params.put("station_id", destId.getGraphId());

        String query = "MATCH (begin:ROUTE_STATION)-[:ON_ROUTE*0..]->(dest:ROUTE_STATION) " +
                "WHERE (begin.route_station_id = $route_station_id) " +
                " AND (dest.route_id = $route_id) " +
                " AND (dest.station_id = $station_id) " +
                "RETURN DISTINCT dest";

        boolean foundRoute;
        Result result = txn.execute(query, params);
        foundRoute = result.hasNext();
        result.close();

        return foundRoute;
    }

    // supports position inference on live data
    public List<Route> getRoutesFromStartToNeighbour(StationPair pair) {
        List<Route> results = new ArrayList<>();
        Station startStation = pair.getBegin();
        Set<Route> firstRoutes = startStation.getRoutes();
        IdFor<Station> endStationId = pair.getEnd().getId();

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
