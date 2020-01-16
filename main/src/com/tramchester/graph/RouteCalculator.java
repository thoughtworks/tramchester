package com.tramchester.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.RawJourney;
import com.tramchester.domain.StationWalk;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.repository.ReachabilityRepository;
import com.tramchester.repository.RunningServices;
import com.tramchester.repository.TransportData;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public Stream<RawJourney> calculateRoute(String startStationId, String endStationId, List<TramTime> queryTimes,
                                          TramServiceDate queryDate) {
        logger.info(format("Finding shortest path for %s --> %s on %s at %s",
                startStationId, endStationId, queryDate, queryTimes));

        return gatherJounerys(getStationNode(startStationId), endStationId, queryTimes, queryDate);
    }

    public Stream<RawJourney> calculateRoute(LatLong origin, List<StationWalk> stationWalks, String destinationId,
                                             List<TramTime> queryTimes, TramServiceDate queryDate) {

        return getWalkingJourneyStream(origin, stationWalks, destinationId, queryTimes, queryDate);
    }

    private Stream<RawJourney> getWalkingJourneyStream(LatLong origin, List<StationWalk> stationWalks, String destinationId,
                                                       List<TramTime> queryTimes, TramServiceDate queryDate) {
        List<Relationship> addedWalks = new LinkedList<>();

        Node startOfWalkNode = nodeOperations.createQueryNode(graphDatabaseService);
        startOfWalkNode.setProperty(GraphStaticKeys.Station.LAT, origin.getLat());
        startOfWalkNode.setProperty(GraphStaticKeys.Station.LONG, origin.getLon());
        startOfWalkNode.setProperty(GraphStaticKeys.Station.NAME, queryNodeName);
        logger.info(format("Added start node at %s as node %s", origin, startOfWalkNode));

        stationWalks.forEach(stationWalk -> {
            String walkStationId = stationWalk.getStationId();
            Node node = getStationNode(walkStationId);
            int cost = stationWalk.getCost();
            logger.info(format("Add walking relationship from %s to %s cost %s", startOfWalkNode, walkStationId, cost));
            Relationship walkingRelationship = startOfWalkNode.createRelationshipTo(node, TransportRelationshipTypes.WALKS_TO);
            walkingRelationship.setProperty(GraphStaticKeys.COST, cost);
            walkingRelationship.setProperty(GraphStaticKeys.STATION_ID, walkStationId);
            addedWalks.add(walkingRelationship);
        });

        Stream<RawJourney> journeys = gatherJounerys(startOfWalkNode, destinationId, queryTimes, queryDate);

        // must delete relationships first, otherwise may not delete node
        journeys.onClose(() -> {
            logger.info("Removed added walks and start of walk node");
            addedWalks.forEach(Relationship::delete);
            nodeOperations.deleteNode(startOfWalkNode);
        });

        return journeys;
    }

    private Stream<RawJourney> gatherJounerys(Node startNode, String destinationId, List<TramTime> queryTimes,
                                              TramServiceDate queryDate) {
        RunningServices runningServicesIds = new RunningServices(transportData.getServicesOnDate(queryDate));
        Node endNode = getStationNode(destinationId);
        ServiceReasons serviceReasons = new ServiceReasons();

        return queryTimes.stream().
                map(queryTime -> new ServiceHeuristics(nodeOperations, reachabilityRepository, config,
                        queryTime, runningServicesIds, destinationId, serviceReasons)).
                map(serviceHeuristics -> findShortestPathEdgePerTrip(startNode, endNode, serviceHeuristics, serviceReasons, destinationId)).
                flatMap(Function.identity()).
                map(path -> new RawJourney(pathToStages.mapDirect(path.getPath()), path.getQueryTime(), path.path.weight()));

    }

    private Stream<TimedWeightedPath> findShortestPathEdgePerTrip(Node startNode, Node endNode,
                                                                  ServiceHeuristics serviceHeutistics,
                                                                  ServiceReasons reasons, String endStationId) {
        if (startNode.getProperty(GraphStaticKeys.Station.NAME).equals(queryNodeName)) {
            logger.info("Query node based search, setting start time to actual query time");
        }

        TramNetworkTraverser tramNetworkTraverser = new TramNetworkTraverser(serviceHeutistics, reasons, nodeOperations,
                endNode, endStationId, config.getChangeAtInterchangeOnly());

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

