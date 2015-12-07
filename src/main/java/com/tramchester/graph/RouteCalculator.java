package com.tramchester.graph;

import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.Journey;
import com.tramchester.domain.Stage;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.RouteStationNode;
import com.tramchester.graph.Nodes.StationNode;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.Relationships.TramGoesToRelationship;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TramRelationship;
import org.neo4j.graphalgo.*;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.InitialBranchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.tramchester.graph.GraphStaticKeys.COST;
import static com.tramchester.graph.GraphStaticKeys.Station.NAME;

public class RouteCalculator extends StationIndexs {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculator.class);

    public static final int MAX_WAIT_TIME_MINS = 25; // todo into config
    public static final CostEvaluator<Double> COST_EVALUATOR = CommonEvaluators.doubleCostEvaluator(COST);

    private NodeFactory nodeFactory;
    private PathExpander pathExpander;

    public RouteCalculator(GraphDatabaseService db) {
        super(db, true);
        nodeFactory = new NodeFactory();
        RelationshipFactory routeFactory = new RelationshipFactory();
        pathExpander = new TimeBasedPathExpander(COST_EVALUATOR, MAX_WAIT_TIME_MINS, routeFactory, nodeFactory);
    }

    public Set<Journey> calculateRoute(String startStationId, String endStationId, int time, DaysOfWeek dayOfWeek, TramServiceDate queryDate) throws UnknownStationException {
        Set<Journey> journeys = new HashSet<>();
        try (Transaction tx = graphDatabaseService.beginTx()) {

            Iterable<WeightedPath> pathIterator = findShortestPath(startStationId, endStationId, time, dayOfWeek, queryDate);
            // todo eliminate duplicate journeys that use different services??
            Stream<WeightedPath> paths = StreamSupport.stream(pathIterator.spliterator(), false);
            paths.limit(2).forEach(path->{
                logger.info("Map journey of length " + path.length());
                Journey journey = mapJourney(path);
                journey.setJourneyIndex(journeys.size());
                journeys.add(journey);
                });
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

            // todo refactor out first and subsequent stage handling
            if (tramRelationship.isBoarding()) {
                // station -> route station
                RouteStationNode routeStationNode = (RouteStationNode) endNode;
                String routeName = routeStationNode.getRouteName();
                String routeId = routeStationNode.getRouteId();
                logger.info(String.format("board tram: at:'%s' from '%s'", endNode, startNode));
                currentStage = new Stage(startNodeId, routeName, routeId);
            } else if (tramRelationship.isDepartTram()) {
                // route station -> station
                StationNode stationNode = (StationNode) endNode;
                String stationName = stationNode.getName();
                logger.info(String.format("depart tram: at:'%s' to: '%s' '%s' ", startNodeId, stationName, endNodeId));
                currentStage.setLastStation(endNodeId);
                stages.add(currentStage);
            } else if (tramRelationship.isTramGoesTo()) {
                // routeStation -> routeStation
                TramGoesToRelationship tramGoesToRelationship = (TramGoesToRelationship) tramRelationship;
                currentStage.setServiceId(tramGoesToRelationship.getService());
                logger.debug(String.format("Add step, goes from %s to %s on %s", startNodeId,
                    endNodeId, tramGoesToRelationship.getService()));
            }
        }
        logger.info(String.format("Number of stages: %s Total cost:%s ",stages.size(), totalCost));
        return new Journey(stages);
    }

    public TramNode getStation(String id) throws UnknownStationException {
        try (Transaction tx = graphDatabaseService.beginTx()) {
            NodeFactory factory = new NodeFactory();
            Node node =  super.getStationNode(id);
            tx.success();
            return factory.getNode(node);
        }
    }

    public TramNode getRouteStation(String id) throws UnknownStationException {
        try (Transaction tx = graphDatabaseService.beginTx()) {
            NodeFactory factory = new NodeFactory();
            Node node = getRouteStationNode(id);
            tx.success();
            return factory.getNode(node);
        }
    }

    private Iterable<WeightedPath> findShortestPath(String startId, String endId, int time, DaysOfWeek dayOfWeek, TramServiceDate queryDate) throws UnknownStationException {
        Node startNode = getStationNode(startId);
        Node endNode = getStationNode(endId);
        logger.info(String.format("Finding shortest path for %s (%s) --> %s (%s) on %s",
                startId, startNode.getProperty(NAME),
                endId, endNode.getProperty(NAME), dayOfWeek));

        GraphBranchState state = new GraphBranchState(time, dayOfWeek, endId, queryDate);
        PathFinder<WeightedPath> pathFinder = GraphAlgoFactory.dijkstra(
                pathExpander,
                new InitialBranchState.State<>(state, state),
                COST_EVALUATOR);

        return pathFinder.findAllPaths(startNode, endNode);
    }

    public List<TramRelationship> getOutboundStationRelationships(String stationId) throws UnknownStationException {
        try (Transaction tx = graphDatabaseService.beginTx()) {
            Node node = getStationNode(stationId);
            List<TramRelationship> relationships = getTramRelationships(node, Direction.OUTGOING);
            tx.success();
            return relationships;
        }
    }

    public List<TramRelationship> getOutboundRouteStationRelationships(String routeStationId) throws UnknownStationException {
        try (Transaction tx = graphDatabaseService.beginTx()) {
            Node node = getRouteStationNode(routeStationId);
            List<TramRelationship> relationships = getTramRelationships(node, Direction.OUTGOING);
            tx.success();
            return relationships;
        }
    }

    public List<TramRelationship> getInboundRouteStationRelationships(String routeStationId) throws UnknownStationException {
        try (Transaction tx = graphDatabaseService.beginTx()) {
            Node node = getRouteStationNode(routeStationId);
            List<TramRelationship> relationships = getTramRelationships(node, Direction.INCOMING);
            tx.success();
            return relationships;
        }
    }

    private List<TramRelationship> getTramRelationships(Node startNode, Direction direction) throws UnknownStationException {
        RelationshipFactory relationshipFactory = new RelationshipFactory();

        List<TramRelationship> relationships = new LinkedList<>();
        for (Relationship relate : startNode.getRelationships(direction)) {
            relationships.add(relationshipFactory.getRelationship(relate));
        }
        return relationships;
    }

}
