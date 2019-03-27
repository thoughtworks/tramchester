package com.tramchester.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.DTO.AreaDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TransportRelationship;
import com.tramchester.repository.TransportData;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphalgo.impl.path.Dijkstra;
import org.neo4j.graphalgo.impl.util.PathInterestFactory;
import org.neo4j.graphdb.*;
import org.neo4j.kernel.impl.util.NoneStrictMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.String.format;

public class RouteCalculator extends StationIndexs {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculator.class);

    private static final int MAX_NUM_GRAPH_PATHS = 2; // todo into config

    private final String queryNodeName = "BEGIN";

    private final NodeFactory nodeFactory;
    private final MapPathToStages pathToStages;
    private final CostEvaluator<Double> costEvaluator;
    private final TramchesterConfig config;
    private final NodeOperations nodeOperations;
    private final TransportData transportData;

    public RouteCalculator(GraphDatabaseService db, TransportData transportData, NodeFactory nodeFactory, RelationshipFactory relationshipFactory,
                           SpatialDatabaseService spatialDatabaseService, NodeOperations nodeOperations, MapPathToStages pathToStages,
                           CostEvaluator<Double> costEvaluator, TramchesterConfig config) {
        super(db, relationshipFactory, spatialDatabaseService, true);
        this.transportData = transportData;
        this.nodeFactory = nodeFactory;
        this.nodeOperations = nodeOperations;
        this.pathToStages = pathToStages;
        this.costEvaluator = costEvaluator;
        this.config = config;
    }

    public Set<RawJourney>  calculateRoute(String startStationId, String endStationId, List<LocalTime> queryTimes,
                                          TramServiceDate queryDate) {
        Set<RawJourney> journeys = new LinkedHashSet<>(); // order matters
        Node endNode = getStationNode(endStationId);

        Station endStation = transportData.getStation(endStationId).get();

        try (Transaction tx = graphDatabaseService.beginTx()) {
            Node startNode = getStationNode(startStationId);

            gatherJounerys(startNode, endNode, queryTimes, queryDate, MAX_NUM_GRAPH_PATHS, journeys,
                    endStation.getRoutes());

            tx.success();
        }
        return journeys;
    }

    public Set<RawJourney> calculateRoute(AreaDTO areaA, AreaDTO areaB, List<LocalTime> queryTimes, TramServiceDate queryDate) {
        Set<RawJourney> journeys = new LinkedHashSet<>(); // order matters
        Node endNode = getAreaNode(areaA.getAreaName());

        try (Transaction tx = graphDatabaseService.beginTx()) {
            Node startNode = getAreaNode(areaB.getAreaName());

            Set<String> preferRoute = Collections.EMPTY_SET;
            gatherJounerys(startNode, endNode, queryTimes, queryDate, MAX_NUM_GRAPH_PATHS, journeys, preferRoute);

            tx.success();
        }
        return journeys;
    }

    public Set<RawJourney> calculateRoute(LatLong origin, List<StationWalk> stationWalks, Station endStation,
                                          List<LocalTime> queryTimes, TramServiceDate queryDate) {
        Set<RawJourney> journeys = new LinkedHashSet<>(); // order matters
        int limit = stationWalks.isEmpty() ? MAX_NUM_GRAPH_PATHS : (MAX_NUM_GRAPH_PATHS * stationWalks.size());
        Node endNode = getStationNode(endStation.getId());
        List<Relationship> addedWalks = new LinkedList<>();

        try (Transaction tx = graphDatabaseService.beginTx()) {

            Node startNode = graphDatabaseService.createNode(TransportGraphBuilder.Labels.QUERY_NODE);
            startNode.setProperty(GraphStaticKeys.Station.LAT, origin.getLat());
            startNode.setProperty(GraphStaticKeys.Station.LONG, origin.getLon());
            startNode.setProperty(GraphStaticKeys.Station.NAME, queryNodeName);
            logger.info(format("Added start node at %s as node %s", origin, startNode));

            stationWalks.forEach(stationWalk -> {
                String id = stationWalk.getId();
                Node node = getStationNode(id);
                int cost = stationWalk.getCost();
                logger.info(format("Add walking relationship from %s to %s cost %s", startNode, id, cost));
                Relationship walkingRelationship = startNode.createRelationshipTo(node, TransportRelationshipTypes.WALKS_TO);
                walkingRelationship.setProperty(GraphStaticKeys.COST, cost);
                addedWalks.add(walkingRelationship);
            });

            gatherJounerys(startNode, endNode, queryTimes, queryDate, limit, journeys, endStation.getRoutes());

            // must delete relationships first, otherwise may not delete node
            addedWalks.forEach(walk -> walk.delete());
            startNode.delete();
        }
        return journeys;
    }

    private void gatherJounerys(Node startNode, Node endNode, List<LocalTime> queryTimes, TramServiceDate queryDate,
                                int limit, Set<RawJourney> journeys, Set<String> preferRoutes) {
        Set<String> runningServices = transportData.getServicesOnDate(queryDate).
                stream().
                map(svc -> svc.getServiceId()).
                collect(Collectors.toSet());

        queryTimes.forEach(queryTime -> {
            ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, config,
                    queryTime, runningServices, preferRoutes);
            LazyTimeBasedPathExpander pathExpander = new LazyTimeBasedPathExpander(queryTime, relationshipFactory,
                    serviceHeuristics, config, nodeOperations);
            Stream<WeightedPath> paths = findShortestPath(startNode, endNode, queryTime, queryDate, pathExpander);
            logger.info(format("Journey from %s to %s at %s on %s limit:%s", startNode, endNode, queryTime, queryDate, limit));
            if (!mapStreamToJourneySet(journeys, paths, limit, queryTime)) {
                serviceHeuristics.reportReasons();
                pathExpander.reportVisits(MAX_NUM_GRAPH_PATHS);
            }
            serviceHeuristics.reportStats();
        });
    }

    private boolean mapStreamToJourneySet(Set<RawJourney> journeys, Stream<WeightedPath> paths,
                                       int limit, LocalTime queryTime) {
        paths.limit(limit).forEach(path -> {
            logger.info(format("parse graph path of length %s with limit of %s ", path.length(), limit));
            try {
                List<RawStage> stages = pathToStages.map(path, queryTime);
                RawJourney journey = new RawJourney(stages, queryTime);
                journeys.add(journey);
            } catch (TramchesterException exception) {
                logger.error("Failed to parse paths to a journey",exception);
            }
        });
        paths.close();

        boolean foundJourney = journeys.size() > 0;
        if (!foundJourney) {
            logger.warn(format("Did not create any journeys from the limit:%s queryTime:%s",
                    limit, queryTime));
        }
        return foundJourney;
    }

    public TramNode getStation(String stationId) {
        Node node = getStationNode(stationId);
        return nodeFactory.getNode(node);
    }

    private Stream<WeightedPath> findShortestPath(Node startNode, Node endNode, LocalTime queryTime,
                                                  TramServiceDate queryDate, PathExpander<Double> pathExpander) {
        logger.info(format("Finding shortest path for %s --> %s on %s at %s",
                startNode.getProperty(GraphStaticKeys.Station.NAME),
                endNode.getProperty(GraphStaticKeys.Station.NAME), queryDate, queryTime));

        if (startNode.getProperty(GraphStaticKeys.Station.NAME).equals(queryNodeName)) {
            logger.info("Query node based search, setting start time to actual query time");
        }

        Dijkstra pathFinder = new Dijkstra(pathExpander, costEvaluator,
                PathInterestFactory.numberOfShortest(NoneStrictMath.EPSILON, MAX_NUM_GRAPH_PATHS));

        Iterable<WeightedPath> pathIterator = pathFinder.findAllPaths(startNode, endNode);
        
        return StreamSupport.stream(pathIterator.spliterator(), false);
    }

    public List<TransportRelationship> getOutboundRouteStationRelationships(String routeStationId) throws TramchesterException {
        return graphQuery.getRouteStationRelationships(routeStationId, Direction.OUTGOING);
    }

    public List<TransportRelationship> getInboundRouteStationRelationships(String routeStationId) throws TramchesterException {
        return graphQuery.getRouteStationRelationships(routeStationId, Direction.INCOMING);
    }
}

