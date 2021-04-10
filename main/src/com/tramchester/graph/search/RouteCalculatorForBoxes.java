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
import com.tramchester.graph.*;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.ReachabilityRepository;
import com.tramchester.repository.ServiceRepository;
import com.tramchester.repository.TransportData;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Entity;
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
                                   GraphDatabase graphDatabaseService, GraphQuery graphQuery, MapPathToStages pathToStages,
                                   NodeContentsRepository nodeOperations, NodeTypeRepository nodeTypeRepository,
                                   ReachabilityRepository reachabilityRepository, ProvidesLocalNow providesLocalNow,
                                   SortsPositions sortsPosition, MapPathToLocations mapPathToLocations,
                                   CompositeStationRepository compositeStationRepository) {
        super(graphQuery, pathToStages, nodeOperations, nodeTypeRepository, reachabilityRepository, graphDatabaseService,
                providesLocalNow, sortsPosition, mapPathToLocations, compositeStationRepository, transportData, config, transportData);
        this.config = config;
        this.serviceRepository = transportData;
        this.graphDatabaseService = graphDatabaseService;
    }

    public Stream<JourneysForBox> calculateRoutes(Set<Station> destinations, JourneyRequest journeyRequest,
                                                  List<BoundingBoxWithStations> grouped, long numberToFind) {
        logger.info("Finding routes for bounding boxes");

        final TramTime time = journeyRequest.getTime();

        JourneyConstraints journeyConstraints = new JourneyConstraints(config, serviceRepository, journeyRequest, destinations);

        Set<Long> destinationNodeIds = getDestinationNodeIds(destinations);

        return grouped.parallelStream().map(box -> {

            // can only be shared as same date and same set of destinations, will eliminate previously seen paths/results
            // trying to share across boxes causes RouteCalulcatorForBoundingBoxTest tests to fail
            final PreviousSuccessfulVisits previousSuccessfulVisit = new PreviousSuccessfulVisits();

            logger.info(format("Finding shortest path for %s --> %s for %s", box, destinations, journeyRequest));
            Set<Station> startingStations = box.getStaions();

            try(Transaction txn = graphDatabaseService.beginTx()) {
                Stream<Journey> journeys = startingStations.stream().
                        filter(start -> !destinations.contains(start)).
                        map(start -> getStationNodeSafe(txn, start)).
                        flatMap(startNode -> numChangesRange(journeyRequest).
                                map(numChanges -> new PathRequest(startNode, time, numChanges, journeyConstraints))).
                        flatMap(pathRequest -> findShortestPath(txn, destinationNodeIds, destinations,
                                previousSuccessfulVisit, createServiceReasons(journeyRequest, time, pathRequest), pathRequest)).
                        map(timedPath -> createJourney(journeyRequest, timedPath));

                // TODO Limit here, or return the stream?
                List<Journey> collect = journeys.limit(numberToFind).collect(Collectors.toList());

                // yielding
                return new JourneysForBox(box, collect);
            }
        });

    }

    @NotNull
    private Set<Long> getDestinationNodeIds(Set<Station> destinations) {
        Set<Long> destinationNodeIds;
        try(Transaction txn = graphDatabaseService.beginTx()) {
            destinationNodeIds = destinations.stream().
                    map(station -> getStationNodeSafe(txn, station)).
                    map(Entity::getId).
                    collect(Collectors.toSet());
        }
        return destinationNodeIds;
    }
}
