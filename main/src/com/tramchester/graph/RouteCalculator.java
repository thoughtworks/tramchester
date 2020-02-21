package com.tramchester.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.CreateQueryTimes;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.ReachabilityRepository;
import com.tramchester.repository.RunningServices;
import com.tramchester.repository.TransportData;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.String.format;

public class RouteCalculator implements TramRouteCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculator.class);

    private final MapPathToStages pathToStages;
    private final TramchesterConfig config;
    private final CachedNodeOperations nodeOperations;
    private final TransportData transportData;
    private final ReachabilityRepository reachabilityRepository;
    private final CreateQueryTimes createQueryTimes;
    private final StationIndexs stationIndexs;
    private final GraphDatabaseService graphDatabaseService;

    public RouteCalculator(TransportData transportData, CachedNodeOperations nodeOperations, MapPathToStages pathToStages,
                           TramchesterConfig config, ReachabilityRepository reachabilityRepository,
                           CreateQueryTimes createQueryTimes, StationIndexs stationIndexs, GraphDatabaseService graphDatabaseService) {
        this.transportData = transportData;
        this.nodeOperations = nodeOperations;
        this.pathToStages = pathToStages;
        this.config = config;
        this.reachabilityRepository = reachabilityRepository;
        this.createQueryTimes = createQueryTimes;
        this.stationIndexs = stationIndexs;
        this.graphDatabaseService = graphDatabaseService;
    }

    @Override
    public Stream<Journey> calculateRoute(String startStationId, String destinationId, TramTime queryTime,
                                          TramServiceDate queryDate) {
        logger.info(format("Finding shortest path for %s --> %s on %s at %s", startStationId, destinationId,
                queryDate, queryTime));

        Node startNode = stationIndexs.getStationNode(startStationId);
        Node endNode = stationIndexs.getStationNode(destinationId);
        List<String> destinationIds = Collections.singletonList(destinationId);

        return getJourneyStream(startNode, endNode, queryTime, destinationIds, queryDate, false);
    }

    public Stream<Journey> calculateRouteWalkAtEnd(String startId, Node endOfWalk, List<String> desinationStationIds,
                                                   TramTime queryTime, TramServiceDate queryDate)
    {
        Node startNode = stationIndexs.getStationNode(startId);
        return getJourneyStream(startNode, endOfWalk, queryTime, desinationStationIds, queryDate, false);
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStart(Node startOfWalkNode, String destinationId,
                                                     TramTime queryTime, TramServiceDate queryDate) {
        Node endNode = stationIndexs.getStationNode(destinationId);
        List<String> destinationIds = Collections.singletonList(destinationId);
        return getJourneyStream(startOfWalkNode, endNode, queryTime, destinationIds, queryDate, true);
    }

    private Stream<Journey> getJourneyStream(Node startNode, Node endNode, TramTime queryTime,
                                             List<String> destinationIds, TramServiceDate queryDate, boolean walkAtStart) {
        RunningServices runningServicesIds = new RunningServices(transportData.getServicesOnDate(queryDate));
        ServiceReasons serviceReasons = new ServiceReasons();

        List<TramTime> queryTimes = createQueryTimes.generate(queryTime, walkAtStart);

        return queryTimes.stream().
                map(time -> new ServiceHeuristics(nodeOperations, reachabilityRepository, config,
                        time, runningServicesIds, destinationIds, serviceReasons)).
                map(serviceHeuristics -> findShortestPath(startNode, endNode, serviceHeuristics, serviceReasons, destinationIds)).
                flatMap(Function.identity()).
                map(path -> {
                    List<TransportStage> stages = pathToStages.mapDirect(path.getPath(), path.getQueryTime());
                    return new Journey(stages, path.getQueryTime());
                });
    }

    private Stream<TimedPath> findShortestPath(Node startNode, Node endNode,
                                               ServiceHeuristics serviceHeuristics,
                                               ServiceReasons reasons, List<String> endStationIds) {

        TramNetworkTraverser tramNetworkTraverser = new TramNetworkTraverser(graphDatabaseService, serviceHeuristics, reasons, nodeOperations,
                endNode, endStationIds, config.getChangeAtInterchangeOnly());

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

