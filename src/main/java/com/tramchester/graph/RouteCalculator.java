package com.tramchester.graph;

import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.Journey;
import com.tramchester.domain.Stage;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.RouteStationNode;
import com.tramchester.graph.Nodes.StationNode;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TramGoesToRelationship;
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
import static com.tramchester.graph.GraphStaticKeys.QUERY;
import static com.tramchester.graph.GraphStaticKeys.STATION_TYPE;
import static com.tramchester.graph.GraphStaticKeys.Station.NAME;
import static java.lang.String.format;

public class RouteCalculator extends StationIndexs {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculator.class);

    public static final int MAX_WAIT_TIME_MINS = 25; // todo into config

    // TODO Use INT
    public static final CostEvaluator<Double> COST_EVALUATOR = CommonEvaluators.doubleCostEvaluator(COST);
    public static final int MAX_NUM_GRAPH_PATHS = 2;

    private NodeFactory nodeFactory;
    private PathExpander pathExpander;

    public RouteCalculator(GraphDatabaseService db, NodeFactory nodeFactory, RelationshipFactory relationshipFactory) {
        super(db, true);
        this.nodeFactory = nodeFactory;
        pathExpander = new TimeBasedPathExpander(COST_EVALUATOR, MAX_WAIT_TIME_MINS, relationshipFactory, nodeFactory);
    }

    public Set<Journey> calculateRoute(String startStationId, String endStationId, int queryTime, DaysOfWeek dayOfWeek,
                                       TramServiceDate queryDate) throws UnknownStationException {
        Set<Journey> journeys = new HashSet<>();

        try (Transaction tx = graphDatabaseService.beginTx()) {
            Node startNode = getStationNode(startStationId);
            Node endNode = getStationNode(endStationId);

            Stream<WeightedPath> paths = findShortestPath(startNode, endNode, queryTime, dayOfWeek, queryDate);
            // todo eliminate duplicate journeys that use different services??

            mapStreamToJourneySet(journeys, paths, MAX_NUM_GRAPH_PATHS);
            tx.success();
        }
        return journeys;
    }

    public Set<Journey> calculateRoute(List<Node> starts, List<Node> ends, int minutesFromMidnight,
                                       DaysOfWeek dayOfWeek, TramServiceDate queryDate) throws UnknownStationException {
        Set<Journey> journeys = new HashSet<>();
        try (Transaction tx = graphDatabaseService.beginTx()) {
            Node startNode = graphDatabaseService.createNode(DynamicLabel.label("QUERY_NODE"));
            startNode.setProperty(NAME, "BEGIN");
            startNode.setProperty(STATION_TYPE, QUERY);
            starts.forEach(start -> startNode.
                    createRelationshipTo(start, TransportRelationshipTypes.WALKS_TO).
                    setProperty(COST,0));

            Node endNode = graphDatabaseService.createNode(DynamicLabel.label("QUERY_NODE"));
            endNode.setProperty(NAME, "END");
            endNode.setProperty(STATION_TYPE, QUERY);
            ends.forEach(end -> end.
                    createRelationshipTo(endNode, TransportRelationshipTypes.WALKS_TO).
                    setProperty(COST,0));

            Stream<WeightedPath> paths = findShortestPath(startNode, endNode, minutesFromMidnight, dayOfWeek, queryDate);

            int limit = MAX_NUM_GRAPH_PATHS * starts.size();
            mapStreamToJourneySet(journeys, paths, limit);

            startNode.delete();
            endNode.delete();

            tx.close();
        }
        return journeys;
    }

    private void mapStreamToJourneySet(Set<Journey> journeys, Stream<WeightedPath> paths, int limit) {
        paths.limit(limit).forEach(path->{
            logger.info("Map graph path of length " + path.length());
            List<Stage> stages = mapStages(path);
            Journey journey = new Journey(stages);
            journey.setJourneyIndex(journeys.size());
            journeys.add(journey);
        });
    }

    private List<Stage> mapStages(WeightedPath path) {
        List<Stage> stages = new ArrayList<>();
        Stage currentStage = null;

        Iterable<Relationship> relationships = path.relationships();
        RelationshipFactory relationshipFactory = new RelationshipFactory();

        int totalCost = 0;
        for (Relationship graphRelationship : relationships) {
            TramRelationship tramRelationship = relationshipFactory.getRelationship(graphRelationship);

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
                logger.info(format("board tram: at:'%s' from '%s'", endNode, startNode));
                String tramRouteClass = routeId.substring(4, 8); // todo move into helper
                currentStage = new Stage(startNodeId, routeName, tramRelationship.getMode(), tramRouteClass);
            } else if (tramRelationship.isTramGoesTo()) {
                // routeStation -> routeStation
                TramGoesToRelationship tramGoesToRelationship = (TramGoesToRelationship) tramRelationship;
                currentStage.setServiceId(tramGoesToRelationship.getService());
            } else if (tramRelationship.isDepartTram()) {
                // route station -> station
                StationNode stationNode = (StationNode) endNode;
                String stationName = stationNode.getName();
                logger.info(format("depart tram: at:'%s' to: '%s' '%s' ", startNodeId, stationName, endNodeId));
                currentStage.setLastStation(endNodeId);
                logger.info(format("Added stage: '%s'",currentStage));
                stages.add(currentStage);
            }
        }
        logger.info(format("Number of stages: %s Total cost:%s ",stages.size(), totalCost));
        return stages;
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

    private Stream<WeightedPath> findShortestPath(Node startNode, Node endNode, int queryTime, DaysOfWeek dayOfWeek,
                                                    TramServiceDate queryDate) {
        logger.info(format("Finding shortest path for %s --> %s on %s",
                startNode.getProperty(NAME),
                endNode.getProperty(NAME), dayOfWeek));

        GraphBranchState state = new GraphBranchState(dayOfWeek, queryDate, queryTime);
        PathFinder<WeightedPath> pathFinder = GraphAlgoFactory.dijkstra(
                pathExpander,
                new InitialBranchState.State<>(state, state),
                COST_EVALUATOR);

        Iterable<WeightedPath> pathIterator = pathFinder.findAllPaths(startNode, endNode);
        return StreamSupport.stream(pathIterator.spliterator(), false);
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
