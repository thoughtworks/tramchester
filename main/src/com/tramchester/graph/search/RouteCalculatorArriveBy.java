package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.time.InvalidDurationException;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.RouteCostCalculator;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
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
    public Stream<Journey> calculateRoute(Transaction txn, Location<?> startStation, Location<?> destination, JourneyRequest journeyRequest) {
        try {
            Duration costToDest = costCalculator.getAverageCostBetween(txn, startStation, destination, journeyRequest.getDate());
            Duration maxInitialWait = RouteCalculatorSupport.getMaxInitialWaitFor(startStation, config);
            JourneyRequest departureTime = calcDepartTime(journeyRequest, costToDest, maxInitialWait);
            logger.info(format("Plan journey, arrive by %s so depart by %s", journeyRequest, departureTime));
            return routeCalculator.calculateRoute(txn, startStation, destination, departureTime);
        } catch (InvalidDurationException invalidDurationException) {
            logger.error("Unable to compute cost from %s to %s for %s".formatted(startStation.getId(), destination.getId(), journeyRequest),
                    invalidDurationException);
            return Stream.empty();
        }

    }

    @Override
    public Stream<Journey> calculateRouteWalkAtEnd(Transaction txn, Location<?> start, Node endOfWalk, LocationSet  destStations,
                                                   JourneyRequest journeyRequest, NumberOfChanges numberOfChanges) {
        try {
            Duration costToDest = costCalculator.getAverageCostBetween(txn, start, endOfWalk, journeyRequest.getDate());
            Duration maxInitialWait = RouteCalculatorSupport.getMaxInitialWaitFor(start, config);
            JourneyRequest departureTime = calcDepartTime(journeyRequest, costToDest, maxInitialWait);
            logger.info(format("Plan journey, arrive by %s so depart by %s", journeyRequest, departureTime));
            return routeCalculator.calculateRouteWalkAtEnd(txn, start, endOfWalk, destStations, departureTime, numberOfChanges);
        } catch (InvalidDurationException invalidDurationException) {
            logger.error("Unable to compute cost from %s to node %s for %s".formatted(start.getId(), endOfWalk.getId(), journeyRequest),
                    invalidDurationException);
            return Stream.empty();
        }
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStart(Transaction txn, Set<StationWalk> stationWalks, Node origin, Location<?> destination,
                                                     JourneyRequest journeyRequest, NumberOfChanges numberOfChanges) {
        try {
            Duration costToDest = costCalculator.getAverageCostBetween(txn, origin, destination, journeyRequest.getDate());
            Duration maxInitialWait = RouteCalculatorSupport.getMaxInitialWaitFor(stationWalks, config);
            JourneyRequest departureTime = calcDepartTime(journeyRequest, costToDest, maxInitialWait);
            logger.info(format("Plan journey, arrive by %s so depart by %s", journeyRequest, departureTime));
            return routeCalculator.calculateRouteWalkAtStart(txn, stationWalks, origin, destination, departureTime, numberOfChanges);
        } catch (InvalidDurationException invalidDurationException) {
            logger.error("Unable to compute cost from node %s to %s for %s".formatted(origin.getId(), destination.getId(), journeyRequest),
                    invalidDurationException);
            return Stream.empty();
        }
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStartAndEnd(Transaction txn, Set<StationWalk> stationWalks, Node startNode,
                                                           Node endNode, LocationSet destinationStations,
                                                           JourneyRequest journeyRequest, NumberOfChanges numberOfChanges) {
        try {
            Duration costToDest = costCalculator.getAverageCostBetween(txn, startNode, endNode, journeyRequest.getDate());
            Duration maxInitialWait = RouteCalculatorSupport.getMaxInitialWaitFor(stationWalks, config);
            JourneyRequest departureTime = calcDepartTime(journeyRequest, costToDest, maxInitialWait);
            logger.info(format("Plan journey, arrive by %s so depart by %s", journeyRequest, departureTime));
            return routeCalculator.calculateRouteWalkAtStartAndEnd(txn, stationWalks, startNode, endNode, destinationStations, departureTime, numberOfChanges);
        } catch (InvalidDurationException invalidDurationException) {
            logger.error("Unable to compute cost from node %s to node %s [walks:%s] for %s".formatted(
                    startNode.getId(), endNode.getId(), stationWalks, journeyRequest), invalidDurationException);
            return Stream.empty();
        }
    }


    private JourneyRequest calcDepartTime(JourneyRequest originalRequest, Duration costToDest, Duration maxInitialWait) {
        TramTime queryTime = originalRequest.getOriginalTime();

        final TramTime departTime = queryTime.minus(costToDest);

        //final int waitTime = config.getMaxInitialWait() / 2;
        final int waitTimeMinutes = Math.toIntExact(maxInitialWait.toMinutes() / 2);
        TramTime newQueryTime = departTime.minusMinutes(waitTimeMinutes);

        return new JourneyRequest(originalRequest, newQueryTime);
    }



}
