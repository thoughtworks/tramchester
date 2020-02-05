package com.tramchester.graph;

import com.tramchester.domain.Journey;
import com.tramchester.domain.StationWalk;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.format;

public class RouteCalculatorArriveBy implements TramRouteCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculatorArriveBy.class);

    private final TramRouteReachable tramRouteReachable;
    private final RouteCalculator routeCalculator;

    public RouteCalculatorArriveBy(TramRouteReachable tramRouteReachable, RouteCalculator routeCalculator) {
        this.tramRouteReachable = tramRouteReachable;
        this.routeCalculator = routeCalculator;
    }

    @Override
    public Stream<Journey> calculateRoute(String startStationId, String destinationId, TramTime queryTime,
                                          TramServiceDate queryDate) {
        int costToDest = tramRouteReachable.getApproxCostBetween(startStationId, destinationId);
        TramTime departureTime = queryTime.minusMinutes(costToDest);
        logger.info(format("Plan journey, arrive by %s so depart by %s", queryDate, departureTime));
        return routeCalculator.calculateRoute(startStationId, destinationId, departureTime, queryDate);
    }

    /// TODO For walking have to do the cost calculation after adding the walking nodes to the graph

    @Override
    public Stream<Journey> calculateRouteWalkAtEnd(String startId, LatLong destination, List<StationWalk> walksToDest,
                                                   TramTime queryTime, TramServiceDate queryDate) {
        logger.error("Not implemented");
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStart(LatLong origin, List<StationWalk> walksToStartStations,
                                                     String destinationId, TramTime queryTime, TramServiceDate queryDate) {
        logger.error("Not implemented");
        throw new RuntimeException("Not implemented yet");

    }
}
