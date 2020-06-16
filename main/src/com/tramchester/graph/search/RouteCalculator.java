package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.CreateQueryTimes;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.NodeContentsRepository;
import com.tramchester.graph.NodeTypeRepository;
import com.tramchester.repository.RunningServices;
import com.tramchester.repository.TramReachabilityRepository;
import com.tramchester.repository.TransportData;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class RouteCalculator implements TramRouteCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculator.class);

    private static final int BUSES_MAX_PATH_LENGTH = 1000;
    private static final int TRAMS_MAX_PATH_LENGTH = 400;

    private final MapPathToStages pathToStages;
    private final TramchesterConfig config;
    private final NodeContentsRepository nodeOperations;
    private final NodeTypeRepository nodeTypeRepository;

    private final TransportData transportData;
    private final TramReachabilityRepository tramReachabilityRepository;
    private final CreateQueryTimes createQueryTimes;
    private final GraphDatabase graphDatabaseService;
    private final ProvidesLocalNow providesLocalNow;
    private final GraphQuery graphQuery;

    public RouteCalculator(TransportData transportData, NodeContentsRepository nodeOperations, MapPathToStages pathToStages,
                           TramchesterConfig config, TramReachabilityRepository tramReachabilityRepository,
                           CreateQueryTimes createQueryTimes, GraphDatabase graphDatabaseService,
                           ProvidesLocalNow providesLocalNow, GraphQuery graphQuery, NodeTypeRepository nodeTypeRepository) {
        this.transportData = transportData;
        this.nodeOperations = nodeOperations;
        this.pathToStages = pathToStages;
        this.config = config;
        this.tramReachabilityRepository = tramReachabilityRepository;
        this.createQueryTimes = createQueryTimes;
        this.graphDatabaseService = graphDatabaseService;
        this.providesLocalNow = providesLocalNow;
        this.graphQuery = graphQuery;
        this.nodeTypeRepository = nodeTypeRepository;
    }

    @Override
    public Stream<Journey> calculateRoute(Transaction txn, Station startStation, Station destination, JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s --> %s for %s", startStation, destination, journeyRequest));

        Node startNode = getStationNodeSafe(txn, startStation);
        Node endNode = getStationNodeSafe(txn, destination);

        List<Station> destinations = Collections.singletonList(destination);

        return getJourneyStream(txn, startNode, endNode, journeyRequest, destinations, false);
    }

    private Node getStationNodeSafe(Transaction txn, Station startStation) {
        Node stationNode = graphQuery.getStationNode(txn, startStation);
        if (stationNode==null) {
            throw new RuntimeException("Unable to find station node based on " + startStation.getId());
        }
        return stationNode;
    }

    public Stream<Journey> calculateRouteWalkAtEnd(Transaction txn, Station start, Node endOfWalk, List<Station> desinationStations,
                                                   JourneyRequest journeyRequest)
    {
        Node startNode = getStationNodeSafe(txn, start);
        return getJourneyStream(txn, startNode, endOfWalk, journeyRequest, desinationStations, false);
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStart(Transaction txn, Node startOfWalkNode, Station destination,
                                                     JourneyRequest journeyRequest) {
        Node endNode = getStationNodeSafe(txn, destination);
        List<Station> destinationIds = Collections.singletonList(destination);
        return getJourneyStream(txn, startOfWalkNode, endNode, journeyRequest, destinationIds, true);
    }

    public Stream<Journey> calculateRouteWalkAtStartAndEnd(Transaction txn, Node startNode, Node endNode,
                                                           List<Station> destinationStations, JourneyRequest journeyRequest) {
        return getJourneyStream(txn, startNode, endNode, journeyRequest, destinationStations, true);
    }

    private Stream<Journey> getJourneyStream(Transaction txn, Node startNode, Node endNode, JourneyRequest journeyRequest,
                                             List<Station> destinations, boolean walkAtStart) {

        RunningServices runningServicesIds = new RunningServices(transportData.getServicesOnDate(journeyRequest.getDate()));
        ServiceReasons serviceReasons = new ServiceReasons(providesLocalNow, journeyRequest);

        List<TramTime> queryTimes = createQueryTimes.generate(journeyRequest.getTime(), walkAtStart);

        int maxPathLength = config.getBus() ? BUSES_MAX_PATH_LENGTH : TRAMS_MAX_PATH_LENGTH;
        int maxChanges = journeyRequest.getMaxChanges();

        return queryTimes.stream().
                map(time -> new ServiceHeuristics(transportData, nodeOperations, tramReachabilityRepository, config,
                        time, runningServicesIds, destinations, serviceReasons, maxPathLength, maxChanges)).
                flatMap(serviceHeuristics -> findShortestPath(txn, startNode, endNode, nodeTypeRepository, serviceHeuristics,
                        serviceReasons, destinations)).
                map(path -> {
                    List<TransportStage> stages = pathToStages.mapDirect(path.getPath(), path.getQueryTime(), journeyRequest);
                    return new Journey(stages, path.getQueryTime());
                });
    }

    private Stream<TimedPath> findShortestPath(Transaction txn, Node startNode, Node endNode,
                                               NodeTypeRepository nodeTypeRepository, ServiceHeuristics serviceHeuristics,
                                               ServiceReasons reasons, List<Station> destinations) {

        List<String> endStationIds = destinations.stream().map(Station::getId).collect(Collectors.toList());

        TramNetworkTraverser tramNetworkTraverser = new TramNetworkTraverser(graphDatabaseService, serviceHeuristics,
                reasons, nodeOperations, endNode, endStationIds, config, nodeTypeRepository);

        return tramNetworkTraverser.findPaths(txn, startNode).map(path -> new TimedPath(path, serviceHeuristics.getQueryTime()));
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

