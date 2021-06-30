package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.CreateQueryTimes;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.repository.ServiceRepository;
import com.tramchester.repository.TransportData;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class RouteCalculator extends RouteCalculatorSupport implements TramRouteCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculator.class);
    private final ServiceRepository serviceRepository;
    private final TramchesterConfig config;
    private final CreateQueryTimes createQueryTimes;

    @Inject
    public RouteCalculator(TransportData transportData, NodeContentsRepository nodeOperations, PathToStages pathToStages,
                           TramchesterConfig config, CreateQueryTimes createQueryTimes,
                           TraversalStateFactory traversalStateFactory, GraphDatabase graphDatabaseService,
                           ProvidesLocalNow providesLocalNow, GraphQuery graphQuery,
                           SortsPositions sortsPosition, MapPathToLocations mapPathToLocations,
                           BetweenRoutesCostRepository routeToRouteCosts, ReasonsToGraphViz reasonToGraphViz) {
        super(graphQuery, pathToStages, nodeOperations, graphDatabaseService,
                traversalStateFactory, providesLocalNow, sortsPosition, mapPathToLocations,
                transportData, config, transportData, routeToRouteCosts, reasonToGraphViz);
        this.serviceRepository = transportData;
        this.config = config;
        this.createQueryTimes = createQueryTimes;
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
                                             Set<Station> unexpanded, boolean walkAtStart) {

        Set<Station> destinations = CompositeStation.expandStations(unexpanded);
        if (destinations.size()!=unexpanded.size()) {
            logger.info("Expanded (composite) destinations from " + unexpanded.size() + " to " + destinations.size());
        }

        List<TramTime> queryTimes = createQueryTimes.generate(journeyRequest.getOriginalTime(), walkAtStart);
        Set<Long> destinationNodeIds = Collections.singleton(endNode.getId());

        // can only be shared as same date and same set of destinations, will eliminate previously seen paths/results
        JourneyConstraints journeyConstraints = new JourneyConstraints(config, serviceRepository, journeyRequest, destinations);

        LowestCostSeen lowestCostSeen = new LowestCostSeen();

        Stream<Journey> results = numChangesRange(journeyRequest).
                flatMap(numChanges -> queryTimes.stream().
                        map(queryTime -> createPathRequest(startNode, queryTime, numChanges, journeyConstraints))).
                flatMap(pathRequest -> findShortestPath(txn, destinationNodeIds, destinations,
                        createServiceReasons(journeyRequest, pathRequest), pathRequest, createPreviousVisits(),
                        lowestCostSeen)).
                limit(journeyRequest.getMaxNumberOfJourneys()).
                map(path -> createJourney(txn, journeyRequest, path, destinations));

        results.onClose(() -> logger.info("Journey stream closed"));

        return results;
    }

    public static class TimedPath {
        private final Path path;
        private final TramTime queryTime;
        private final int numChanges;

        public TimedPath(Path path, TramTime queryTime, int numChanges) {
            this.path = path;
            this.queryTime = queryTime;
            this.numChanges = numChanges;
        }

        public Path getPath() {
            return path;
        }

        public TramTime getQueryTime() {
            return queryTime;
        }

        public int getNumChanges() {
            return numChanges;
        }

        @Override
        public String toString() {
            return "TimedPath{" +
                    "path=" + path +
                    ", queryTime=" + queryTime +
                    ", numChanges=" + numChanges +
                    '}';
        }
    }
}

