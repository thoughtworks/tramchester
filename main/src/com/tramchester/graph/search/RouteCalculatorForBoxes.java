package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.JourneysForBox;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBoxWithStations;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.repository.ServiceRepository;
import com.tramchester.repository.TransportData;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
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
    private final ServiceRepository serviceRepository;
    private final GraphDatabase graphDatabaseService;

    @Inject
    public RouteCalculatorForBoxes(TramchesterConfig config,
                                   TransportData transportData,
                                   GraphDatabase graphDatabaseService, GraphQuery graphQuery, TraversalStateFactory traversalStateFactory,
                                   PathToStages pathToStages,
                                   NodeContentsRepository nodeContentsRepository,
                                   ProvidesNow providesNow,
                                   SortsPositions sortsPosition, MapPathToLocations mapPathToLocations,
                                   BetweenRoutesCostRepository routeToRouteCosts, ReasonsToGraphViz reasonToGraphViz) {
        super(graphQuery, pathToStages, nodeContentsRepository, graphDatabaseService,
                traversalStateFactory, providesNow, sortsPosition, mapPathToLocations,
                transportData, config, transportData, routeToRouteCosts, reasonToGraphViz);
        this.config = config;
        this.serviceRepository = transportData;
        this.graphDatabaseService = graphDatabaseService;
    }

    public Stream<JourneysForBox> calculateRoutes(Set<Station> destinations, JourneyRequest journeyRequest,
                                                  List<BoundingBoxWithStations> grouped) {
        logger.info("Finding routes for bounding boxes");

        // TODO Compute over a range of times
        final TramTime originalTime = journeyRequest.getOriginalTime();

        LowestCostsForRoutes lowestCostForDestinations = routeToRouteCosts.getLowestCostCalcutatorFor(destinations);
        JourneyConstraints journeyConstraints = new JourneyConstraints(config, serviceRepository,
                journeyRequest, destinations, lowestCostForDestinations);

        Set<Long> destinationNodeIds = getDestinationNodeIds(destinations);

        return grouped.parallelStream().map(box -> {

            logger.info(format("Finding shortest path for %s --> %s for %s", box, destinations, journeyRequest));
            Set<Station> startingStations = box.getStaions();
            LowestCostSeen lowestCostSeenForBox = new LowestCostSeen();

            // TODO Compute the max from the stations in the box?
            NumberOfChanges numberOfChanges = computeNumberOfChanges(startingStations, destinations);

            Instant begin = providesNow.getInstant();

            try(Transaction txn = graphDatabaseService.beginTx()) {
                Stream<Journey> journeys = startingStations.stream().
                        filter(start -> !destinations.contains(start)).
                        map(start -> getStationNodeSafe(txn, start)).
                        flatMap(startNode -> numChangesRange(journeyRequest, numberOfChanges).
                                map(numChanges -> createPathRequest(startNode, originalTime, numChanges, journeyConstraints))).
                        flatMap(pathRequest -> findShortestPath(txn, destinationNodeIds, destinations,
                                createServiceReasons(journeyRequest, originalTime), pathRequest, journeyConstraints.getFewestChangesCalculator(),
                                createPreviousVisits(), lowestCostSeenForBox, begin)).
                        map(timedPath -> createJourney(txn, journeyRequest, timedPath, destinations, lowestCostForDestinations));

                List<Journey> collect = journeys.
                        filter(journey -> !journey.getStages().isEmpty()).
                        limit(journeyRequest.getMaxNumberOfJourneys()).collect(Collectors.toList());

                // yielding
                return new JourneysForBox(box, collect);
            }
        });

    }

    private NumberOfChanges computeNumberOfChanges(Set<Station> starts, Set<Station> destinations) {
        return routeToRouteCosts.getNumberOfChanges(starts, destinations);
    }
}
