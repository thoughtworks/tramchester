package com.tramchester.graph;

import com.tramchester.domain.TramTime;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.impl.util.WeightedPathImpl;
import org.neo4j.graphdb.Path;

import java.time.LocalTime;


public class PathBasedTimeProvider implements ElapsedTime {
    private final PersistsBoardingTime boardTime;
    private final TramTime queryTime;
    private int cost = -1;
    private final WeightedPathImpl weightedPath;

    public PathBasedTimeProvider(CostEvaluator<Double> costEvaluator, Path path, PersistsBoardingTime boardTime,
                                 TramTime queryTime) {
        weightedPath = new WeightedPathImpl(costEvaluator, path);
        this.boardTime = boardTime;
        this.queryTime = queryTime;
    }

    @Override
    public TramTime getElapsedTime() {
        int costOfJourney = getCost();
        if (boardTime.isPresent()) {
            return boardTime.get().plusMinutes(costOfJourney);
        }
        else {
            return queryTime.plusMinutes(costOfJourney);
        }
    }

    private int getCost() {
        if (cost==-1) {
            cost = (int) weightedPath.weight();
        }
        return cost;
    }

    @Override
    public boolean startNotSet() {
        return false;
    }

    @Override
    public void setJourneyStart(TramTime minutesPastMidnight) {
        boardTime.save(minutesPastMidnight);
    }

    public void reset() {

    }
}
