package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBoxWithStations;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.RouteInterchanges;
import com.tramchester.repository.RunningRoutesAndServices;
import com.tramchester.repository.TransportData;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class RouteCalculatorForBoxes extends RouteCalculatorSupport {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculatorForBoxes.class);

    private final TramchesterConfig config;
    private final GraphDatabase graphDatabaseService;
    private final ClosedStationsRepository closedStationsRepository;
    private final RunningRoutesAndServices runningRoutesAndService;

    @Inject
    public RouteCalculatorForBoxes(TramchesterConfig config,
                                   TransportData transportData,
                                   GraphDatabase graphDatabaseService, GraphQuery graphQuery, TraversalStateFactory traversalStateFactory,
                                   PathToStages pathToStages,
                                   NodeContentsRepository nodeContentsRepository,
                                   ProvidesNow providesNow,
                                   SortsPositions sortsPosition, MapPathToLocations mapPathToLocations,
                                   BetweenRoutesCostRepository routeToRouteCosts, ReasonsToGraphViz reasonToGraphViz,
                                   ClosedStationsRepository closedStationsRepository, RunningRoutesAndServices runningRoutesAndService,
                                   RouteInterchanges routeInterchanges, RouteCostCalculator routeCostCalculator) {
        super(graphQuery, pathToStages, nodeContentsRepository, graphDatabaseService,
                traversalStateFactory, providesNow, sortsPosition, mapPathToLocations,
                transportData, config, transportData, routeToRouteCosts, reasonToGraphViz, routeInterchanges, routeCostCalculator);
        this.config = config;
        this.graphDatabaseService = graphDatabaseService;
        this.closedStationsRepository = closedStationsRepository;
        this.runningRoutesAndService = runningRoutesAndService;
    }

    public Stream<JourneysForBox> calculateRoutes(LocationSet destinations, JourneyRequest journeyRequest,
                                                  List<BoundingBoxWithStations> grouped) {
        logger.info("Finding routes for bounding boxes");

        // TODO Compute over a range of times
        final TramTime originalTime = journeyRequest.getOriginalTime();

        final TramServiceDate queryDate = journeyRequest.getDate();

        final LowestCostsForDestRoutes lowestCostForDestinations = routeToRouteCosts.getLowestCostCalcutatorFor(destinations);
        RunningRoutesAndServices.FilterForDate routeAndServicesFilter = runningRoutesAndService.getFor(queryDate.getDate());

        Duration maxJourneyDuration = Duration.ofMinutes(journeyRequest.getMaxJourneyDuration());
        final JourneyConstraints journeyConstraints = new JourneyConstraints(config, routeAndServicesFilter, journeyRequest, closedStationsRepository,
                destinations, lowestCostForDestinations, maxJourneyDuration);

        final Set<Long> destinationNodeIds = getDestinationNodeIds(destinations);

        return grouped.parallelStream().map(box -> {

            logger.info(format("Finding shortest path for %s --> %s for %s", box, destinations, journeyRequest));
            final LocationSet startingStations = LocationSet.of(box.getStations());
            final LowestCostSeen lowestCostSeenForBox = new LowestCostSeen();

            final NumberOfChanges numberOfChanges = computeNumberOfChanges(startingStations, destinations);

            final Instant begin = providesNow.getInstant();

            try(Transaction txn = graphDatabaseService.beginTx()) {
                Stream<Journey> journeys = startingStations.stream().
                        filter(start -> !destinations.contains(start)).
                        map(start -> getLocationNodeSafe(txn, start)).
                        flatMap(startNode -> numChangesRange(journeyRequest, numberOfChanges).
                                map(numChanges -> createPathRequest(startNode, queryDate, originalTime, numChanges, journeyConstraints))).
                        flatMap(pathRequest -> findShortestPath(txn, destinationNodeIds, destinations,
                                createServiceReasons(journeyRequest, originalTime), pathRequest, journeyConstraints.getFewestChangesCalculator(),
                                createPreviousVisits(), lowestCostSeenForBox, begin)).
                        map(timedPath -> createJourney(journeyRequest, timedPath, destinations, lowestCostForDestinations));

                Set<Journey> collect = journeys.
                        filter(journey -> !journey.getStages().isEmpty()).
                        limit(journeyRequest.getMaxNumberOfJourneys()).
                        collect(Collectors.toSet());

                // yielding
                return new JourneysForBox(box, collect);
            }
        });

    }

    private NumberOfChanges computeNumberOfChanges(LocationSet starts, LocationSet destinations) {
        return routeToRouteCosts.getNumberOfChanges(starts, destinations);
    }
}
