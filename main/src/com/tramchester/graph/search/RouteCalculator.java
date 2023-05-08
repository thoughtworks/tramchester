package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.CreateQueryTimes;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.search.diagnostics.ReasonsToGraphViz;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.RouteInterchangeRepository;
import com.tramchester.repository.RunningRoutesAndServices;
import com.tramchester.repository.TransportData;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
import java.util.Collections;
import java.util.EnumSet;
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
                           RouteInterchangeRepository routeInterchanges, CacheMetrics cacheMetrics) {
        super(graphQuery, pathToStages, nodeOperations, graphDatabaseService,
                traversalStateFactory, providesNow, sortsPosition, mapPathToLocations,
                transportData, config, transportData, routeToRouteCosts, reasonToGraphViz, routeInterchanges);
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

        TramDate date = journeyRequest.getDate();

        NumberOfChanges numberOfChanges =  routeToRouteCosts.getNumberOfChanges(start, destination,
                journeyRequest.getRequestedModes(), date, journeyRequest.getTimeRange());

        if (journeyRequest.getMaxChanges()>numberOfChanges.getMax()) {
            if (closedStationsRepository.hasClosuresOn(date)) {
                logger.warn(format("Closures on in effect today %s so over ride max changes", journeyRequest.getDate()));
                numberOfChanges.overrideMax(journeyRequest.getMaxChanges());
            }
            else {
                logger.error(format("Computed max changes (%s) is less than requested number of changes (%s)",
                        numberOfChanges.getMax(), journeyRequest.getMaxChanges()));
            }
        }

        Duration maxInitialWait = getMaxInitialWaitFor(start, config);
        return getJourneyStream(txn, startNode, endNode, journeyRequest, destinations, queryTimes, numberOfChanges, maxInitialWait).
                limit(journeyRequest.getMaxNumberOfJourneys());
    }

    public Stream<Journey> calculateRouteWalkAtEnd(Transaction txn, Location<?> start, Node endOfWalk, LocationSet destinations,
                                                   JourneyRequest journeyRequest, NumberOfChanges numberOfChanges)
    {
        Node startNode = getLocationNodeSafe(txn, start);
        final List<TramTime> queryTimes = createQueryTimes.generate(journeyRequest.getOriginalTime());

        Duration maxInitialWait = getMaxInitialWaitFor(start, config);

        return getJourneyStream(txn, startNode, endOfWalk, journeyRequest, destinations, queryTimes, numberOfChanges, maxInitialWait).
                limit(journeyRequest.getMaxNumberOfJourneys());
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStart(Transaction txn, Set<StationWalk> stationWalks, Node startOfWalkNode, Location<?> destination,
                                                     JourneyRequest journeyRequest, NumberOfChanges numberOfChanges) {

        final InitialWalksFinished finished = new InitialWalksFinished(journeyRequest, stationWalks);
        Node endNode = getLocationNodeSafe(txn, destination);
        LocationSet destinations = LocationSet.singleton(destination);
        final List<TramTime> queryTimes = createQueryTimes.generate(journeyRequest.getOriginalTime(), stationWalks);

        Duration maxInitialWait = getMaxInitialWaitFor(stationWalks, config);

        return getJourneyStream(txn, startOfWalkNode, endNode, journeyRequest, destinations, queryTimes, numberOfChanges, maxInitialWait).
                takeWhile(finished::notDoneYet);
    }

    public Stream<Journey> calculateRouteWalkAtStartAndEnd(Transaction txn, Set<StationWalk> stationWalks, Node startNode, Node endNode,
                                                           LocationSet destinationStations, JourneyRequest journeyRequest,
                                                           NumberOfChanges numberOfChanges) {

        final InitialWalksFinished finished = new InitialWalksFinished(journeyRequest, stationWalks);
        final List<TramTime> queryTimes = createQueryTimes.generate(journeyRequest.getOriginalTime(), stationWalks);

        Duration maxInitialWait = getMaxInitialWaitFor(stationWalks, config);
        return getJourneyStream(txn, startNode, endNode, journeyRequest, destinationStations, queryTimes, numberOfChanges, maxInitialWait).
                takeWhile(finished::notDoneYet);
    }

    private Stream<Journey> getJourneyStream(Transaction txn, Node startNode, Node endNode, JourneyRequest journeyRequest,
                                             LocationSet destinations, List<TramTime> queryTimes, NumberOfChanges numberOfChanges,
                                             Duration maxInitialWait) {

        if (numberOfChanges.getMin()==Integer.MAX_VALUE) {
            logger.error(format("Computed min number of changes is MAX_VALUE, journey %s is not possible?", journeyRequest));
            return Stream.empty();
        }

        EnumSet<TransportMode> requestedModes = journeyRequest.getRequestedModes();

        // TODO groups will need their own method, or get expanded much earlier on
        //final Set<Station> destinations = GroupedStations.expandStations(unexpanded);
//        if (destinations.size()!=unexpanded.size()) {
//            logger.info("Expanded (composite) destinations from " + unexpanded.size() + " to " + destinations.size());
//        }

        //final TramServiceDate queryServiceDate = journeyRequest.getDate();
        final Set<Long> destinationNodeIds = Collections.singleton(endNode.getId());
        final TramDate tramDate = journeyRequest.getDate();

        // can only be shared as same date and same set of destinations, will eliminate previously seen paths/results
        final LowestCostsForDestRoutes lowestCostsForRoutes = routeToRouteCosts.getLowestCostCalcutatorFor(destinations, tramDate,
                journeyRequest.getTimeRange(), requestedModes);
        final Duration maxJourneyDuration = getMaxDurationFor(journeyRequest);

        final IdSet<Station> closedStations = closedStationsRepository.getFullyClosedStationsFor(tramDate).stream().
                map(ClosedStation::getStationId).collect(IdSet.idCollector());

        final JourneyConstraints journeyConstraints = new JourneyConstraints(config, runningRoutesAndServices.getFor(tramDate),
                closedStations, destinations, lowestCostsForRoutes, maxJourneyDuration);

        logger.info("Journey Constraints: " + journeyConstraints);
        logger.info("Query times: " + queryTimes);

        final LowestCostSeen lowestCostSeen = new LowestCostSeen();

        final Stream<Journey> results = numChangesRange(journeyRequest, numberOfChanges).
                flatMap(numChanges -> queryTimes.stream().
                        map(queryTime -> createPathRequest(startNode, tramDate, queryTime, requestedModes, numChanges, journeyConstraints, maxInitialWait))).
                flatMap(pathRequest -> findShortestPath(txn, destinationNodeIds, destinations,
                        createServiceReasons(journeyRequest, pathRequest), pathRequest, lowestCostsForRoutes, createPreviousVisits(),
                        lowestCostSeen)).
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

