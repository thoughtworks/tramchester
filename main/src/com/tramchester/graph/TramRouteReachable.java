package com.tramchester.graph;

import com.tramchester.domain.input.TramInterchanges;
import com.tramchester.graph.Relationships.RelationshipFactory;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.*;
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription;

import static com.tramchester.graph.GraphStaticKeys.ROUTE_ID;
import static com.tramchester.graph.GraphStaticKeys.STATION_ID;
import static com.tramchester.graph.TransportGraphBuilder.Labels.ROUTE_STATION;
import static com.tramchester.graph.TransportRelationshipTypes.*;

public class TramRouteReachable extends StationIndexs {
    public TramRouteReachable(GraphDatabaseService graphDatabaseService, RelationshipFactory relationshipFactory,
                              SpatialDatabaseService spatialDatabaseService) {
        super(graphDatabaseService, relationshipFactory, spatialDatabaseService, false);
    }

    public boolean getRouteReachable(String startStationId, String endStationId, String routeId) {
        Evaluator evaluator = new ExactMatchEvaluator(endStationId, routeId);
        return evaluatePaths(startStationId, evaluator);
    }

    public boolean getRouteReachableWithInterchange(String startStationId, String endStationId, String routeId) {
        Evaluator evaluator = new MatchOrInterchangeEvaluator(endStationId, routeId);
        return evaluatePaths(startStationId, evaluator);
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

    private class ExactMatchEvaluator implements Evaluator {

        private final String routeId;
        private final String finishNodeId;

        public ExactMatchEvaluator(String finishNodeId, String routeId) {
            this.finishNodeId = finishNodeId;
            this.routeId = routeId;
        }

        @Override
        public Evaluation evaluate(Path path) {
            Node queryNode = path.endNode();

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

    private class MatchOrInterchangeEvaluator implements Evaluator {
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
                String routeStationId = queryNode.getProperty(STATION_ID).toString();
                if (endStationId.equals(routeStationId)) {
                    return Evaluation.INCLUDE_AND_PRUNE;
                }
                if (TramInterchanges.has(routeStationId)) {
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
}
