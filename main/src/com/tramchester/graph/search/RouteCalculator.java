package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.CreateQueryTimes;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.*;
import com.tramchester.repository.RunningServices;
import com.tramchester.repository.TramReachabilityRepository;
import com.tramchester.repository.TransportData;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class RouteCalculator implements TramRouteCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculator.class);

    private static int BUSES_MAX_PATH_LENGTH = 1000;
    private static int TRAMS_MAX_PATH_LENGTH = 400;

    private final MapPathToStages pathToStages;
    private final TramchesterConfig config;
    private final CachedNodeOperations nodeOperations;
    private final TransportData transportData;
    private final TramReachabilityRepository tramReachabilityRepository;
    private final CreateQueryTimes createQueryTimes;
    private final NodeIdQuery nodeIdQuery;
    private final GraphDatabase graphDatabaseService;
    private final ProvidesLocalNow providesLocalNow;

    public RouteCalculator(TransportData transportData, CachedNodeOperations nodeOperations, MapPathToStages pathToStages,
                           TramchesterConfig config, TramReachabilityRepository tramReachabilityRepository,
                           CreateQueryTimes createQueryTimes, NodeIdQuery nodeIdQuery, GraphDatabase graphDatabaseService,
                           ProvidesLocalNow providesLocalNow) {
        this.transportData = transportData;
        this.nodeOperations = nodeOperations;
        this.pathToStages = pathToStages;
        this.config = config;
        this.tramReachabilityRepository = tramReachabilityRepository;
        this.createQueryTimes = createQueryTimes;
        this.nodeIdQuery = nodeIdQuery;
        this.graphDatabaseService = graphDatabaseService;
        this.providesLocalNow = providesLocalNow;
    }

    @Override
    public Stream<Journey> calculateRoute(String startStationId, Station destination, JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s --> %s on %s", startStationId, destination,
                journeyRequest));

        Node startNode = getStationNodeSafe(startStationId);
        Node endNode = getStationNodeSafe(destination.getId());

        List<Station> destinations = Collections.singletonList(destination);

        return getJourneyStream(startNode, endNode, journeyRequest, destinations, false);
    }

    private Node getStationNodeSafe(String startStationId) {
        Node stationNode = nodeIdQuery.getStationNode(startStationId);
        if (stationNode==null) {
            throw new RuntimeException("Unable to find station node based on " + startStationId);
        }
        return stationNode;
    }

    public Stream<Journey> calculateRouteWalkAtEnd(String startId, Node endOfWalk, List<Station> desinationStations,
                                                   JourneyRequest journeyRequest)
    {
        Node startNode = getStationNodeSafe(startId);
        return getJourneyStream(startNode, endOfWalk, journeyRequest, desinationStations, false);
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStart(Node startOfWalkNode, Station destination,
                                                     JourneyRequest journeyRequest) {
        Node endNode = getStationNodeSafe(destination.getId());
        List<Station> destinationIds = Collections.singletonList(destination);
        return getJourneyStream(startOfWalkNode, endNode, journeyRequest, destinationIds, true);
    }

    private Stream<Journey> getJourneyStream(Node startNode, Node endNode, JourneyRequest journeyRequest,
                                             List<Station> destinations, boolean walkAtStart) {
        RunningServices runningServicesIds = new RunningServices(transportData.getServicesOnDate(journeyRequest.getDate()));
        ServiceReasons serviceReasons = new ServiceReasons(providesLocalNow);

        List<TramTime> queryTimes = createQueryTimes.generate(journeyRequest.getTime(), walkAtStart);

        int maxPathLength = config.getBus() ? BUSES_MAX_PATH_LENGTH : TRAMS_MAX_PATH_LENGTH;

        return queryTimes.stream().
                map(time -> new ServiceHeuristics(transportData, nodeOperations, tramReachabilityRepository, config,
                        time, runningServicesIds, destinations, serviceReasons, maxPathLength)).
                map(serviceHeuristics -> findShortestPath(startNode, endNode, serviceHeuristics, serviceReasons, destinations)).
                flatMap(Function.identity()).
                map(path -> {
                    List<TransportStage> stages = pathToStages.mapDirect(path.getPath(), path.getQueryTime());
                    return new Journey(stages, path.getQueryTime());
                });
    }

    private Stream<TimedPath> findShortestPath(Node startNode, Node endNode,
                                               ServiceHeuristics serviceHeuristics,
                                               ServiceReasons reasons, List<Station> destinations) {

        List<String> endStationIds = destinations.stream().map(Station::getId).collect(Collectors.toList());

        TramNetworkTraverser tramNetworkTraverser = new TramNetworkTraverser(graphDatabaseService, serviceHeuristics,
                reasons, nodeOperations, endNode, endStationIds, config);

        return tramNetworkTraverser.findPaths(startNode).map(path -> new TimedPath(path, serviceHeuristics.getQueryTime()));
    }

    private static class TimedPath {
        private final Path path;
        private final TramTime queryTime;

        public TimedPath(Path path, TramTime queryTime) {
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

