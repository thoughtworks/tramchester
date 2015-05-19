package com.tramchester.graph;

import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.Journey;
import com.tramchester.domain.Stage;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.RouteStationNode;
import com.tramchester.graph.Nodes.StationNode;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.Relationships.GoesToRelationship;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TramRelationship;
import org.neo4j.cypher.internal.compiler.v1_9.pipes.matching.GraphRelationship;
import org.neo4j.graphalgo.*;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.traversal.InitialBranchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.tramchester.graph.GraphStaticKeys.COST;
import static com.tramchester.graph.GraphStaticKeys.Station;
import static com.tramchester.graph.GraphStaticKeys.ID;
import static com.tramchester.graph.GraphStaticKeys.Station.NAME;

public class RouteCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculator.class);

    public static final int MAX_WAIT_TIME_MINS = 25; // todo into config
    public static final CostEvaluator<Double> COST_EVALUATOR = CommonEvaluators.doubleCostEvaluator(COST);
//    private static final PathExpander<GraphBranchState> pathExpander =
//            new TimeBasedPathExpander(COST_EVALUATOR, MAX_WAIT_TIME_MINS);

    private final GraphDatabaseService db;
    private NodeFactory nodeFactory;

    private Index<Node> trams = null;
    private Index<Node> routeStations = null;
    private PathExpander pathExpander;

    public RouteCalculator(GraphDatabaseService db) {
        this.db = db;
        nodeFactory = new NodeFactory();
        RelationshipFactory routeFactory = new RelationshipFactory();
        pathExpander = new TimeBasedPathExpander(COST_EVALUATOR, MAX_WAIT_TIME_MINS, routeFactory, nodeFactory);
    }

    public Set<Journey> calculateRoute(String startStationId, String endStationId, int time, DaysOfWeek dayOfWeek) throws UnknownStationException {
        Set<Journey> journeys = new HashSet<>();
        try (Transaction tx = db.beginTx()) {

            Iterable<WeightedPath> pathIterator = findShortestPath(startStationId, endStationId, time, dayOfWeek);
            int index = 0;
            // todo eliminate duplicate journeys that use different services??
            for (WeightedPath path : pathIterator) {
                logger.info("Map journey of length " + path.length());
                Journey journey = mapJourney(path);
                journey.setJourneyIndex(index++);
                journeys.add(journey);
                if(index == 2){
                    break;
                }
            }
            tx.success();
        }
        return journeys;
    }

    private Journey mapJourney(WeightedPath path) {
        List<Stage> stages = new ArrayList<>();
        Stage currentStage = null;

        Iterable<Relationship> relationships = path.relationships();

        int totalCost = 0;
        for (Relationship graphRelationship : relationships) {
            TramRelationship tramRelationship = new RelationshipFactory().getRelationship(graphRelationship);

            TramNode startNode = nodeFactory.getNode(graphRelationship.getStartNode());
            String startNodeId = startNode.getId();

            TramNode endNode = nodeFactory.getNode(graphRelationship.getEndNode());
            String endNodeId = endNode.getId();

            int cost = tramRelationship.getCost();
            totalCost += cost;

            if (tramRelationship.isBoarding() || tramRelationship.isInterchange()) {
                // station -> route station
                RouteStationNode routeStationNode = (RouteStationNode) endNode;
                String routeName = routeStationNode.getRouteName();
                String routeId = routeStationNode.getRouteId();
                logger.info(String.format("board tram: at:'%s' from '%s'", endNode, startNode));
                currentStage = new Stage(startNodeId, routeName, routeId);
            } else {
                if (tramRelationship.isDepartTram()) {
                    // route station -> station
                    StationNode stationNode = (StationNode) endNode;
                    String stationName = stationNode.getName();
                    logger.info(String.format("depart tram: at:'%s' to: '%s' '%s' ", startNodeId, stationName, endNodeId));
                    currentStage.setLastStation(endNodeId);
                    stages.add(currentStage);
                } else if (tramRelationship.isGoesTo()) {
                    // routeStation -> routeStation
                    GoesToRelationship goesToRelationship = (GoesToRelationship) tramRelationship;
                    currentStage.setServiceId(goesToRelationship.getService());
//                    logger.debug(String.format("Add step, goes from %s to %s on %s", startNodeId,
//                        endNodeId, tramRelationship.getService()));
                }
            }
        }
        logger.info(String.format("Number of stages: %s Total cost:%s ",stages.size(), totalCost));
        return new Journey(stages);
    }

    private Iterable<WeightedPath> findShortestPath(String start, String end, int time, DaysOfWeek dayOfWeek) throws UnknownStationException {
        Index<Node> stationsIndex = getStationsIndex();
        Node startNode = getStationByID(start, stationsIndex);
        Node endNode = getStationByID(end, stationsIndex);
        logger.info(String.format("Finding shortest path for (%s) --> (%s) on %s",
                startNode.getProperty(NAME),
                endNode.getProperty(NAME), dayOfWeek));

        GraphBranchState state = new GraphBranchState(time, dayOfWeek);
        PathFinder<WeightedPath> pathFinder = GraphAlgoFactory.dijkstra(
                pathExpander,
                new InitialBranchState.State<>(state, state),
                COST_EVALUATOR);

        Iterable<WeightedPath> allPaths = pathFinder.findAllPaths(startNode, endNode);

        return allPaths;
    }

    private Node getStationByID(String stationId, Index<Node> stationsIndex) throws UnknownStationException {
        Node node = stationsIndex.get(ID, stationId).getSingle();
        if (node==null) {
            logger.error("Unable to find station for ID " + stationId);
            throw new UnknownStationException(stationId);
        }
        return node;
    }

    private Index<Node> getStationsIndex() {
        if (trams == null) {
            trams = db.index().forNodes(Station.IndexName);
        }
        return trams;
    }

    private Index<Node> getRouteStationsIndex() {
        if (routeStations == null) {
            routeStations = db.index().forNodes(GraphStaticKeys.RouteStation.IndexName);
        }
        return routeStations;
    }

    public List<TramRelationship> getOutboundStationRelationships(String stationId) throws UnknownStationException {
        RelationshipFactory relationshipFactory = new RelationshipFactory();

        try (Transaction tx = db.beginTx()) {
            List<TramRelationship> relationships = new LinkedList<>();
            Node startNode = getStationByID(stationId, getStationsIndex());
            for (Relationship relate : startNode.getRelationships(Direction.OUTGOING)) {
                relationships.add(relationshipFactory.getRelationship(relate));
            }
            tx.success();
            return relationships;
        }
    }

    public List<TramRelationship> getOutboundRouteStationRelationships(String routeStationId) throws UnknownStationException {
        RelationshipFactory relationshipFactory = new RelationshipFactory();

        try (Transaction tx = db.beginTx()) {
            List<TramRelationship> relationships = new LinkedList<>();
            Node startNode = getStationByID(routeStationId, getRouteStationsIndex());
            for (Relationship relate : startNode.getRelationships(Direction.OUTGOING)) {
                relationships.add(relationshipFactory.getRelationship(relate));
            }
            tx.success();
            return relationships;
        }
    }
}
