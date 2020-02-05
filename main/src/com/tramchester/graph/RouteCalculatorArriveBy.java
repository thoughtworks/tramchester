package com.tramchester.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.format;

public class RouteCalculatorArriveBy implements TramRouteCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculatorArriveBy.class);

    private final TramRouteReachable tramRouteReachable;
    private final RouteCalculator routeCalculator;
    private final TramchesterConfig config;

    public RouteCalculatorArriveBy(TramRouteReachable tramRouteReachable, RouteCalculator routeCalculator, TramchesterConfig config) {
        this.tramRouteReachable = tramRouteReachable;
        this.routeCalculator = routeCalculator;
        this.config = config;
    }

    @Override
    public Stream<Journey> calculateRoute(String startStationId, String destinationId, TramTime queryTime,
                                          TramServiceDate queryDate) {
        int costToDest = tramRouteReachable.getApproxCostBetween(startStationId, destinationId);
        TramTime departureTime = calcDepartTime(queryTime, costToDest);
        logger.info(format("Plan journey, arrive by %s so depart by %s", queryTime, departureTime));
        return routeCalculator.calculateRoute(startStationId, destinationId, departureTime, queryDate);
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtEnd(String startId, Node endOfWalk, List<String> destStations,
                                                   TramTime queryTime, TramServiceDate queryDate) {
        int costToDest = tramRouteReachable.getApproxCostBetween(startId, endOfWalk);
        TramTime departureTime = calcDepartTime(queryTime, costToDest);
        logger.info(format("Plan journey, arrive by %s so depart by %s", queryTime, departureTime));
        return routeCalculator.calculateRouteWalkAtEnd(startId, endOfWalk, destStations, departureTime, queryDate);
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStart(Node origin, String destinationId, TramTime queryTime,
                                                     TramServiceDate queryDate) {
        int costToDest = tramRouteReachable.getApproxCostBetween(origin, destinationId);
        TramTime departureTime = calcDepartTime(queryTime, costToDest);
        logger.info(format("Plan journey, arrive by %s so depart by %s", queryTime, departureTime));
        return routeCalculator.calculateRouteWalkAtStart(origin, destinationId, departureTime, queryDate);
    }

    private TramTime calcDepartTime(TramTime queryTime, int costToDest) {
        int buffer = config.getMaxWait() / 2;
        return queryTime.minusMinutes(costToDest).minusMinutes(buffer);
    }
}
