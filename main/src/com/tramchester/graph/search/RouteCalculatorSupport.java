package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.*;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.caches.NodeTypeRepository;
import com.tramchester.graph.caches.PreviousSuccessfulVisits;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.ReachabilityRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TripRepository;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class RouteCalculatorSupport {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculatorSupport.class);

    private final GraphQuery graphQuery;
    private final MapPathToStages pathToStages;
    private final NodeContentsRepository nodeOperations;
    private final NodeTypeRepository nodeTypeRepository;
    private final ReachabilityRepository reachabilityRepository;
    private final GraphDatabase graphDatabaseService;
    private final ProvidesLocalNow providesLocalNow;
    private final SortsPositions sortsPosition;
    private final MapPathToLocations mapPathToLocations;
    private final CompositeStationRepository compositeStationRepository;
    private final StationRepository stationRepository;
    private final TramchesterConfig config;
    private final TripRepository tripRepository;

    protected RouteCalculatorSupport(GraphQuery graphQuery, MapPathToStages pathToStages, NodeContentsRepository nodeOperations,
                                     NodeTypeRepository nodeTypeRepository, ReachabilityRepository reachabilityRepository,
                                     GraphDatabase graphDatabaseService, ProvidesLocalNow providesLocalNow, SortsPositions sortsPosition,
                                     MapPathToLocations mapPathToLocations, CompositeStationRepository compositeStationRepository,
                                     StationRepository stationRepository, TramchesterConfig config, TripRepository tripRepository) {
        this.graphQuery = graphQuery;
        this.pathToStages = pathToStages;
        this.nodeOperations = nodeOperations;
        this.nodeTypeRepository = nodeTypeRepository;
        this.reachabilityRepository = reachabilityRepository;
        this.graphDatabaseService = graphDatabaseService;
        this.providesLocalNow = providesLocalNow;
        this.sortsPosition = sortsPosition;
        this.mapPathToLocations = mapPathToLocations;
        this.compositeStationRepository = compositeStationRepository;
        this.stationRepository = stationRepository;
        this.config = config;
        this.tripRepository = tripRepository;
    }

    protected Node getStationNodeSafe(Transaction txn, Station station) {
        Node stationNode = graphQuery.getStationOrGrouped(txn, station);
        if (stationNode == null) {
            String msg = "Unable to find station (or grouped) graph node based for " + station.getId();
            logger.error(msg);
            throw new RuntimeException(msg);
        }
        return stationNode;
    }

    @NotNull
    protected Stream<Integer> numChangesRange(JourneyRequest journeyRequest) {
        logger.info("Check journey from 0 to " + journeyRequest.getMaxChanges() +" changes");
        return IntStream.rangeClosed(0, journeyRequest.getMaxChanges()).boxed();
    }

    @NotNull
    private ServiceHeuristics createHeuristics(TramTime time, JourneyConstraints journeyConstraints, int maxNumChanges) {
        return new ServiceHeuristics(stationRepository, nodeOperations, reachabilityRepository,
                journeyConstraints, time, maxNumChanges);
    }


    protected Stream<RouteCalculator.TimedPath> findShortestPath(Transaction txn, Set<Long> destinationNodeIds,
                                                               final Set<Station> endStations, PreviousSuccessfulVisits previousSuccessfulVisit,
                                                               ServiceReasons reasons, PathRequest pathRequest) {

        TramNetworkTraverser tramNetworkTraverser = new TramNetworkTraverser(graphDatabaseService,
                pathRequest.serviceHeuristics, compositeStationRepository, sortsPosition, nodeOperations,
                tripRepository, endStations, config, nodeTypeRepository, destinationNodeIds, reasons);

        return tramNetworkTraverser.
                findPaths(txn, pathRequest.startNode, previousSuccessfulVisit).
                map(path -> new RouteCalculator.TimedPath(path, pathRequest.queryTime, pathRequest.numChanges));
    }

    @NotNull
    protected Journey createJourney(JourneyRequest journeyRequest, RouteCalculator.TimedPath path) {
        return new Journey(pathToStages.mapDirect(path, journeyRequest),
                path.getQueryTime(), mapPathToLocations.mapToLocations(path.getPath()));
    }

    @NotNull
    protected ServiceReasons createServiceReasons(JourneyRequest journeyRequest, TramTime time, PathRequest pathRequest) {
        return new ServiceReasons(journeyRequest, time, providesLocalNow, pathRequest.numChanges);
    }

    @NotNull
    protected ServiceReasons createServiceReasons(JourneyRequest journeyRequest, PathRequest pathRequest) {
        return new ServiceReasons(journeyRequest, pathRequest.queryTime, providesLocalNow, pathRequest.numChanges);
    }

    protected class PathRequest {
        private final Node startNode;
        private final TramTime queryTime;
        private final int numChanges;
        private final ServiceHeuristics serviceHeuristics;

        public PathRequest(Node startNode, TramTime queryTime, int numChanges, JourneyConstraints journeyConstraints) {
            this.startNode = startNode;
            this.queryTime = queryTime;
            this.numChanges = numChanges;
            this.serviceHeuristics = createHeuristics(queryTime, journeyConstraints, numChanges);
        }
    }


}
