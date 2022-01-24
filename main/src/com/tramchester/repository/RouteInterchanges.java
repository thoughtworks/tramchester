package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.metrics.TimedTransaction;
import com.tramchester.metrics.Timing;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.util.Streams;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@LazySingleton
public class RouteInterchanges {
    private static final Logger logger = LoggerFactory.getLogger(RouteInterchanges.class);

    private final RouteRepository routeRepository;
    private final StationRepository stationRepository;
    private final InterchangeRepository interchangeRepository;
    private final GraphDatabase graphDatabase;

    private final Map<Route, Set<InterchangeStation>> interchangesForRoute;
    private Map<RouteStation, Integer> routeStationToInterchangeCost;

    @Inject
    public RouteInterchanges(RouteRepository routeRepository, StationRepository stationRepository, InterchangeRepository interchangeRepository,
                             GraphDatabase graphDatabase, StagedTransportGraphBuilder.Ready ready) {
        this.routeRepository = routeRepository;
        this.stationRepository = stationRepository;
        this.interchangeRepository = interchangeRepository;

        this.graphDatabase = graphDatabase;
        interchangesForRoute = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        populateRouteToInterchangeMap();
        populateRouteStationToFirstInterchangeByRouteStation();
        logger.info("started");
    }

    private void populateRouteStationToFirstInterchangeByRouteStation() {
        final Set<RouteStation> routeStations = stationRepository.getRouteStations();
        logger.info("Populate for first interchange " + routeStations.size() + " route stations");
//        try(TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "routeStationToInterchange")) {
//            routeStationToInterchangeCost = routeStations.stream().
//                    collect(Collectors.toMap(routeStation -> routeStation,
//                            routeStation -> lowestCostBetween(timedTransaction.transaction(), routeStation)));
//        }
        routeStationToInterchangeCost = new HashMap<>();
        try(TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "spike")) {
            routeRepository.getRoutes().forEach(route -> spike(timedTransaction.transaction(), route));
        }
    }

    private int lowestCostBetween(Transaction txn, RouteStation routeStation) {
        int cost = lowestCostInterchangeSameRoute(txn, routeStation);
        if (cost<0) {
            return Integer.MAX_VALUE;
        }
        return cost;
    }

    private void populateRouteToInterchangeMap() {
        try (Timing ignored = new Timing(logger,"Populate interchanges for routes")) {
            routeRepository.getRoutes().forEach(route -> interchangesForRoute.put(route, new HashSet<>()));
            Set<InterchangeStation> allInterchanges = interchangeRepository.getAllInterchanges();
            allInterchanges.stream().
                    flatMap(inter -> inter.getDropoffRoutes().stream().map(route -> Pair.of(route, inter))).
                    forEach(pair -> interchangesForRoute.get(pair.getLeft()).add(pair.getRight()));
        }
    }

    public Set<InterchangeStation> getFor(Route route) {
        return interchangesForRoute.get(route);
    }

    public int costToInterchange(RouteStation routeStation) {
        if (interchangeRepository.isInterchange(routeStation.getStation())) {
            return 0;
        }
        if (routeStationToInterchangeCost.containsKey(routeStation)) {
            return routeStationToInterchangeCost.get(routeStation);
        }
        return Integer.MAX_VALUE;
    }


    public int lowestCostInterchangeSameRoute(Transaction txn, RouteStation routeStation) {
        if (interchangeRepository.isInterchange(routeStation.getStation())) {
            return 0;
        }
        return  findCostToInterchangeB(txn, routeStation);
    }

    private void spike(Transaction txn, Route route) {
        IdFor<Route> routeId = route.getId();

        long maxNodes = route.getTrips().stream().flatMap(trip -> trip.getStopCalls().getStationSequence().stream()).distinct().count();

        logger.info("Find stations to interchange least costs for " + routeId + " max nodes " + maxNodes);

        String template = "MATCH path = (rs:ROUTE_STATION {route_id:$id})-[:ON_ROUTE*1..%s {route_id:$id}]->(:INTERCHANGE {route_id:$id})" +
                " WHERE NOT rs:INTERCHANGE " +
                " RETURN path";

        String query = String.format(template, maxNodes);

        Map<String, Object> params = new HashMap<>();
        params.put("id", routeId.getGraphId());

        Result results = txn.execute(query, params);

        List<Pair<IdFor<Station>, Integer>> pairs = results.stream().
                filter(row -> row.containsKey("path")).
                map(row -> (Path) row.get("path")).
                map(path -> Pair.of(GraphProps.getStationId(path.startNode()), length(path))).
                collect(Collectors.toList());

        logger.info("Got " + pairs.size() + " for " + routeId);

        Map<IdFor<Station>, Integer> stationToInterPair = new HashMap<>();

        pairs.forEach(pair -> {
            final IdFor<Station> key = pair.getKey();
            final Integer cost = pair.getValue();
            if (stationToInterPair.containsKey(key)) {
                if (cost < stationToInterPair.get(key)) {
                    stationToInterPair.put(key, cost);
                }
            } else {
                stationToInterPair.put(key, cost);
            }
        });

        logger.info("Found " + stationToInterPair.size() + " results for " + routeId);

        stationToInterPair.forEach((stationIdPair, cost) -> {
            RouteStation routeStation = stationRepository.getRouteStationById(RouteStation.createId(stationIdPair, routeId));
            routeStationToInterchangeCost.put(routeStation, cost);
        });

        stationToInterPair.clear();
    }

    private int length(Path path) {
        return Streams.stream(path.relationships()).
                filter(relationship -> relationship.hasProperty("cost")).
                mapToInt(GraphProps::getCost).sum();
    }

    private int findCostToInterchangeA(Transaction txn, RouteStation routeStation) {

        logger.debug("Find cost to first interchange for " + routeStation);

        String query = "MATCH path = (start:ROUTE_STATION {route_station_id: $routeStationId})-[:ON_ROUTE*]->(inter:INTERCHANGE) " +
                " WITH length(path) as length, path" +
                " ORDER BY length " +
                " LIMIT 1 " +
                " RETURN path";

        Map<String, Object> params = new HashMap<>();
        params.put("routeStationId", routeStation.getId().getGraphId());

        Result results = txn.execute(query, params);

        List<Path> paths = results.stream().
                filter(row -> row.containsKey("path")).
                map(row -> (Path) row.get("path")).collect(Collectors.toList());

        if (paths.isEmpty()) {
            logger.debug("No interchange reachable for " + routeStation.getId());
            return -1;
        }

        if (paths.size()>1) {
            final String msg = "Too many results for " + routeStation.getId() + " " + paths;
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        Path path = paths.get(0);

        return Streams.stream(path.relationships()).
                filter(relationship -> relationship.hasProperty("cost")).
                mapToInt(relationship -> (int)relationship.getProperty("cost")).
                sum();

    }


    private int findCostToInterchangeB(Transaction txn, RouteStation routeStation) {

        logger.debug("Find cost to first interchange for " + routeStation);

        String query = "MATCH path = (start:ROUTE_STATION {route_station_id: $routeStationId})-[:ON_ROUTE*]->(inter:INTERCHANGE) " +
                "WITH REDUCE (total = 0, r in relationships(path) | total + r.cost) as cost, path" +
                " ORDER BY cost" +
                " RETURN cost LIMIT 1 ";

        Map<String, Object> params = new HashMap<>();
        params.put("routeStationId", routeStation.getId().getGraphId());

        Result results = txn.execute(query, params);

        List<Long> costs = results.stream().
                filter(row -> row.containsKey("cost")).
                map(row -> (long) row.get("cost")).
                collect(Collectors.toList());

        if (costs.isEmpty()) {
            logger.debug("No interchange reachable for " + routeStation.getId());
            return -1;
        }

        if (costs.size()>1) {
            final String msg = "Too many results for " + routeStation.getId() + " " + costs;
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        return Math.toIntExact(costs.get(0));

    }
}
