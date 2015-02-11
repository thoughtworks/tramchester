package com.tramchester.graph;

import com.tramchester.domain.DaysOfWeek;
import org.neo4j.graphalgo.*;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.traversal.InitialBranchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tramchester.graph.GraphStaticKeys.*;
import static com.tramchester.graph.GraphStaticKeys.Station.ID;
import static com.tramchester.graph.GraphStaticKeys.Station.NAME;

public class RouteCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculator.class);
    private static final CostEvaluator<Double> COST_EVALUATOR = CommonEvaluators.doubleCostEvaluator(COST);
    private static final PathExpander<GraphBranchState> pathExpander = new TimeBasedPathExpander(COST_EVALUATOR);
    private final GraphDatabaseService db;
    private Index<Node> trams = null;

    public RouteCalculator(GraphDatabaseService db) {
        this.db = db;
    }

    public void calculateRoute(String start, String end, int time) {
        try (Transaction tx = db.beginTx()) {

            Iterable<WeightedPath> paths = findShortestPath(start, end, time);

            for (WeightedPath path : paths) {
                printRoute(path);
            }

            tx.success();
        }
    }

    private void printRoute(WeightedPath path) {
        String stringPath = "\n";

        Iterable<Relationship> relationships = path.relationships();
        for (Relationship relationship : relationships) {
            if (relationship.isType(TransportRelationshipTypes.GOES_TO)) {
                if (path.startNode().equals(relationship.getStartNode())) {
                    stringPath += String.format("(%s)", relationship.getStartNode().getProperty("name"));
                }
                stringPath += "---" + relationship.getProperty("route") + "-" + relationship.getProperty("service_id") + "-->";
                if (relationship.getEndNode().hasProperty("name")) {
                    stringPath += String.format("(%s)", relationship.getEndNode().getProperty("name"));
                }
            } else if (relationship.isType(TransportRelationshipTypes.BOARD)) {
                if (path.startNode().equals(relationship.getStartNode())) {
                    stringPath += String.format("(%s)", relationship.getStartNode().getProperty("name"));
                }
            } else if (relationship.isType(TransportRelationshipTypes.DEPART)) {
                stringPath += String.format("(%s)", relationship.getEndNode().getProperty("name"));
            }
        }
        stringPath += "weight: " + path.weight();
        System.out.println(stringPath);
        System.out.println("------------------------------------------------------------------------------------");

    }

    private Iterable<WeightedPath> findShortestPath(String start, String end, int time) {
        Node startNode = getStationsIndex().get(ID, start).getSingle();
        Node endNode = getStationsIndex().get(ID, end).getSingle();
        logger.info(String.format("Finding shortest path for (%s) --> (%s)", startNode.getProperty(NAME), endNode.getProperty(NAME)));

        GraphBranchState state = new GraphBranchState(time, DaysOfWeek.fromToday());
        PathFinder<WeightedPath> pathFinder = GraphAlgoFactory.dijkstra(
                pathExpander,
                new InitialBranchState.State<>(state, state),
                COST_EVALUATOR);

        return pathFinder.findAllPaths(startNode, endNode);
    }

    private Index<Node> getStationsIndex() {
        if (trams == null) {
            trams = db.index().forNodes(Station.IndexName);
        }
        return trams;
    }
}
