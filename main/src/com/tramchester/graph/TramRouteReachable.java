package com.tramchester.graph;

import com.tramchester.domain.Route;
import com.tramchester.domain.Station;
import com.tramchester.domain.input.TramInterchanges;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.BranchOrderingPolicies;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.tramchester.graph.GraphStaticKeys.ROUTE_ID;
import static com.tramchester.graph.GraphStaticKeys.STATION_ID;
import static com.tramchester.graph.TransportGraphBuilder.Labels.ROUTE_STATION;
import static com.tramchester.graph.TransportRelationshipTypes.*;

public class TramRouteReachable extends StationIndexs {

    public TramRouteReachable(GraphDatabaseService graphDatabaseService, GraphQuery graphQuery) {
        super(graphDatabaseService, graphQuery, false);
    }

    public boolean getRouteReachable(String startStationId, String targetStationId, String routeId) {
        Evaluator evaluator = new ExactMatchEvaluator(targetStationId, routeId);
        return evaluatePaths(startStationId, evaluator);
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

            Node startNode = getStationNode(startStationId);
            Traverser traverser = new MonoDirectionalTraversalDescription().
                    relationships(ON_ROUTE, Direction.OUTGOING).
                    relationships(ENTER_PLATFORM, Direction.OUTGOING).
                    relationships(BOARD, Direction.OUTGOING).
                    relationships(INTERCHANGE_BOARD, Direction.OUTGOING).
                    order( BranchOrderingPolicies.PREORDER_DEPTH_FIRST)
                    .evaluator(evaluator)
                    .traverse(startNode);
            ResourceIterator<Path> paths = traverser.iterator();

            number = paths.stream().count();

            tx.success();
        }
        return number>0;
    }

    private static class ExactMatchEvaluator implements Evaluator {

        private final String routeId;
        private final String finishNodeId;

        public ExactMatchEvaluator(String targetStationId, String routeId) {
            this.finishNodeId = targetStationId;
            this.routeId = routeId;
        }

        @Override
        public Evaluation evaluate(Path path) {
            Node queryNode = path.endNode();

            // route stations include the ID of the final station
            if (queryNode.hasLabel(ROUTE_STATION)) {
                String routeStationId = queryNode.getProperty(STATION_ID).toString();
                if (finishNodeId.equals(routeStationId)) {
                    return Evaluation.INCLUDE_AND_PRUNE;
                }
            }

            if (queryNode.hasRelationship(Direction.OUTGOING, ON_ROUTE)) {
                Relationship routeRelat = queryNode.getSingleRelationship(ON_ROUTE, Direction.OUTGOING);
                String id = routeRelat.getProperty(ROUTE_ID).toString();
                if (routeId.equals(id)) {
                    return Evaluation.EXCLUDE_AND_CONTINUE;
                } else {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }
            }

            return Evaluation.EXCLUDE_AND_CONTINUE;
        }
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
                    return Evaluation.EXCLUDE_AND_CONTINUE;
                } else {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }
            }

            return Evaluation.EXCLUDE_AND_CONTINUE;
        }
    }
}
