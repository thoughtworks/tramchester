package com.tramchester.graph;

import com.tramchester.domain.Route;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.PathEvaluator;


public class RouteEvaluator implements Evaluator {
    private String route;

    public RouteEvaluator(String route) {
        this.route = route;
    }

    @Override
    public Evaluation evaluate(Path path) {
        String incomingRouteName = path.endNode().getProperty(GraphStaticKeys.RouteStation.ROUTE_NAME).toString();
        if (route.equals(incomingRouteName)) {
            return Evaluation.INCLUDE_AND_CONTINUE;
        } else {
            return Evaluation.EXCLUDE_AND_PRUNE;
        }
    }
}
