package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.RouteReachable;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.format;

public class RouteCalculatorArriveBy implements TramRouteCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculatorArriveBy.class);

    private final RouteReachable routeReachable;
    private final RouteCalculator routeCalculator;
    private final TramchesterConfig config;

    public RouteCalculatorArriveBy(RouteReachable routeReachable, RouteCalculator routeCalculator, TramchesterConfig config) {
        this.routeReachable = routeReachable;
        this.routeCalculator = routeCalculator;
        this.config = config;
    }

    @Override
    public Stream<Journey> calculateRoute(String startStationId, Station destination, TramTime queryTime,
                                          TramServiceDate queryDate) {
        int costToDest = routeReachable.getApproxCostBetween(startStationId, destination.getId());
        TramTime departureTime = calcDepartTime(queryTime, costToDest);
        logger.info(format("Plan journey, arrive by %s so depart by %s", queryTime, departureTime));
        return routeCalculator.calculateRoute(startStationId, destination, departureTime, queryDate);
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtEnd(String startId, Node endOfWalk, List<Station> destStations,
                                                   TramTime queryTime, TramServiceDate queryDate) {
        int costToDest = routeReachable.getApproxCostBetween(startId, endOfWalk);
        TramTime departureTime = calcDepartTime(queryTime, costToDest);
        logger.info(format("Plan journey, arrive by %s so depart by %s", queryTime, departureTime));
        return routeCalculator.calculateRouteWalkAtEnd(startId, endOfWalk, destStations, departureTime, queryDate);
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStart(Node origin, Station destination, TramTime queryTime,
                                                     TramServiceDate queryDate) {
        int costToDest = routeReachable.getApproxCostBetween(origin, destination.getId());
        TramTime departureTime = calcDepartTime(queryTime, costToDest);
        logger.info(format("Plan journey, arrive by %s so depart by %s", queryTime, departureTime));
        return routeCalculator.calculateRouteWalkAtStart(origin, destination, departureTime, queryDate);
    }

    private TramTime calcDepartTime(TramTime queryTime, int costToDest) {
        int buffer = config.getMaxWait() / 2;
        return queryTime.minusMinutes(costToDest).minusMinutes(buffer);
    }
}
