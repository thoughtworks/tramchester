package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneysForBox;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.CreateQueryTimes;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBoxWithStations;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.*;
import com.tramchester.repository.TramReachabilityRepository;
import com.tramchester.repository.TransportData;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class RouteCalculator implements TramRouteCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculator.class);

    private final MapPathToStages pathToStages;
    private final TramchesterConfig config;
    private final NodeContentsRepository nodeOperations;
    private final NodeTypeRepository nodeTypeRepository;

    private final TransportData transportData;
    private final TramReachabilityRepository tramReachabilityRepository;
    private final CreateQueryTimes createQueryTimes;
    private final GraphDatabase graphDatabaseService;
    private final ProvidesLocalNow providesLocalNow;
    private final GraphQuery graphQuery;
    private final SortsPositions sortsPosition;
    private final MapPathToLocations mapPathToLocations;

    @Inject
    public RouteCalculator(TransportData transportData, NodeContentsRepository nodeOperations, MapPathToStages pathToStages,
                           TramchesterConfig config, TramReachabilityRepository tramReachabilityRepository,
                           CreateQueryTimes createQueryTimes, GraphDatabase graphDatabaseService,
                           ProvidesLocalNow providesLocalNow, GraphQuery graphQuery, NodeTypeRepository nodeTypeRepository,
                           SortsPositions sortsPosition, MapPathToLocations mapPathToLocations) {
        this.transportData = transportData;
        this.nodeOperations = nodeOperations;
        this.pathToStages = pathToStages;
        this.config = config;
        this.tramReachabilityRepository = tramReachabilityRepository;
        this.createQueryTimes = createQueryTimes;
        this.graphDatabaseService = graphDatabaseService;
        this.providesLocalNow = providesLocalNow;
        this.graphQuery = graphQuery;
        this.nodeTypeRepository = nodeTypeRepository;

        this.sortsPosition = sortsPosition;
        this.mapPathToLocations = mapPathToLocations;
    }

    @Override
    public Stream<Journey> calculateRoute(Transaction txn, Station startStation, Station destination, JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s (%s) --> %s (%s) for %s",
                startStation.getName(), startStation.getId(), destination.getName(), destination.getId(), journeyRequest));

        Node startNode = getStationNodeSafe(txn, startStation);
        Node endNode = getStationNodeSafe(txn, destination);

        Set<Station> destinations = Collections.singleton(destination);

        return getJourneyStream(txn, startNode, endNode, journeyRequest, destinations, false);
    }

    private Node getStationNodeSafe(Transaction txn, Station station) {
        Node stationNode = graphQuery.getStationNode(txn, station);
        if (stationNode==null) {
            String msg = "Unable to find station graph node based on " + station;
            logger.warn(msg);
            throw new RuntimeException(msg);
        }
        return stationNode;
    }

    public Stream<Journey> calculateRouteWalkAtEnd(Transaction txn, Station start, Node endOfWalk, Set<Station> desinationStations,
                                                   JourneyRequest journeyRequest)
    {
        Node startNode = getStationNodeSafe(txn, start);
        return getJourneyStream(txn, startNode, endOfWalk, journeyRequest, desinationStations, false);
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStart(Transaction txn, Node startOfWalkNode, Station destination,
                                                     JourneyRequest journeyRequest) {
        Node endNode = getStationNodeSafe(txn, destination);
        Set<Station> destinations = Collections.singleton(destination);
        return getJourneyStream(txn, startOfWalkNode, endNode, journeyRequest, destinations, true);
    }

    public Stream<Journey> calculateRouteWalkAtStartAndEnd(Transaction txn, Node startNode, Node endNode,
                                                           Set<Station> destinationStations, JourneyRequest journeyRequest) {
        return getJourneyStream(txn, startNode, endNode, journeyRequest, destinationStations, true);
    }

    private Stream<Journey> getJourneyStream(Transaction txn, Node startNode, Node endNode, JourneyRequest journeyRequest,
                                             Set<Station> destinations, boolean walkAtStart) {

        List<TramTime> queryTimes = createQueryTimes.generate(journeyRequest.getTime(), walkAtStart);
        Set<Long> destinationNodeIds = Collections.singleton(endNode.getId());

        // can only be shared as same date and same set of destinations, will eliminate previously seen paths/results
        PreviousSuccessfulVisits previousSuccessfulVisit = new PreviousSuccessfulVisits();
        JourneyConstraints journeyConstraints = new JourneyConstraints(config, transportData, journeyRequest, destinations);

        return numChangesRange(journeyRequest).
                flatMap(numChanges -> queryTimes.stream().
                        map(queryTime-> new PathRequest(startNode, queryTime, numChanges, journeyConstraints))).
                flatMap(pathRequest -> findShortestPath(txn, destinationNodeIds, destinations, previousSuccessfulVisit,
                        createServiceReasons(journeyRequest, pathRequest.queryTime, pathRequest.numChanges), pathRequest)).
                map(path -> createJourney(journeyRequest, path));
    }

    public Stream<JourneysForBox> calculateRoutes(Set<Station> destinations, JourneyRequest journeyRequest,
                                                  List<BoundingBoxWithStations> grouped, long numberToFind) {
        logger.info("Finding routes for bounding boxes");

        final TramTime time = journeyRequest.getTime();

        JourneyConstraints journeyConstraints = new JourneyConstraints(config, transportData, journeyRequest, destinations);

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
                                previousSuccessfulVisit, createServiceReasons(journeyRequest, time, pathRequest.numChanges), pathRequest)).
                        map(timedPath -> createJourney(journeyRequest, timedPath));

                // TODO Limit here, or return the stream?
                List<Journey> collect = journeys.limit(numberToFind).collect(Collectors.toList());

                // yielding
                return new JourneysForBox(box, collect);
            }
        });

    }

    @NotNull
    private Stream<Integer> numChangesRange(JourneyRequest journeyRequest) {
        return IntStream.rangeClosed(0, journeyRequest.getMaxChanges()).boxed();
    }

    @NotNull
    private ServiceReasons createServiceReasons(JourneyRequest journeyRequest, TramTime time, int numChanges) {
        return new ServiceReasons(journeyRequest, time, providesLocalNow, numChanges);
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

    private Stream<TimedPath> findShortestPath(Transaction txn, Set<Long> destinationNodeIds,
                                               final Set<Station> endStations, PreviousSuccessfulVisits previousSuccessfulVisit,
                                               ServiceReasons reasons, PathRequest pathRequest) {

        TramNetworkTraverser tramNetworkTraverser = new TramNetworkTraverser(graphDatabaseService, transportData,
                pathRequest.serviceHeuristics,
                sortsPosition, nodeOperations, endStations, config, nodeTypeRepository, destinationNodeIds, reasons);

        return tramNetworkTraverser.
                findPaths(txn, pathRequest.startNode, previousSuccessfulVisit).
                map(path -> new TimedPath(path, pathRequest.queryTime));
    }

    @NotNull
    private Journey createJourney(JourneyRequest journeyRequest, TimedPath path) {
        return new Journey(pathToStages.mapDirect(path, journeyRequest),
                path.getQueryTime(), mapPathToLocations.mapToLocations(path.getPath()));
    }

    @NotNull
    private ServiceHeuristics createHeuristics(TramTime time, JourneyConstraints journeyConstraints, int maxNumChanges) {
        return new ServiceHeuristics(transportData, nodeOperations, tramReachabilityRepository,
                journeyConstraints, time, maxNumChanges);
    }

    private class PathRequest {
        private final Node startNode;
        protected final TramTime queryTime;
        protected final int numChanges;
        private final ServiceHeuristics serviceHeuristics;


        private PathRequest(Node startNode, TramTime queryTime, int numChanges, JourneyConstraints journeyConstraints) {
            this.startNode = startNode;
            this.queryTime = queryTime;
            this.numChanges = numChanges;
            this.serviceHeuristics = createHeuristics(queryTime, journeyConstraints, numChanges);
        }
    }


    public static class TimedPath {
        private final Path path;
        private final TramTime queryTime;

        protected TimedPath(Path path, TramTime queryTime) {
            this.path = path;
            this.queryTime = queryTime;
        }

        public Path getPath() {
            return path;
        }

        public TramTime getQueryTime() {
            return queryTime;
        }
    }
}

