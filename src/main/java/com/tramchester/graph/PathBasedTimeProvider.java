package com.tramchester.graph;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.impl.util.WeightedPathImpl;
import org.neo4j.graphdb.Path;

import java.time.LocalTime;


public class PathBasedTimeProvider implements ElapsedTime {
    private final PersistsBoardingTime boardTime;
    private final LocalTime queryTime;
    private final int cost;

    public PathBasedTimeProvider(CostEvaluator<Double> costEvaluator, Path path, PersistsBoardingTime boardTime,
                                 LocalTime queryTime) {
        WeightedPathImpl weightedPath = new WeightedPathImpl(costEvaluator, path);
        this.cost = (int) weightedPath.weight();
        this.boardTime = boardTime;
        this.queryTime = queryTime;
    }

    @Override
    public LocalTime getElapsedTime() {
        if (boardTime.isPresent()) {
            return boardTime.get().plusMinutes(cost);
        }
        else {
            return queryTime.plusMinutes(cost);
        }

    }

    @Override
    public boolean startNotSet() {
        return false;
    }

    @Override
    public void setJourneyStart(LocalTime minutesPastMidnight) {
        boardTime.save(minutesPastMidnight);
    }

    public void reset() {

    }
}
