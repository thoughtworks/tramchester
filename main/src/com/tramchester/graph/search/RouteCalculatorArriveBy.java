package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.RouteCostCalculator;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class RouteCalculatorArriveBy implements TramRouteCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculatorArriveBy.class);

    private final RouteCostCalculator costCalculator;
    private final TramRouteCalculator routeCalculator;
    private final TramchesterConfig config;

    @Inject
    public RouteCalculatorArriveBy(RouteCostCalculator costCalculator, RouteCalculator routeCalculator, TramchesterConfig config) {
        this.costCalculator = costCalculator;
        this.routeCalculator = routeCalculator;
        this.config = config;
    }

    @Override
    public Stream<Journey> calculateRoute(Transaction txn, Station startStation, Station destination, JourneyRequest journeyRequest) {
        int costToDest = costCalculator.getAverageCostBetween(txn, startStation, destination, journeyRequest.getDate());
        JourneyRequest departureTime = calcDepartTime(journeyRequest, costToDest);
        logger.info(format("Plan journey, arrive by %s so depart by %s", journeyRequest, departureTime));
        return routeCalculator.calculateRoute(txn, startStation, destination, departureTime);
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtEnd(Transaction txn, Station start, Node endOfWalk, Set<Station> destStations,
                                                   JourneyRequest journeyRequest, NumberOfChanges numberOfChanges) {
        int costToDest = costCalculator.getAverageCostBetween(txn, start, endOfWalk, journeyRequest.getDate());
        JourneyRequest departureTime = calcDepartTime(journeyRequest, costToDest);
        logger.info(format("Plan journey, arrive by %s so depart by %s", journeyRequest, departureTime));
        return routeCalculator.calculateRouteWalkAtEnd(txn, start, endOfWalk, destStations, departureTime, numberOfChanges);
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStart(Transaction txn, Set<StationWalk> stationWalks, Node origin, Station destination,
                                                     JourneyRequest journeyRequest, NumberOfChanges numberOfChanges) {
        int costToDest = costCalculator.getAverageCostBetween(txn, origin, destination, journeyRequest.getDate());
        JourneyRequest departureTime = calcDepartTime(journeyRequest, costToDest);
        logger.info(format("Plan journey, arrive by %s so depart by %s", journeyRequest, departureTime));
        return routeCalculator.calculateRouteWalkAtStart(txn, stationWalks, origin, destination, departureTime, numberOfChanges);
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStartAndEnd(Transaction txn, Set<StationWalk> stationWalks, Node startNode,
                                                           Node endNode, Set<Station> destinationStations,
                                                           JourneyRequest journeyRequest, NumberOfChanges numberOfChanges) {
        int costToDest = costCalculator.getAverageCostBetween(txn, startNode, endNode, journeyRequest.getDate());
        JourneyRequest departureTime = calcDepartTime(journeyRequest, costToDest);
        logger.info(format("Plan journey, arrive by %s so depart by %s", journeyRequest, departureTime));
        return routeCalculator.calculateRouteWalkAtStartAndEnd(txn, stationWalks, startNode, endNode, destinationStations, departureTime, numberOfChanges);
    }

    private JourneyRequest calcDepartTime(JourneyRequest originalRequest, int costToDest) {
        TramTime queryTime = originalRequest.getOriginalTime();

        final TramTime departTime;
        if (costToDest<0) {
            logger.error("Could not find an approx cost to the destination for request: " + originalRequest);
            departTime = queryTime;
        } else {
            departTime = queryTime.minusMinutes(costToDest);
        }
        // TODO Handle buses & trains wait time here?
        final int waitTime = config.getMaxInitialWait() / 2;
        TramTime newQueryTime = departTime.minusMinutes(waitTime);

        return new JourneyRequest(originalRequest, newQueryTime);
    }
}
