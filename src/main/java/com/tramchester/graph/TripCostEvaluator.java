package com.tramchester.graph;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

public class TripCostEvaluator implements CostEvaluator<Double> {

    @Override
    public Double getCost(Relationship relationship, Direction direction) {
        return Double.parseDouble(relationship.getProperty("cost").toString());
    }
}
