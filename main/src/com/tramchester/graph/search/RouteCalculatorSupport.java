package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.caches.PreviousVisits;
import com.tramchester.graph.search.diagnostics.ReasonsToGraphViz;
import com.tramchester.graph.search.diagnostics.ServiceReasons;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.repository.RouteInterchangeRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TripRepository;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.String.format;

public class RouteCalculatorSupport {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculatorSupport.class);

    private final GraphQuery graphQuery;
    private final PathToStages pathToStages;
    private final GraphDatabase graphDatabaseService;
    protected final ProvidesNow providesNow;
    private final SortsPositions sortsPosition;
    private final MapPathToLocations mapPathToLocations;
    private final StationRepository stationRepository;
    private final TramchesterConfig config;
    private final TripRepository tripRepository;
    private final TraversalStateFactory traversalStateFactory;
    protected final BetweenRoutesCostRepository routeToRouteCosts;
    private final NodeContentsRepository nodeContentsRepository;
    private final ReasonsToGraphViz reasonToGraphViz;
    private final RouteInterchangeRepository routeInterchanges;

    protected RouteCalculatorSupport(GraphQuery graphQuery, PathToStages pathToStages, NodeContentsRepository nodeContentsRepository,
                                     GraphDatabase graphDatabaseService, TraversalStateFactory traversalStateFactory,
                                     ProvidesNow providesNow, SortsPositions sortsPosition, MapPathToLocations mapPathToLocations,
                                     StationRepository stationRepository, TramchesterConfig config, TripRepository tripRepository,
                                     BetweenRoutesCostRepository routeToRouteCosts, ReasonsToGraphViz reasonToGraphViz,
                                     RouteInterchangeRepository routeInterchanges) {
        this.graphQuery = graphQuery;
        this.pathToStages = pathToStages;
        this.nodeContentsRepository = nodeContentsRepository;
        this.graphDatabaseService = graphDatabaseService;
        this.traversalStateFactory = traversalStateFactory;
        this.providesNow = providesNow;
        this.sortsPosition = sortsPosition;
        this.mapPathToLocations = mapPathToLocations;
        this.stationRepository = stationRepository;
        this.config = config;
        this.tripRepository = tripRepository;
        this.routeToRouteCosts = routeToRouteCosts;
        this.reasonToGraphViz = reasonToGraphViz;
        this.routeInterchanges = routeInterchanges;
    }


    protected Node getLocationNodeSafe(Transaction txn, Location<?> location) {
        Node stationNode = graphQuery.getLocationNode(txn, location);
        if (stationNode == null) {
            String msg = "Unable to find node for " + location;
            logger.error(msg);
            throw new RuntimeException(msg);
        }
        return stationNode;
    }

    @NotNull
    public Set<Long> getDestinationNodeIds(LocationSet destinations) {
        Set<Long> destinationNodeIds;
        try(Transaction txn = graphDatabaseService.beginTx()) {
            destinationNodeIds = destinations.stream().
                    map(location -> getLocationNodeSafe(txn, location)).
                    map(Entity::getId).
                    collect(Collectors.toSet());
        }
        return destinationNodeIds;
    }

    @NotNull
    protected Stream<Integer> numChangesRange(JourneyRequest journeyRequest, NumberOfChanges computedChanges) {
        final int requestedMaxChanges = journeyRequest.getMaxChanges();

        final int computedMaxChanges = computedChanges.getMax();
        final int computedMinChanges = computedChanges.getMin();

        if (requestedMaxChanges < computedMinChanges) {
            logger.error(format("Requested max changes (%s) is less than computed minimum changes (%s) needed",
                    requestedMaxChanges, computedMaxChanges));
        }

        if (computedMaxChanges > requestedMaxChanges) {
            logger.info(format("Will exclude some routes, requests changes %s is less then computed max changes %s",
                    requestedMaxChanges, computedMaxChanges));
        }

        int max = Math.min(computedMaxChanges, requestedMaxChanges);
        int min = Math.min(computedMinChanges, requestedMaxChanges);

        logger.info("Will check journey from " + min + " to " + max +" changes. Computed was " + computedChanges);
        return IntStream.rangeClosed(min, max).boxed();
    }

    @NotNull
    private ServiceHeuristics createHeuristics(TramTime actualQueryTime, JourneyConstraints journeyConstraints, int maxNumChanges) {
        return new ServiceHeuristics(stationRepository, routeInterchanges, nodeContentsRepository, journeyConstraints, actualQueryTime,
                maxNumChanges);
    }

    public Stream<RouteCalculator.TimedPath> findShortestPath(Transaction txn, Set<Long> destinationNodeIds,
                                                              final LocationSet endStations,
                                                              ServiceReasons reasons, PathRequest pathRequest,
                                                              LowestCostsForDestRoutes lowestCostsForRoutes,
                                                              PreviousVisits previousSuccessfulVisit,
                                                              LowestCostSeen lowestCostSeen) {

        TramNetworkTraverser tramNetworkTraverser = new TramNetworkTraverser(
                pathRequest, sortsPosition, nodeContentsRepository,
                tripRepository, traversalStateFactory, endStations, config, destinationNodeIds,
                reasons, reasonToGraphViz, providesNow);

        logger.info("Traverse for " + pathRequest);

        return tramNetworkTraverser.
                findPaths(txn, pathRequest.startNode, previousSuccessfulVisit, lowestCostSeen, lowestCostsForRoutes).
                map(path -> new RouteCalculator.TimedPath(path, pathRequest.queryTime, pathRequest.numChanges));
    }

    @NotNull
    protected Journey createJourney(JourneyRequest journeyRequest, RouteCalculator.TimedPath path,
                                    LocationSet destinations, LowestCostsForDestRoutes lowestCostForRoutes) {

        final List<TransportStage<?, ?>> stages = pathToStages.mapDirect(path, journeyRequest, lowestCostForRoutes, destinations);
        final List<Location<?>> locationList = mapPathToLocations.mapToLocations(path.getPath());

        if (stages.isEmpty()) {
            logger.error("No stages were mapped for " + journeyRequest + " for " + locationList);
        }
        TramTime arrivalTime = getArrivalTimeFor(stages, journeyRequest);
        TramTime departTime = getDepartTimeFor(stages, journeyRequest);
        return new Journey(departTime, path.getQueryTime(), arrivalTime, stages, locationList, path.getNumChanges());
    }

    private TramTime getDepartTimeFor(List<TransportStage<?, ?>> stages, JourneyRequest journeyRequest) {
        if (stages.isEmpty()) {
            logger.warn("No stages were mapped, can't get depart time");
            return journeyRequest.getOriginalTime();
        } else {
            TransportStage<?, ?> firstStage = stages.get(0);
            return firstStage.getFirstDepartureTime();
        }
    }

    private TramTime getArrivalTimeFor(List<TransportStage<?, ?>> stages, JourneyRequest journeyRequest) {
        int size = stages.size();
        if (size == 0) {
            logger.warn("No stages were mapped, can't get arrival time");
            return journeyRequest.getOriginalTime();
        } else {
            TransportStage<?, ?> lastStage = stages.get(size - 1);
            return lastStage.getExpectedArrivalTime();
        }
    }

    protected PreviousVisits createPreviousVisits() {
        return new PreviousVisits();
    }

    @NotNull
    protected ServiceReasons createServiceReasons(JourneyRequest journeyRequest, TramTime time) {
        return new ServiceReasons(journeyRequest, time, providesNow);
    }

    @NotNull
    protected ServiceReasons createServiceReasons(JourneyRequest journeyRequest, PathRequest pathRequest) {
        return new ServiceReasons(journeyRequest, pathRequest.queryTime, providesNow);
    }

    protected Duration getMaxDurationFor(JourneyRequest journeyRequest) {
        return journeyRequest.getMaxJourneyDuration();

    }

//    private Duration getMaxCostBetween(Transaction txn, Node startNode, JourneyRequest journeyRequest, Location<?> dest, Set<TransportMode> modes) {
//        try {
//            return routeCostCalculator.getMaxCostBetween(txn, startNode, dest, journeyRequest.getDate(), modes);
//        } catch (InvalidDurationException invalidDurationException) {
//            return Duration.ofSeconds(-1);
//        }
//    }

    public PathRequest createPathRequest(Node startNode, TramDate queryDate, TramTime actualQueryTime, Set<TransportMode>
            requestedModes, int numChanges, JourneyConstraints journeyConstraints, Duration maxInitialWait) {
        ServiceHeuristics serviceHeuristics = createHeuristics(actualQueryTime, journeyConstraints, numChanges);
        return new PathRequest(startNode, queryDate, actualQueryTime, numChanges, serviceHeuristics, requestedModes, maxInitialWait);
    }

    public static class PathRequest {
        private final Node startNode;
        private final TramTime queryTime;
        private final int numChanges;
        private final ServiceHeuristics serviceHeuristics;
        private final TramDate queryDate;
        private final Set<TransportMode> requestedModes;
        private final Duration maxInitialWait;

        public PathRequest(Node startNode, TramDate queryDate, TramTime queryTime, int numChanges,
                           ServiceHeuristics serviceHeuristics, Set<TransportMode> requestedModes, Duration maxInitialWait) {
            this.startNode = startNode;
            this.queryDate = queryDate;
            this.queryTime = queryTime;
            this.numChanges = numChanges;
            this.serviceHeuristics = serviceHeuristics;
            this.requestedModes = requestedModes;
            this.maxInitialWait = maxInitialWait;
        }

        public ServiceHeuristics getServiceHeuristics() {
            return serviceHeuristics;
        }

        public TramTime getActualQueryTime() {
            return queryTime;
        }

        public int getNumChanges() {
            return numChanges;
        }

        @Override
        public String toString() {
            return "PathRequest{" +
                    "startNode=" + startNode +
                    ", queryTime=" + queryTime +
                    ", numChanges=" + numChanges +
                    ", serviceHeuristics=" + serviceHeuristics +
                    ", queryDate=" + queryDate +
                    ", requestedModes=" + requestedModes +
                    ", maxInitialWait=" + maxInitialWait +
                    '}';
        }

        public TramDate getQueryDate() {
            return queryDate;
        }

        public Set<TransportMode> getRequestedModes() {
            return requestedModes;
        }

        public Duration getMaxInitialWait() {
            return maxInitialWait;
        }
    }

    public static Duration getMaxInitialWaitFor(Location<?> location, TramchesterConfig config) {
        DataSourceID dataSourceID = location.getDataSourceID();
        return config.getInitialMaxWaitFor(dataSourceID);
    }

    public static Duration getMaxInitialWaitFor(Set<StationWalk> stationWalks, TramchesterConfig config) {
        Optional<Duration> longestWait = stationWalks.stream().
                map(StationWalk::getStation).
                map(station -> getMaxInitialWaitFor(station, config)).
                max(Duration::compareTo);
        if (longestWait.isEmpty()) {
            throw new RuntimeException("Could not compute inital max wait for " + stationWalks);
        }
        return longestWait.get();
    }

    public static class InitialWalksFinished {

        private final long maxJourneys;
        private int seenMaxJourneys;

        private final Map<Location<?>, AtomicLong> journeysPerStation;

        public InitialWalksFinished(JourneyRequest journeyRequest, Set<StationWalk> stationWalks) {
            this.maxJourneys = journeyRequest.getMaxNumberOfJourneys();
            journeysPerStation = new HashMap<>();

            seenMaxJourneys = 0;
            stationWalks.stream().map(StationWalk::getStation).forEach(station -> journeysPerStation.put(station, new AtomicLong()));

        }

        public boolean notDoneYet(Journey journey) {
            if (!(journey.firstStageIsWalk() || journey.firstStageIsConnect())) {
                throw new RuntimeException("Expected walk to be first stage of " + journey);
            }

            TransportStage<?, ?> walkingStage = journey.getStages().get(0);

            final Location<?> lastStation = walkingStage.getLastStation();
            long countForStation = journeysPerStation.get(lastStation).incrementAndGet();
            if (countForStation==maxJourneys) {
                logger.info("Seen " + maxJourneys + " for " + lastStation.getId());
                seenMaxJourneys = seenMaxJourneys + 1;
            }
            return seenMaxJourneys < journeysPerStation.size();

        }
    }


}
