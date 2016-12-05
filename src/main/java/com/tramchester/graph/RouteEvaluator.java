package com.tramchester.graph;

import com.tramchester.domain.Route;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.PathEvaluator;


public class RouteEvaluator implements Evaluator {
    public RouteEvaluator(Route route) {
    }

    @Override
    public Evaluation evaluate(Path path) {
        return Evaluation.INCLUDE_AND_CONTINUE;
    }
}
