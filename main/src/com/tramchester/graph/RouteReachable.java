package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.graphbuild.GraphProps;
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
    private final InterchangeRepository interchangeRepository;
    private final StationRepository stationRepository;
    private final GraphQuery graphQuery;
    private final RouteCallingStations routeCallingStations;

    @Inject
    public RouteReachable(GraphDatabase graphDatabaseService,
                          InterchangeRepository interchangeRepository, StationRepository stationRepository,
                          GraphQuery graphQuery, RouteCallingStations routeCallingStations) {
        this.graphDatabaseService = graphDatabaseService;
        this.interchangeRepository = interchangeRepository;
        this.stationRepository = stationRepository;
        this.graphQuery = graphQuery;
        this.routeCallingStations = routeCallingStations;
    }

    // supports building tram station reachability matrix
    public IdSet<Station> getRouteReachableWithInterchange(RouteStation start, IdSet<Station> destinations) {
        logger.debug(format("Checking reachability from %s to %s stations", start.getStationId(), destinations.size()));

        // TODO cache the interchange list
        IdSet<Station> interchanges = interchangeRepository.getInterchangesFor(start.getTransportMode());
        List<String> interchangeIds = interchanges.stream().map(IdFor::getGraphId).collect(Collectors.toList());

        IdSet<Station> result;
        try (Transaction txn = graphDatabaseService.beginTx()) {
            if (interchangeReachable(txn, start, interchangeIds)) {
                // all stations will be (assumed to be) reachable if we can reach an interchange from the starting point
                logger.debug("Can reach interchange from " + start.getId());
                result = destinations;
            } else {
                // no interchange access so only consider stations on the same route
                // TODO Push this filtering and selection into the query?
                IdSet<Station> stationsOnRoute = routeCallingStations.getStationsFor(start.getRoute()).
                        stream().collect(IdSet.collector());
                // TODO Parallel?
                logger.debug("No reachable interchange from " + start.getId());
                result = stationsOnRoute.stream().
                        filter(dest -> hasAnyPaths(txn, start, dest)).
                        collect(IdSet.idCollector());
            }
        }

        int size = result.size();
        String msg = format("Found reachability from %s to %s of %s stations", start.getStationId(), size, destinations.size());
        if (size>0) {
            logger.info(msg);
        } else {
            logger.warn(msg);
        }
        return result;
    }

    private boolean interchangeReachable(Transaction txn, RouteStation start, List<String> interchangeIds) {
        Map<String, Object> params = new HashMap<>();
        params.put("route_station_id", start.getId().getGraphId());
        params.put("route_id", start.getRoute().getId().getGraphId());
        params.put("interchanges", interchangeIds);

        String query = "MATCH (begin:ROUTE_STATION)-[:ON_ROUTE*0..]->(dest:ROUTE_STATION) " +
                "WHERE (begin.route_station_id = $route_station_id) " +
                "AND (dest.route_id = $route_id) " +
                " AND (dest.station_id IN $interchanges) " +
                "RETURN DISTINCT dest";

        boolean foundRoute;
        Result result = txn.execute(query, params);
        foundRoute = result.hasNext();
        result.close();

        return foundRoute;
    }

    private boolean hasAnyPaths(Transaction txn, RouteStation start, IdFor<Station> destId) {
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
