package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.time.CreateQueryTimes;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.RouteInterchanges;
import com.tramchester.repository.RunningRoutesAndServices;
import com.tramchester.repository.TransportData;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
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
    private final CacheMetrics cacheMetrics;

    @Inject
    public RouteCalculator(TransportData transportData, NodeContentsRepository nodeOperations, PathToStages pathToStages,
                           TramchesterConfig config, CreateQueryTimes createQueryTimes,
                           TraversalStateFactory traversalStateFactory, GraphDatabase graphDatabaseService,
                           ProvidesNow providesNow, GraphQuery graphQuery,
                           SortsPositions sortsPosition, MapPathToLocations mapPathToLocations,
                           BetweenRoutesCostRepository routeToRouteCosts, ReasonsToGraphViz reasonToGraphViz,
                           ClosedStationsRepository closedStationsRepository, RunningRoutesAndServices runningRoutesAndServices,
                           RouteInterchanges routeInterchanges, CacheMetrics cacheMetrics, RouteCostCalculator routeCostCalculator) {
        super(graphQuery, pathToStages, nodeOperations, graphDatabaseService,
                traversalStateFactory, providesNow, sortsPosition, mapPathToLocations,
                transportData, config, transportData, routeToRouteCosts, reasonToGraphViz, routeInterchanges, routeCostCalculator);
        this.config = config;
        this.createQueryTimes = createQueryTimes;
        this.closedStationsRepository = closedStationsRepository;
        this.runningRoutesAndServices = runningRoutesAndServices;
        this.cacheMetrics = cacheMetrics;
    }

    @Override
    public Stream<Journey> calculateRoute(Transaction txn, Location<?> start, Location<?> destination, JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s (%s) --> %s (%s) for %s",
                start.getName(), start.getId(), destination.getName(), destination.getId(), journeyRequest));

        Node startNode = getLocationNodeSafe(txn, start);
        Node endNode = getLocationNodeSafe(txn, destination);

        LocationSet destinations = LocationSet.singleton(destination);

        final List<TramTime> queryTimes = createQueryTimes.generate(journeyRequest.getOriginalTime());

        NumberOfChanges numberOfChanges =  routeToRouteCosts.getNumberOfChanges(start, destination);
        return getJourneyStream(txn, startNode, endNode, journeyRequest, destinations, queryTimes, numberOfChanges).
                limit(journeyRequest.getMaxNumberOfJourneys());
    }

    public Stream<Journey> calculateRouteWalkAtEnd(Transaction txn, Location<?> start, Node endOfWalk, LocationSet destinations,
                                                   JourneyRequest journeyRequest, NumberOfChanges numberOfChanges)
    {
        Node startNode = getLocationNodeSafe(txn, start);
        final List<TramTime> queryTimes = createQueryTimes.generate(journeyRequest.getOriginalTime());

        return getJourneyStream(txn, startNode, endOfWalk, journeyRequest, destinations, queryTimes, numberOfChanges).
                limit(journeyRequest.getMaxNumberOfJourneys());
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStart(Transaction txn, Set<StationWalk> stationWalks, Node startOfWalkNode, Location<?> destination,
                                                     JourneyRequest journeyRequest, NumberOfChanges numberOfChanges) {

        final InitialWalksFinished finished = new InitialWalksFinished(journeyRequest, stationWalks);
        Node endNode = getLocationNodeSafe(txn, destination);
        LocationSet destinations = LocationSet.singleton(destination);
        final List<TramTime> queryTimes = createQueryTimes.generate(journeyRequest.getOriginalTime(), stationWalks);

        return getJourneyStream(txn, startOfWalkNode, endNode, journeyRequest, destinations, queryTimes, numberOfChanges).
                takeWhile(finished::notDoneYet);
    }

    public Stream<Journey> calculateRouteWalkAtStartAndEnd(Transaction txn, Set<StationWalk> stationWalks, Node startNode, Node endNode,
                                                           LocationSet destinationStations, JourneyRequest journeyRequest,
                                                           NumberOfChanges numberOfChanges) {

        final InitialWalksFinished finished = new InitialWalksFinished(journeyRequest, stationWalks);
        final List<TramTime> queryTimes = createQueryTimes.generate(journeyRequest.getOriginalTime(), stationWalks);

        return getJourneyStream(txn, startNode, endNode, journeyRequest, destinationStations, queryTimes, numberOfChanges).
                takeWhile(finished::notDoneYet);
    }

    private Stream<Journey> getJourneyStream(Transaction txn, Node startNode, Node endNode, JourneyRequest journeyRequest,
                                             LocationSet destinations, List<TramTime> queryTimes, NumberOfChanges numberOfChanges) {

        if (numberOfChanges.getMin()==Integer.MAX_VALUE) {
            logger.error(format("Computed min number of changes is MAX_VALUE, journey %s is not possible?", journeyRequest));
            return Stream.empty();
        }

        // TODO groups will need their own method, or get expanded much earlier on
        //final Set<Station> destinations = GroupedStations.expandStations(unexpanded);
//        if (destinations.size()!=unexpanded.size()) {
//            logger.info("Expanded (composite) destinations from " + unexpanded.size() + " to " + destinations.size());
//        }

        final TramServiceDate queryDate = journeyRequest.getDate();
        final Set<Long> destinationNodeIds = Collections.singleton(endNode.getId());

        // can only be shared as same date and same set of destinations, will eliminate previously seen paths/results
        LowestCostsForDestRoutes lowestCostsForRoutes = routeToRouteCosts.getLowestCostCalcutatorFor(destinations);
        Duration maxJourneyDuration = getMaxDurationFor(txn, startNode, destinations, journeyRequest);

        final JourneyConstraints journeyConstraints = new JourneyConstraints(config, runningRoutesAndServices.getFor(queryDate.getDate()),
                journeyRequest, closedStationsRepository, destinations, lowestCostsForRoutes, maxJourneyDuration);

        logger.info("Journey Constraints: " + journeyConstraints);
        logger.info("Query times: " + queryTimes);

        final LowestCostSeen lowestCostSeen = new LowestCostSeen();

        final Instant begin = providesNow.getInstant(); // TODO REMOVE THIS?

        final Stream<Journey> results = numChangesRange(journeyRequest, numberOfChanges).
                flatMap(numChanges -> queryTimes.stream().
                        map(queryTime -> createPathRequest(startNode, queryDate, queryTime, numChanges, journeyConstraints))).
                flatMap(pathRequest -> findShortestPath(txn, destinationNodeIds, destinations,
                        createServiceReasons(journeyRequest, pathRequest), pathRequest, lowestCostsForRoutes, createPreviousVisits(),
                        lowestCostSeen, begin)).
                map(path -> createJourney(journeyRequest, path, destinations, lowestCostsForRoutes));

        //noinspection ResultOfMethodCallIgnored
        results.onClose(() -> {
            cacheMetrics.report();
            logger.info("Journey stream closed");
        });

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

