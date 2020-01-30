package com.tramchester.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.repository.ReachabilityRepository;
import com.tramchester.repository.RunningServices;
import com.tramchester.repository.TransportData;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.String.format;

public class RouteCalculator extends StationIndexs {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculator.class);


    private final String queryNodeName = "BEGIN";
    private final MapPathToStages pathToStages;
    private final TramchesterConfig config;
    private final CachedNodeOperations nodeOperations;
    private final TransportData transportData;
    private final ReachabilityRepository reachabilityRepository;

    public RouteCalculator(GraphDatabaseService db, TransportData transportData,
                           CachedNodeOperations nodeOperations, MapPathToStages pathToStages,
                           TramchesterConfig config, ReachabilityRepository reachabilityRepository, GraphQuery graphQuery) {
        super(db, graphQuery, true);
        this.transportData = transportData;
        this.nodeOperations = nodeOperations;
        this.pathToStages = pathToStages;
        this.config = config;
        this.reachabilityRepository = reachabilityRepository;
    }

    public Stream<Journey> calculateRoute(String startStationId, String endStationId, List<TramTime> queryTimes,
                                          TramServiceDate queryDate) {
        logger.info(format("Finding shortest path for %s --> %s on %s at %s", startStationId, endStationId,
                queryDate, queryTimes));
        return gatherJounerys(getStationNode(startStationId), endStationId, queryTimes, queryDate);
    }

    public Stream<Journey> calculateRouteWalkAtEnd(String startId, LatLong destination, List<StationWalk> walksToDest,
                                                   TramTime queryTime, TramServiceDate queryDate)
    {
        List<Relationship> addedWalks = new LinkedList<>();
        List<String> desinationStationIds = new ArrayList<>();

        Node endOfWalk = createWalkingNode(destination);

        // todo extract method
        walksToDest.forEach(stationWalk -> {
            String walkStationId = stationWalk.getStationId();
            desinationStationIds.add(walkStationId);
            Node stationNode = getStationNode(walkStationId);
            int cost = stationWalk.getCost();
            logger.info(format("Add walking relationship from %s to %s cost %s", walkStationId, endOfWalk,  cost));
            Relationship walkingRelationship = stationNode.createRelationshipTo(endOfWalk, TransportRelationshipTypes.WALKS_TO);
            walkingRelationship.setProperty(GraphStaticKeys.COST, cost);
            walkingRelationship.setProperty(GraphStaticKeys.STATION_ID, walkStationId);
            addedWalks.add(walkingRelationship);
        });

        List<TramTime> queryTimes = Collections.singletonList(queryTime);
        Stream<Journey> journeys = gatherJounerysWalkAtEnd(startId, endOfWalk, desinationStationIds, queryTimes, queryDate);

        journeys.onClose(() -> {
            logger.info("Removed added walks and start of walk node");
            addedWalks.forEach(Relationship::delete);
            nodeOperations.deleteNode(endOfWalk);
        });
        return journeys;

    }


    public Stream<Journey> calculateRoute(LatLong origin, List<StationWalk> walksToStartStations, String destinationId,
                                          TramTime queryTime, TramServiceDate queryDate) {
        List<Relationship> addedWalks = new LinkedList<>();

        Node startOfWalkNode = createWalkingNode(origin);

        // todo extract method
        walksToStartStations.forEach(stationWalk -> {
            String walkStationId = stationWalk.getStationId();
            Node stationNode = getStationNode(walkStationId);
            int cost = stationWalk.getCost();
            logger.info(format("Add walking relationship from %s to %s cost %s", startOfWalkNode, walkStationId, cost));
            Relationship walkingRelationship = startOfWalkNode.createRelationshipTo(stationNode, TransportRelationshipTypes.WALKS_TO);
            walkingRelationship.setProperty(GraphStaticKeys.COST, cost);
            walkingRelationship.setProperty(GraphStaticKeys.STATION_ID, walkStationId);
            addedWalks.add(walkingRelationship);
        });

        List<TramTime> queryTimes = Collections.singletonList(queryTime);
        Stream<Journey> journeys = gatherJounerys(startOfWalkNode, destinationId, queryTimes, queryDate);

        // must delete relationships first, otherwise may not delete node
        journeys.onClose(() -> {
            logger.info("Removed added walks and start of walk node");
            addedWalks.forEach(Relationship::delete);
            nodeOperations.deleteNode(startOfWalkNode);
        });

        return journeys;
    }

    private Node createWalkingNode(LatLong origin) {
        Node startOfWalkNode = nodeOperations.createQueryNode(graphDatabaseService);
        startOfWalkNode.setProperty(GraphStaticKeys.Station.LAT, origin.getLat());
        startOfWalkNode.setProperty(GraphStaticKeys.Station.LONG, origin.getLon());
        startOfWalkNode.setProperty(GraphStaticKeys.Station.NAME, queryNodeName);
        logger.info(format("Added walking node at %s as node %s", origin, startOfWalkNode));
        return startOfWalkNode;
    }

    private Stream<Journey> gatherJounerys(Node startNode, String destinationId, List<TramTime> queryTimes,
                                           TramServiceDate queryDate) {
        RunningServices runningServicesIds = new RunningServices(transportData.getServicesOnDate(queryDate));
        Node endNode = getStationNode(destinationId);
        ServiceReasons serviceReasons = new ServiceReasons();

        List<String> destinationIds = Collections.singletonList(destinationId);
        return queryTimes.stream().
                map(queryTime -> new ServiceHeuristics(nodeOperations, reachabilityRepository, config,
                        queryTime, runningServicesIds, destinationIds, serviceReasons)).
                map(serviceHeuristics -> findShortestPath(startNode, endNode, serviceHeuristics, serviceReasons, destinationIds)).
                flatMap(Function.identity()).
                map(path -> {
                    List<TransportStage> stages = pathToStages.mapDirect(path.getPath(), path.getQueryTime());
                    return new Journey(stages, path.getQueryTime(), path.path.weight());
                });
    }

    private Stream<Journey> gatherJounerysWalkAtEnd(String startId, Node endNode, List<String> destinationIds, List<TramTime> queryTimes,
                                           TramServiceDate queryDate) {
        RunningServices runningServicesIds = new RunningServices(transportData.getServicesOnDate(queryDate));
        Node startNode = getStationNode(startId);
        ServiceReasons serviceReasons = new ServiceReasons();

        return queryTimes.stream().
                map(queryTime -> new ServiceHeuristics(nodeOperations, reachabilityRepository, config,
                        queryTime, runningServicesIds, destinationIds, serviceReasons)).
                map(serviceHeuristics -> findShortestPath(startNode, endNode, serviceHeuristics, serviceReasons, destinationIds)).
                flatMap(Function.identity()).
                map(path -> {
                    List<TransportStage> stages = pathToStages.mapDirect(path.getPath(), path.getQueryTime());
                    return new Journey(stages, path.getQueryTime(), path.path.weight());
                });
    }

    private Stream<TimedWeightedPath> findShortestPath(Node startNode, Node endNode,
                                                       ServiceHeuristics serviceHeutistics,
                                                       ServiceReasons reasons, List<String> endStationIds) {
        if (startNode.getProperty(GraphStaticKeys.Station.NAME).equals(queryNodeName)) {
            logger.info("Query node based search, setting start time to actual query time");
        }

        TramNetworkTraverser tramNetworkTraverser = new TramNetworkTraverser(serviceHeutistics, reasons, nodeOperations,
                endNode, endStationIds, config.getChangeAtInterchangeOnly());

        return tramNetworkTraverser.findPaths(startNode).map(path -> new TimedWeightedPath(path, serviceHeutistics.getQueryTime()));
    }

    private static class TimedWeightedPath {
        private final WeightedPath path;
        private final TramTime queryTime;

        public TimedWeightedPath(WeightedPath path, TramTime queryTime) {

            this.path = path;
            this.queryTime = queryTime;
        }

        public WeightedPath getPath() {
            return path;
        }

        public TramTime getQueryTime() {
            return queryTime;
        }
    }
}

