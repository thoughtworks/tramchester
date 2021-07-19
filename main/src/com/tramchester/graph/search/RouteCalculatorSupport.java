package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.caches.PreviousVisits;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TripRepository;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO;
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

    protected RouteCalculatorSupport(GraphQuery graphQuery, PathToStages pathToStages, NodeContentsRepository nodeContentsRepository,
                                     GraphDatabase graphDatabaseService, TraversalStateFactory traversalStateFactory,
                                     ProvidesNow providesNow, SortsPositions sortsPosition, MapPathToLocations mapPathToLocations,
                                     StationRepository stationRepository, TramchesterConfig config, TripRepository tripRepository,
                                     BetweenRoutesCostRepository routeToRouteCosts, ReasonsToGraphViz reasonToGraphViz) {
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
    protected Stream<Integer> numChangesRange(JourneyRequest journeyRequest, NumberOfChanges numberOfChanges) {
        final int requestedMaxChanges = journeyRequest.getMaxChanges();
        final int computedMaxChanges = numberOfChanges.getMax();
        final int computedMinChanges = numberOfChanges.getMin();

        if (requestedMaxChanges < computedMinChanges) {
            logger.error("Requested max changes is less than computed minimum changes needed");
        }

        if (computedMaxChanges > requestedMaxChanges) {
            logger.error(format("Computed max changes (%s) is greater then max in journey request (%s)",
                    computedMaxChanges, requestedMaxChanges));
        }

        int max = Math.min(computedMaxChanges, requestedMaxChanges);

        logger.info("Will check journey from " + computedMinChanges + " to " + max +" changes. Computed was " + numberOfChanges);
        return IntStream.rangeClosed(computedMinChanges, max).boxed();
    }

    @NotNull
    private ServiceHeuristics createHeuristics(TramTime actualQueryTime, JourneyConstraints journeyConstraints, int maxNumChanges) {
        return new ServiceHeuristics(stationRepository, nodeContentsRepository, journeyConstraints, actualQueryTime,
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
    protected Journey createJourney(Transaction txn, JourneyRequest journeyRequest, RouteCalculator.TimedPath path,
                                    Set<Station> endStations, LowestCostsForRoutes lowestCostForRoutes) {

        final List<TransportStage<?, ?>> stages = pathToStages.mapDirect(txn, path, journeyRequest, lowestCostForRoutes, endStations);
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

    public PathRequest createPathRequest(Node startNode, TramTime actualQueryTime, int numChanges, JourneyConstraints journeyConstraints) {
        ServiceHeuristics serviceHeuristics = createHeuristics(actualQueryTime, journeyConstraints, numChanges);
        return new PathRequest(startNode, actualQueryTime, numChanges, serviceHeuristics);
    }

    public static class PathRequest {
        private final Node startNode;
        private final TramTime queryTime;
        private final int numChanges;
        private final ServiceHeuristics serviceHeuristics;

        public PathRequest(Node startNode, TramTime queryTime, int numChanges, ServiceHeuristics serviceHeuristics) {
            this.startNode = startNode;
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
                    '}';
        }

    }

    public static class Finished {

        private final AtomicInteger count;
        private final long maxJourneys;
        private Set<Long> needToSee;

        public Finished(JourneyRequest journeyRequest) {
            this.maxJourneys = journeyRequest.getMaxNumberOfJourneys();
            count = new AtomicInteger(0);
            needToSee = Collections.emptySet();
        }

        public boolean notDoneYet(RouteCalculator.TimedPath path) {
            if (!needToSee.isEmpty()) {
                Relationship firstRelationship = path.getPath().relationships().iterator().next();
                needToSee.remove(firstRelationship.getId());
                logger.info(needToSee.size() +" walks still to be seen");
                return true;
            }

            count.incrementAndGet();
            return count.get() <= maxJourneys || !needToSee.isEmpty();
        }

        public void needToSeeAllWalksFrom(Node startNode) {
            Iterable<Relationship> walks = startNode.getRelationships(Direction.OUTGOING, WALKS_TO);
            needToSee = new HashSet<>();
            for (Relationship relationship : walks) {
                needToSee.add(relationship.getId());
            }
            logger.info("Added " +needToSee.size() + " walks");
        }
    }


}
