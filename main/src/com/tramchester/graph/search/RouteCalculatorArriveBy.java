package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.RouteCostCalculator;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Stream;

import static java.lang.String.format;

public class RouteCalculatorArriveBy implements TramRouteCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculatorArriveBy.class);

    private final RouteCostCalculator costCalculator;
    private final RouteCalculator routeCalculator;
    private final TramchesterConfig config;

    public RouteCalculatorArriveBy(RouteCostCalculator costCalculator, RouteCalculator routeCalculator, TramchesterConfig config) {
        this.costCalculator = costCalculator;
        this.routeCalculator = routeCalculator;
        this.config = config;
    }

    @Override
    public Stream<Journey> calculateRoute(Transaction txn, Station startStation, Station destination, JourneyRequest journeyRequest) {
        int costToDest = costCalculator.getApproxCostBetween(txn, startStation, destination);
        JourneyRequest departureTime = calcDepartTime(journeyRequest, costToDest);
        logger.info(format("Plan journey, arrive by %s so depart by %s", journeyRequest, departureTime));
        return routeCalculator.calculateRoute(txn, startStation, destination, departureTime);
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtEnd(Transaction txn, Station start, Node endOfWalk, Set<Station> destStations,
                                                   JourneyRequest journeyRequest) {
        int costToDest = costCalculator.getApproxCostBetween(txn, start, endOfWalk);
        JourneyRequest departureTime = calcDepartTime(journeyRequest, costToDest);
        logger.info(format("Plan journey, arrive by %s so depart by %s", journeyRequest, departureTime));
        return routeCalculator.calculateRouteWalkAtEnd(txn, start, endOfWalk, destStations, departureTime);
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStart(Transaction txn, Node origin, Station destination, JourneyRequest journeyRequest) {
        int costToDest = costCalculator.getApproxCostBetween(txn, origin, destination);
        JourneyRequest departureTime = calcDepartTime(journeyRequest, costToDest);
        logger.info(format("Plan journey, arrive by %s so depart by %s", journeyRequest, departureTime));
        return routeCalculator.calculateRouteWalkAtStart(txn, origin, destination, departureTime);
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStartAndEnd(Transaction txn, Node startNode, Node endNode, Set<Station> destinationStations,
                                                           JourneyRequest journeyRequest) {
        int costToDest = costCalculator.getApproxCostBetween(txn, startNode, endNode);
        JourneyRequest departureTime = calcDepartTime(journeyRequest, costToDest);
        logger.info(format("Plan journey, arrive by %s so depart by %s", journeyRequest, departureTime));
        return routeCalculator.calculateRouteWalkAtStartAndEnd(txn, startNode, endNode, destinationStations, departureTime);
    }

    private JourneyRequest calcDepartTime(JourneyRequest originalRequest, int costToDest) {
        // TODO Handle buses & trains
        //int buffer = config.getBus() ? config.getMaxWait() : config.getMaxWait() / 2;
        TramTime queryTime = originalRequest.getTime();
        TramTime computedDepartTime = queryTime.minusMinutes(costToDest).minusMinutes(config.getMaxWait() / 2);
        return new JourneyRequest(originalRequest.getDate(), computedDepartTime, true,
                originalRequest.getMaxChanges(), originalRequest.getMaxJourneyDuration());
    }
}
