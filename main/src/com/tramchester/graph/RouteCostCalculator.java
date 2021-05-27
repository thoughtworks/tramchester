package com.tramchester.graph;

import com.google.common.collect.Streams;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
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
    public RouteCostCalculator(GraphQuery graphQuery, GraphDatabase graphDatabaseService,
                               StagedTransportGraphBuilder.Ready ready) {
        this.graphQuery = graphQuery;
        this.graphDatabaseService = graphDatabaseService;
    }

    public int getApproxCostBetween(Transaction txn, Station station, Node endNode) {
        Node startNode = graphQuery.getStationOrGrouped(txn, station);
        return getApproxCostBetween(txn, startNode, endNode);
    }

    // startNode must have been found within supplied txn
    public int getApproxCostBetween(Transaction txn, Node startNode, Station endStation) {
        Node endNode = graphQuery.getStationOrGrouped(txn, endStation);
        return getApproxCostBetween(txn, startNode, endNode);
    }

    public int getApproxCostBetween(Transaction txn, Station startStation, Station endStation) {
        Node startNode = graphQuery.getStationOrGrouped(txn, startStation);
        if (startNode==null) {
            throw new RuntimeException("Could not find start node for graph id " + startStation.getId().getGraphId());
        }
        Node endNode = graphQuery.getStationOrGrouped(txn, endStation);
        if (endNode==null) {
            throw new RuntimeException("Could not find end node for graph id" + endStation.getId().getGraphId());
        }

        return getApproxCostBetween(txn, startNode, endNode);
    }

    // startNode and endNode must have been found within supplied txn
    public int getApproxCostBetween(Transaction txn, Node startNode, Node endNode) {
        PathExpander<Double> forTypesAndDirections = fullExpanderForCostApproximation();

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

    public long getNumberHops(Transaction txn, Station start, Station end) {
        PathExpander<Double> forTypesAndDirections = expanderForNumberHops();

        EvaluationContext context = graphDatabaseService.createContext(txn);
        PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra(context, forTypesAndDirections, COST.getText());

        Node startNode = graphQuery.getStationNode(txn, start);
        Node endNode = graphQuery.getStationNode(txn, end);

        WeightedPath path = finder.findSinglePath(startNode, endNode);
        if (path==null) {
            logger.error("No path found ");
            return -1;
        }

        return Streams.stream(path.relationships()).
                filter(relationship -> relationship.isType(ON_ROUTE)).
                count();
    }

    private PathExpander<Double> expanderForNumberHops() {
        return PathExpanders.forTypesAndDirections(
                ON_ROUTE, Direction.OUTGOING,
                CONNECT_ROUTES, Direction.OUTGOING,
                ROUTE_TO_STATION, Direction.OUTGOING,
                STATION_TO_ROUTE, Direction.OUTGOING
        );
    }

    private PathExpander<Double> fullExpanderForCostApproximation() {
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
                NEIGHBOUR, Direction.OUTGOING,
                GROUPED_TO_PARENT, Direction.OUTGOING,
                GROUPED_TO_CHILD, Direction.OUTGOING
        );
    }
}
