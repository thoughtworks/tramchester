package com.tramchester.graph;

import com.tramchester.domain.Route;
import com.tramchester.domain.Station;
import com.tramchester.domain.input.TramInterchanges;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.BranchOrderingPolicies;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.tramchester.graph.GraphStaticKeys.*;
import static com.tramchester.graph.TransportGraphBuilder.Labels.ROUTE_STATION;
import static com.tramchester.graph.TransportRelationshipTypes.*;

public class TramRouteReachable extends StationIndexs {

    public TramRouteReachable(GraphDatabaseService graphDatabaseService, GraphQuery graphQuery) {
        super(graphDatabaseService, graphQuery, false);
    }

    public boolean getRouteReachableWithInterchange(String startStationId, String endStationId, String routeId) {
        Evaluator evaluator = new MatchOrInterchangeEvaluator(endStationId, routeId);
        return evaluatePaths(startStationId, evaluator);
    }

    public List<Route> getRoutesFromStartToNeighbour(Station startStation, String endStationId) {
        List<Route> results = new ArrayList<>();
        Set<Route> firstRoutes = startStation.getRoutes();
        String startStationId = startStation.getId();

        try (Transaction tx = graphDatabaseService.beginTx()) {
            firstRoutes.forEach(route -> {
                Node routeStation = getRouteStationNode(startStationId + route.getId());
                Iterable<Relationship> edges = routeStation.getRelationships(ON_ROUTE, Direction.OUTGOING);
                for (Relationship edge : edges) {
                    if (endStationId.equals(edge.getEndNode().getProperty(STATION_ID).toString())) {
                        results.add(route);
                    }
                }
            });

        }
        return results;
    }

    private boolean evaluatePaths(String startStationId, Evaluator evaluator) {
        long number = 0;
        try (Transaction tx = graphDatabaseService.beginTx()) {
            number = generatePaths(startStationId, evaluator).stream().count();
            tx.success();
        }
        return number>0;
    }

    public int getApproxCostBetween(Station start, Station desination) {
        return getApproxCostBetween(start.getId(), desination.getId());
    }

    public int getApproxCostBetween(String startId, String desinationId) {
        PathExpander<Object> forTypesAndDirections = PathExpanders.forTypesAndDirections(
                ON_ROUTE, Direction.OUTGOING,
                ENTER_PLATFORM, Direction.OUTGOING,
                LEAVE_PLATFORM, Direction.OUTGOING,
                BOARD, Direction.OUTGOING,
                DEPART, Direction.OUTGOING,
                INTERCHANGE_BOARD, Direction.OUTGOING,
                INTERCHANGE_DEPART, Direction.OUTGOING
        );

        int result;
        try (Transaction tx = graphDatabaseService.beginTx()) {

            PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra(forTypesAndDirections, COST);
            Node startNode = getStationNode(startId);
            Node endNode = getStationNode(desinationId);

            WeightedPath path = finder.findSinglePath(startNode, endNode);
            Double weight  = Math.floor(path.weight());
            result = weight.intValue();
        }

        return result;
    }

    private ResourceIterator<Path> generatePaths(String startStationId, Evaluator evaluator) {
        Node startNode = getStationNode(startStationId);
        Traverser traverser = new MonoDirectionalTraversalDescription().
                relationships(ON_ROUTE, Direction.OUTGOING).
                relationships(ENTER_PLATFORM, Direction.OUTGOING).
                relationships(BOARD, Direction.OUTGOING).
                relationships(INTERCHANGE_BOARD, Direction.OUTGOING).
                order(BranchOrderingPolicies.PREORDER_DEPTH_FIRST)
                .evaluator(evaluator)
                .traverse(startNode);
        return traverser.iterator();
    }

    private static class MatchOrInterchangeEvaluator implements Evaluator {
        private final String endStationId;
        private final String routeId;

        public MatchOrInterchangeEvaluator(String endStationId, String routeId) {
            this.endStationId = endStationId;
            this.routeId = routeId;
        }

        @Override
        public Evaluation evaluate(Path path) {
            Node queryNode = path.endNode();

            if (queryNode.hasLabel(ROUTE_STATION)) {
                String currentStationId = queryNode.getProperty(STATION_ID).toString();
                if (endStationId.equals(currentStationId)) {
                    return Evaluation.INCLUDE_AND_PRUNE; // finished, at dest
                }
                if (TramInterchanges.has(currentStationId)) {
                    return Evaluation.INCLUDE_AND_PRUNE; // finished, at interchange
                }
            }

            if (queryNode.hasRelationship(Direction.OUTGOING, ON_ROUTE)) {
                Relationship routeRelat = queryNode.getSingleRelationship(ON_ROUTE, Direction.OUTGOING);
                String id = routeRelat.getProperty(ROUTE_ID).toString();
                if (routeId.equals(id)) {
                    return Evaluation.EXCLUDE_AND_CONTINUE; // only follow if on same route
                } else {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }
            }

            return Evaluation.EXCLUDE_AND_CONTINUE;
        }
    }
}
