package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.places.Station;
import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.*;
import org.reflections.vfs.Vfs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static com.tramchester.graph.GraphPropertyKey.COST;
import static com.tramchester.graph.TransportRelationshipTypes.*;

@LazySingleton
public class RouteCostCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RouteCostCalculator.class);

    private final GraphQuery graphQuery;
    private final GraphDatabase graphDatabaseService;

    @Inject
    public RouteCostCalculator(GraphQuery graphQuery, GraphDatabase graphDatabaseService) {
        this.graphQuery = graphQuery;
        this.graphDatabaseService = graphDatabaseService;
    }

    public int getApproxCostBetween(Transaction txn, Station station, Node endNode) {
        Node startNode = graphQuery.getStationNode(txn, station);
        return getApproxCostBetween(txn, startNode, endNode);
    }

    // startNode must have been found within supplied txn
    public int getApproxCostBetween(Transaction txn, Node startNode, Station endStation) {
        Node endNode = graphQuery.getStationNode(txn, endStation);
        return getApproxCostBetween(txn, startNode, endNode);
    }

    public int getApproxCostBetween(Transaction txn, Station startStation, Station endStation) {
        Node startNode = graphQuery.getStationNode(txn, startStation);
        if (startNode==null) {
            throw new RuntimeException("Could not find start node for " + startStation.getId());
        }
        Node endNode = graphQuery.getStationNode(txn, endStation);
        if (endNode==null) {
            throw new RuntimeException("Could not find end node for " + endStation.getId());
        }

        return getApproxCostBetween(txn, startNode, endNode);
    }

    // startNode and endNode must have been found within supplied txn
    public int getApproxCostBetween(Transaction txn, Node startNode, Node endNode) {
        // follow the ON_ROUTE relationships to quickly find a route without any timing information or check
        PathExpander<Double> forTypesAndDirections = fullExpanderForRoutes();

        EvaluationContext context = graphDatabaseService.createContext(txn);
        PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra(context, forTypesAndDirections, COST.getText());

        WeightedPath path = finder.findSinglePath(startNode, endNode);
        if (path==null) {
            logger.error("No path found ");
            return -1;
        }
        double weight  = Math.floor(path.weight());
        return (int) weight;
    }

    private PathExpander<Double> fullExpanderForRoutes() {
        return PathExpanders.forTypesAndDirections(
                ON_ROUTE, Direction.OUTGOING,
                ENTER_PLATFORM, Direction.OUTGOING,
                LEAVE_PLATFORM, Direction.OUTGOING,
                BOARD, Direction.OUTGOING,
                DEPART, Direction.OUTGOING,
                INTERCHANGE_BOARD, Direction.OUTGOING,
                INTERCHANGE_DEPART, Direction.OUTGOING,
                WALKS_TO, Direction.OUTGOING,
                WALKS_FROM, Direction.OUTGOING,
                FINISH_WALK, Direction.OUTGOING,
                NEIGHBOUR, Direction.OUTGOING,
                GROUPED_TO_PARENT, Direction.OUTGOING,
                GROUPED_TO_CHILD, Direction.OUTGOING
        );
    }
}
