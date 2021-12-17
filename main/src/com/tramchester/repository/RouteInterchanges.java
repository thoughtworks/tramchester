package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.InterchangeStation;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.metrics.TimedTransaction;
import org.apache.commons.lang3.tuple.Pair;
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
    private final RouteCostCalculator routeCostCalculator;
    private final GraphDatabase graphDatabase;

    private final Map<Route, Set<InterchangeStation>> interchangesForRoute;
    private Map<RouteStation, Integer> routeStationToInterchangeCost;

    @Inject
    public RouteInterchanges(RouteRepository routeRepository, StationRepository stationRepository, InterchangeRepository interchangeRepository,
                             RouteCostCalculator routeCostCalculator, GraphDatabase graphDatabase) {
        this.routeRepository = routeRepository;
        this.stationRepository = stationRepository;
        this.interchangeRepository = interchangeRepository;
        this.routeCostCalculator = routeCostCalculator;

        this.graphDatabase = graphDatabase;
        interchangesForRoute = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        populateRouteToInterchangeMap();
        populateRouteStationToFirstInterchange();
        logger.info("started");
    }

    private void populateRouteStationToFirstInterchange() {
        final Set<RouteStation> routeStations = stationRepository.getRouteStations();
        logger.info("Populate for " + routeStations.size() + " route stations");
        try(TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "routeStationToInterchange")) {
            routeStationToInterchangeCost = routeStations.stream().
                    collect(Collectors.toMap(routeStation -> routeStation,
                            routeStation -> lowestCostBetween(timedTransaction.transaction(), routeStation)));
        }
    }

    private int lowestCostBetween(Transaction txn, RouteStation routeStation) {
        int cost = routeCostCalculator.costToInterchange(txn, routeStation);
        if (cost<0) {
            return Integer.MAX_VALUE;
        }
        return cost;
    }

    private void populateRouteToInterchangeMap() {
        routeRepository.getRoutes().forEach(route -> interchangesForRoute.put(route, new HashSet<>()));
        Set<InterchangeStation> allInterchanges = interchangeRepository.getAllInterchanges();
        allInterchanges.stream().
                flatMap(inter -> inter.getDropoffRoutes().stream().map(route -> Pair.of(route, inter))).
                forEach(pair -> interchangesForRoute.get(pair.getLeft()).add(pair.getRight()));
    }

    public Set<InterchangeStation> getFor(Route route) {
        return interchangesForRoute.get(route);
    }

    public int costToInterchange(RouteStation routeStation) {
        return routeStationToInterchangeCost.get(routeStation);
    }

}
