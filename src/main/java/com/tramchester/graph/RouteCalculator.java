package com.tramchester.graph;

import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.Journey;
import com.tramchester.domain.Stage;
import org.neo4j.graphalgo.*;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.traversal.InitialBranchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.tramchester.graph.GraphStaticKeys.COST;
import static com.tramchester.graph.GraphStaticKeys.Station;
import static com.tramchester.graph.GraphStaticKeys.Station.ID;
import static com.tramchester.graph.GraphStaticKeys.Station.NAME;
import static com.tramchester.graph.TransportRelationshipTypes.*;

public class RouteCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculator.class);

    private static final CostEvaluator<Double> COST_EVALUATOR = CommonEvaluators.doubleCostEvaluator(COST);
    private static final PathExpander<GraphBranchState> pathExpander = new TimeBasedPathExpander(COST_EVALUATOR);
    private final GraphDatabaseService db;
    private Index<Node> trams = null;

    public RouteCalculator(GraphDatabaseService db) {
        this.db = db;
    }

    public Set<Journey> calculateRoute(String start, String end, int time, DaysOfWeek dayOfWeek) {
        Set<Journey> journeys = new HashSet<>();
        try (Transaction tx = db.beginTx()) {

            Iterable<WeightedPath> paths = findShortestPath(start, end, time, dayOfWeek);
            int index = 0;

            for (WeightedPath path : paths) {
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
        Iterable<Relationship> relationships = path.relationships();
        Stage currentStage = null;

        for (Relationship relationship : relationships) {
            Node startNode = relationship.getStartNode();
            Node endNode = relationship.getEndNode();

            if (relationship.isType(BOARD)) {
                logger.info(String.format("board tram: at:'%s' route:'%s' routeId:%s",startNode.getProperty("id"), endNode.getProperty("route_name"), endNode.getProperty("route_id")));
                currentStage = new Stage(startNode.getProperty("id").toString(), endNode.getProperty("route_name").toString(), endNode.getProperty("route_id").toString());
            } else if (relationship.isType(DEPART)) {
                logger.info("depart tram: at:" + endNode.getProperty("id"));
                currentStage.setLastStation(endNode.getProperty("id").toString());
                stages.add(currentStage);
            } else if (relationship.isType(GOES_TO)) {
                currentStage.setServiceId(relationship.getProperty("service_id").toString());
            }
        }
        logger.info("Number of stages: " + stages.size());
        return new Journey(stages);
    }


    private Iterable<WeightedPath> findShortestPath(String start, String end, int time, DaysOfWeek dayOfWeek) {
        Node startNode = getStationsIndex().get(ID, start).getSingle();
        Node endNode = getStationsIndex().get(ID, end).getSingle();
        logger.info(String.format("Finding shortest path for (%s) --> (%s)", startNode.getProperty(NAME), endNode.getProperty(NAME)));

        GraphBranchState state = new GraphBranchState(time, dayOfWeek);
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
