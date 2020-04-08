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
    public Stream<Journey> calculateRoute(String startStationId, Station destination, JourneyRequest journeyRequest) {
        int costToDest = routeReachable.getApproxCostBetween(startStationId, destination.getId());
        JourneyRequest departureTime = calcDepartTime(journeyRequest, costToDest);
        logger.info(format("Plan journey, arrive by %s so depart by %s", journeyRequest, departureTime));
        return routeCalculator.calculateRoute(startStationId, destination, departureTime);
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtEnd(String startId, Node endOfWalk, List<Station> destStations,
                                                   JourneyRequest journeyRequest) {
        int costToDest = routeReachable.getApproxCostBetween(startId, endOfWalk);
        JourneyRequest departureTime = calcDepartTime(journeyRequest, costToDest);
        logger.info(format("Plan journey, arrive by %s so depart by %s", journeyRequest, departureTime));
        return routeCalculator.calculateRouteWalkAtEnd(startId, endOfWalk, destStations, departureTime);
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStart(Node origin, Station destination, JourneyRequest journeyRequest) {
        int costToDest = routeReachable.getApproxCostBetween(origin, destination.getId());
        JourneyRequest departureTime = calcDepartTime(journeyRequest, costToDest);
        logger.info(format("Plan journey, arrive by %s so depart by %s", journeyRequest, departureTime));
        return routeCalculator.calculateRouteWalkAtStart(origin, destination, departureTime);
    }

    private JourneyRequest calcDepartTime(JourneyRequest journeyRequest, int costToDest) {
        int buffer = config.getMaxWait() / 2; // TODO CONFIG
        TramTime queryTime = journeyRequest.getTime();
        TramTime computedDepartTime = queryTime.minusMinutes(costToDest).minusMinutes(buffer);
        return new JourneyRequest(journeyRequest.getDate(), computedDepartTime, true);
    }
}
