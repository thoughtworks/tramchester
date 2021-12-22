package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.WalkingStage;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.caches.PreviousVisits;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.repository.RouteInterchanges;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TripRepository;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
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
    private final RouteInterchanges routeInterchanges;
    private final RouteCostCalculator routeCostCalculator;

    protected RouteCalculatorSupport(GraphQuery graphQuery, PathToStages pathToStages, NodeContentsRepository nodeContentsRepository,
                                     GraphDatabase graphDatabaseService, TraversalStateFactory traversalStateFactory,
                                     ProvidesNow providesNow, SortsPositions sortsPosition, MapPathToLocations mapPathToLocations,
                                     StationRepository stationRepository, TramchesterConfig config, TripRepository tripRepository,
                                     BetweenRoutesCostRepository routeToRouteCosts, ReasonsToGraphViz reasonToGraphViz,
                                     RouteInterchanges routeInterchanges, RouteCostCalculator routeCostCalculator) {
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
        this.routeCostCalculator = routeCostCalculator;
    }

    protected Node getStationNodeSafe(Transaction txn, Station station) {
        Node stationNode = graphQuery.getStationOrGrouped(txn, station);
        if (stationNode == null) {
            String msg = "Unable to find station (or grouped) graph node based for " + station;
            logger.error(msg);
            throw new RuntimeException(msg);
        }
        return stationNode;
    }

    @NotNull
    public Set<Long> getDestinationNodeIds(Set<Station> destinations) {
        Set<Long> destinationNodeIds;
        try(Transaction txn = graphDatabaseService.beginTx()) {
            destinationNodeIds = destinations.stream().
                    map(station -> getStationNodeSafe(txn, station)).
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
            logger.error("Requested max changes is less than computed minimum changes needed");
        }

        if (computedMaxChanges > requestedMaxChanges) {
            logger.info(format("Will exclude some routes, requests changes %s is less then computed max changes %s",
                    requestedMaxChanges, computedMaxChanges));
        }

        int max = Math.min(computedMaxChanges, requestedMaxChanges);

        logger.info("Will check journey from " + computedMinChanges + " to " + max +" changes. Computed was " + computedChanges);
        return IntStream.rangeClosed(computedMinChanges, max).boxed();
    }

    @NotNull
    private ServiceHeuristics createHeuristics(TramTime actualQueryTime, JourneyConstraints journeyConstraints, int maxNumChanges) {
        return new ServiceHeuristics(stationRepository, routeInterchanges, nodeContentsRepository, journeyConstraints, actualQueryTime,
                maxNumChanges);
    }

    public Stream<RouteCalculator.TimedPath> findShortestPath(Transaction txn, Set<Long> destinationNodeIds,
                                                              final Set<Station> endStations,
                                                              ServiceReasons reasons, PathRequest pathRequest,
                                                              LowestCostsForRoutes lowestCostsForRoutes, PreviousVisits previousSuccessfulVisit,
                                                              LowestCostSeen lowestCostSeen, Instant begin) {

        TramNetworkTraverser tramNetworkTraverser = new TramNetworkTraverser(graphDatabaseService,
                pathRequest, sortsPosition, nodeContentsRepository,
                tripRepository, traversalStateFactory, endStations, config, destinationNodeIds,
                reasons, reasonToGraphViz, providesNow);

        logger.info("Traverse for " + pathRequest);

        return tramNetworkTraverser.
                findPaths(txn, pathRequest.startNode, previousSuccessfulVisit, lowestCostSeen, begin, lowestCostsForRoutes).
                map(path -> new RouteCalculator.TimedPath(path, pathRequest.queryTime, pathRequest.numChanges));
    }

    @NotNull
    protected Journey createJourney(JourneyRequest journeyRequest, RouteCalculator.TimedPath path,
                                    Set<Station> endStations, LowestCostsForRoutes lowestCostForRoutes) {

        final List<TransportStage<?, ?>> stages = pathToStages.mapDirect(path, journeyRequest, lowestCostForRoutes, endStations);
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

    protected int getMaxDurationFor(Transaction txn, Node startNode, Set<Station> destinations, JourneyRequest journeyRequest) {
        if (config.getTransportModes().contains(TransportMode.Tram) && config.getTransportModes().size()==1) {
            return journeyRequest.getMaxJourneyDuration();
        }

        int maxLeastCostForRoute = destinations.stream().
                mapToInt(dest -> routeCostCalculator.getMaxCostBetween(txn, startNode, dest, journeyRequest.getDate())).
                max().orElse(journeyRequest.getMaxJourneyDuration());

        int longest = maxLeastCostForRoute * 2; // 100% margin

        if (longest>journeyRequest.getMaxJourneyDuration()) {
            logger.warn(format("Computed longest %s is more than journeyRequest %s",
                    longest, journeyRequest.getMaxJourneyDuration()));
        } else {
            logger.info(format("Computed longest to be %s", longest));
        }

        return longest;

    }

    public PathRequest createPathRequest(Node startNode, TramServiceDate queryDate, TramTime actualQueryTime, int numChanges, JourneyConstraints journeyConstraints) {
        ServiceHeuristics serviceHeuristics = createHeuristics(actualQueryTime, journeyConstraints, numChanges);
        return new PathRequest(startNode, queryDate, actualQueryTime, numChanges, serviceHeuristics);
    }

    public static class PathRequest {
        private final Node startNode;
        private final TramTime queryTime;
        private final int numChanges;
        private final ServiceHeuristics serviceHeuristics;
        private final TramServiceDate queryDate;

        public PathRequest(Node startNode, TramServiceDate queryDate, TramTime queryTime, int numChanges, ServiceHeuristics serviceHeuristics) {
            this.startNode = startNode;
            this.queryDate = queryDate;
            this.queryTime = queryTime;
            this.numChanges = numChanges;
            this.serviceHeuristics = serviceHeuristics;
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
                    '}';
        }

        public TramServiceDate getQueryDate() {
            return queryDate;
        }
    }

    public static class InitialWalksFinished {

        private final long maxJourneys;
        private int seenMaxJourneys;

        private final Map<Station, AtomicLong> journeysPerStation;

        public InitialWalksFinished(JourneyRequest journeyRequest, Set<StationWalk> stationWalks) {
            this.maxJourneys = journeyRequest.getMaxNumberOfJourneys();
            journeysPerStation = new HashMap<>();

            seenMaxJourneys = 0;
            stationWalks.stream().map(StationWalk::getStation).forEach(station -> journeysPerStation.put(station, new AtomicLong()));

        }

        public boolean notDoneYet(Journey journey) {
            if (!journey.firstStageIsWalk()) {
                throw new RuntimeException("Expected walk to be first stage of " + journey);
            }

            WalkingStage<?, Station> walkingStage = (WalkingStage<?, Station>) journey.getStages().get(0);

            final Station lastStation = walkingStage.getLastStation();
            long countForStation = journeysPerStation.get(lastStation).incrementAndGet();
            if (countForStation==maxJourneys) {
                logger.info("Seen " + maxJourneys + " for " + lastStation.getId());
                seenMaxJourneys = seenMaxJourneys + 1;
            }
            return seenMaxJourneys < journeysPerStation.size();

        }
    }


}
