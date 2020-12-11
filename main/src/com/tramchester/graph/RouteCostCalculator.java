package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.places.Station;
import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.*;

import javax.inject.Inject;

import static com.tramchester.graph.GraphPropertyKey.COST;
import static com.tramchester.graph.TransportRelationshipTypes.*;

@LazySingleton
public class RouteCostCalculator {
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
        Node endNode = graphQuery.getStationNode(txn, endStation);
        return getApproxCostBetween(txn, startNode, endNode);
    }

    // startNode and endNode must have been found within supplied txn
    public int getApproxCostBetween(Transaction txn, Node startNode, Node endNode) {
        // follow the ON_ROUTE relationships to quickly find a route without any timing information or check
        PathExpander<Double> forTypesAndDirections = fullExpanderForRoutes();

        EvaluationContext context = graphDatabaseService.createContext(txn);
        PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra(context, forTypesAndDirections, COST.getText());
        WeightedPath path = finder.findSinglePath(startNode, endNode);
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
                FINISH_WALK, Direction.OUTGOING
        );
    }
}
