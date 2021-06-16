package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneysForBox;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBoxWithStations;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.caches.PreviousVisits;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.repository.ServiceRepository;
import com.tramchester.repository.TransportData;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
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
                                   ProvidesLocalNow providesLocalNow,
                                   SortsPositions sortsPosition, MapPathToLocations mapPathToLocations,
                                   RouteToRouteCosts routeToRouteCosts, ReasonsToGraphViz reasonToGraphViz) {
        super(graphQuery, pathToStages, nodeContentsRepository, graphDatabaseService,
                traversalStateFactory, providesLocalNow, sortsPosition, mapPathToLocations,
                transportData, config, transportData, routeToRouteCosts, reasonToGraphViz);
        this.config = config;
        this.serviceRepository = transportData;
        this.graphDatabaseService = graphDatabaseService;
    }

    public Stream<JourneysForBox> calculateRoutes(Set<Station> destinations, JourneyRequest journeyRequest,
                                                  List<BoundingBoxWithStations> grouped) {
        logger.info("Finding routes for bounding boxes");

        final TramTime time = journeyRequest.getTime();

        JourneyConstraints journeyConstraints = new JourneyConstraints(config, serviceRepository, journeyRequest, destinations);

        Set<Long> destinationNodeIds = getDestinationNodeIds(destinations);

        return grouped.parallelStream().map(box -> {

            // can only be shared as same date and same set of destinations, will eliminate previously seen paths/results
            // trying to share across boxes causes RouteCalulcatorForBoundingBoxTest tests to fail
            PreviousVisits previousSuccessfulVisit = createPreviousVisits();

            logger.info(format("Finding shortest path for %s --> %s for %s", box, destinations, journeyRequest));
            Set<Station> startingStations = box.getStaions();

            try(Transaction txn = graphDatabaseService.beginTx()) {
                Stream<Journey> journeys = startingStations.stream().
                        filter(start -> !destinations.contains(start)).
                        map(start -> getStationNodeSafe(txn, start)).
                        flatMap(startNode -> numChangesRange(journeyRequest).
                                map(numChanges -> createPathRequest(startNode, time, numChanges, journeyConstraints))).
                        flatMap(pathRequest -> findShortestPath(txn, destinationNodeIds, destinations,
                                createServiceReasons(journeyRequest, time), pathRequest, previousSuccessfulVisit)).
                        map(timedPath -> createJourney(txn, journeyRequest, timedPath, destinations));

                List<Journey> collect = journeys.
                        filter(journey -> !journey.getStages().isEmpty()).
                        limit(journeyRequest.getMaxNumberOfJourneys()).collect(Collectors.toList());

                // yielding
                previousSuccessfulVisit.reportStatsFor(journeyRequest);
                previousSuccessfulVisit.clear();
                return new JourneysForBox(box, collect);
            }
        });

    }
}
