package com.tramchester.graph;


import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.impl.util.WeightedPathImpl;
import org.neo4j.graphdb.Path;

public class ProvidesElapsedTime implements ElapsedTime {
    private Path path;
    private GraphBranchState branchState;
    private CostEvaluator<Double> costEvaluator;

    public ProvidesElapsedTime(Path path, GraphBranchState branchState, CostEvaluator<Double> costEvaluator) {
        this.path = path;
        this.branchState = branchState;
        this.costEvaluator = costEvaluator;
    }

    public int getElapsedTime() {
        int duration = (int)new WeightedPathImpl(costEvaluator, path).weight();
        int journeyStartTime = branchState.getTime();
        return duration + journeyStartTime;
    }
}
