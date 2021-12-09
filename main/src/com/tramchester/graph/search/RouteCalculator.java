package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.time.CreateQueryTimes;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.repository.*;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class RouteCalculator extends RouteCalculatorSupport implements TramRouteCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculator.class);
    private final TramchesterConfig config;
    private final CreateQueryTimes createQueryTimes;
    private final ClosedStationsRepository closedStationsRepository;
    private final RunningRoutesAndServices runningRoutesAndServices;

    @Inject
    public RouteCalculator(TransportData transportData, NodeContentsRepository nodeOperations, PathToStages pathToStages,
                           TramchesterConfig config, CreateQueryTimes createQueryTimes,
                           TraversalStateFactory traversalStateFactory, GraphDatabase graphDatabaseService,
                           ProvidesNow providesNow, GraphQuery graphQuery,
                           SortsPositions sortsPosition, MapPathToLocations mapPathToLocations,
                           BetweenRoutesCostRepository routeToRouteCosts, ReasonsToGraphViz reasonToGraphViz,
                           ClosedStationsRepository closedStationsRepository, RunningRoutesAndServices runningRoutesAndServices) {
        super(graphQuery, pathToStages, nodeOperations, graphDatabaseService,
                traversalStateFactory, providesNow, sortsPosition, mapPathToLocations,
                transportData, config, transportData, routeToRouteCosts, reasonToGraphViz);
        this.config = config;
        this.createQueryTimes = createQueryTimes;
        this.closedStationsRepository = closedStationsRepository;
        this.runningRoutesAndServices = runningRoutesAndServices;
    }

    @Override
    public Stream<Journey> calculateRoute(Transaction txn, Station startStation, Station destination, JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s (%s) --> %s (%s) for %s",
                startStation.getName(), startStation.getId(), destination.getName(), destination.getId(), journeyRequest));

        Node startNode = getStationNodeSafe(txn, startStation);
        Node endNode = getStationNodeSafe(txn, destination);

        Set<Station> destinations = Collections.singleton(destination);

        final List<TramTime> queryTimes = createQueryTimes.generate(journeyRequest.getOriginalTime());

        NumberOfChanges numberOfChanges =  routeToRouteCosts.getNumberOfChanges(startStation, destination);
        return getJourneyStream(txn, startNode, endNode, journeyRequest, destinations, queryTimes, numberOfChanges).
                limit(journeyRequest.getMaxNumberOfJourneys());
    }

    public Stream<Journey> calculateRouteWalkAtEnd(Transaction txn, Station start, Node endOfWalk, Set<Station> desinationStations,
                                                   JourneyRequest journeyRequest, NumberOfChanges numberOfChanges)
    {
        Node startNode = getStationNodeSafe(txn, start);
        final List<TramTime> queryTimes = createQueryTimes.generate(journeyRequest.getOriginalTime());

        return getJourneyStream(txn, startNode, endOfWalk, journeyRequest, desinationStations, queryTimes, numberOfChanges).
                limit(journeyRequest.getMaxNumberOfJourneys());
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStart(Transaction txn, Set<StationWalk> stationWalks, Node startOfWalkNode, Station destination,
                                                     JourneyRequest journeyRequest, NumberOfChanges numberOfChanges) {

        final InitialWalksFinished finished = new InitialWalksFinished(journeyRequest, stationWalks);
        Node endNode = getStationNodeSafe(txn, destination);
        Set<Station> destinations = Collections.singleton(destination);
        final List<TramTime> queryTimes = createQueryTimes.generate(journeyRequest.getOriginalTime(), stationWalks);

        return getJourneyStream(txn, startOfWalkNode, endNode, journeyRequest, destinations, queryTimes, numberOfChanges).
                takeWhile(finished::notDoneYet);
    }

    public Stream<Journey> calculateRouteWalkAtStartAndEnd(Transaction txn, Set<StationWalk> stationWalks, Node startNode, Node endNode,
                                                           Set<Station> destinationStations, JourneyRequest journeyRequest,
                                                           NumberOfChanges numberOfChanges) {

        final InitialWalksFinished finished = new InitialWalksFinished(journeyRequest, stationWalks);
        final List<TramTime> queryTimes = createQueryTimes.generate(journeyRequest.getOriginalTime(), stationWalks);

        return getJourneyStream(txn, startNode, endNode, journeyRequest, destinationStations, queryTimes, numberOfChanges).
                takeWhile(finished::notDoneYet);
    }

    private Stream<Journey> getJourneyStream(Transaction txn, Node startNode, Node endNode, JourneyRequest journeyRequest,
                                             Set<Station> unexpanded, List<TramTime> queryTimes, NumberOfChanges numberOfChanges) {

        final Set<Station> destinations = CompositeStation.expandStations(unexpanded);
        if (destinations.size()!=unexpanded.size()) {
            logger.info("Expanded (composite) destinations from " + unexpanded.size() + " to " + destinations.size());
        }

        final TramServiceDate queryDate = journeyRequest.getDate();
        final Set<Long> destinationNodeIds = Collections.singleton(endNode.getId());

        // can only be shared as same date and same set of destinations, will eliminate previously seen paths/results
        LowestCostsForRoutes lowestCostsForRoutes = routeToRouteCosts.getLowestCostCalcutatorFor(destinations);
        final JourneyConstraints journeyConstraints = new JourneyConstraints(config, runningRoutesAndServices.getFor(queryDate.getDate()),
                journeyRequest, closedStationsRepository, destinations, lowestCostsForRoutes);

        logger.info("Journey Constraints: " + journeyConstraints);
        logger.info("Query times: " + queryTimes);

        final LowestCostSeen lowestCostSeen = new LowestCostSeen();

        final Instant begin = providesNow.getInstant();
        final Stream<Journey> results = numChangesRange(journeyRequest, numberOfChanges).
                flatMap(numChanges -> queryTimes.stream().
                        map(queryTime -> createPathRequest(startNode, queryDate, queryTime, numChanges, journeyConstraints))).
                flatMap(pathRequest -> findShortestPath(txn, destinationNodeIds, destinations,
                        createServiceReasons(journeyRequest, pathRequest), pathRequest, lowestCostsForRoutes, createPreviousVisits(),
                        lowestCostSeen, begin)).
                map(path -> createJourney(journeyRequest, path, destinations, lowestCostsForRoutes));

        //noinspection ResultOfMethodCallIgnored
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

