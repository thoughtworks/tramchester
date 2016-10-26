package com.tramchester.graph;

import com.tramchester.domain.exceptions.TramchesterException;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.impl.util.WeightedPathImpl;
import org.neo4j.graphdb.Path;


public class PathBasedTimeProvider implements ElapsedTime {
    private final PersistsBoardingTime boardTime;
    private final int queryTime;
    private final int cost;

    public PathBasedTimeProvider(CostEvaluator<Double> costEvaluator, Path path, PersistsBoardingTime boardTime,
                                 int queryTime) {
        WeightedPathImpl weightedPath = new WeightedPathImpl(costEvaluator, path);
        this.cost = (int) weightedPath.weight();
        this.boardTime = boardTime;
        this.queryTime = queryTime;
    }

    @Override
    public int getElapsedTime() throws TramchesterException {
        if (boardTime.isPresent()) {
            return boardTime.get() + cost;
        }
        else {
            return queryTime + cost;
        }

    }

    @Override
    public boolean startNotSet() {
        return false;
    }

    @Override
    public void setJourneyStart(int minutesPastMidnight) throws TramchesterException {
        boardTime.save(minutesPastMidnight);
    }

    public void reset() {

    }
}
