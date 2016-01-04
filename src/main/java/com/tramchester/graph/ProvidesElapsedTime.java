package com.tramchester.graph;


import com.tramchester.domain.TramchesterException;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class ProvidesElapsedTime implements ElapsedTime {
    private static final Logger logger = LoggerFactory.getLogger(ProvidesElapsedTime.class);

    private int duration = -1;
    private Path path;
    private BranchState<GraphBranchState> branchState;
    private CostEvaluator<Double> costEvaluator;

    public ProvidesElapsedTime(Path path, BranchState<GraphBranchState> branchState, CostEvaluator<Double> costEvaluator) {
        this.path = path;
        this.branchState = branchState;
        this.costEvaluator = costEvaluator;
    }

    public int getElapsedTime() throws TramchesterException {
        int duration = getDuration();
        GraphBranchState state = getState();
        if (state.hasStartTime()) {
            return duration + state.getStartTime();
        }
        return duration + state.getQueriedTime();
    }

    private int getDuration() {
        if (duration<0) {
            duration = 0;
            for ( Relationship relationship : path.relationships() )
            {
                duration += costEvaluator.getCost( relationship, Direction.OUTGOING );
            }
            //duration = (int)new WeightedPathImpl(costEvaluator, path).weight();
        }
        return duration;
    }

    @Override
    public boolean startNotSet() {
        return !getState().hasStartTime();
    }

    @Override
    public void setJourneyStart(int minutesPastMidnight) {
        GraphBranchState state = getState();
        logger.info(format("Setting journey start to %s (queried time was %s)",
                minutesPastMidnight, state.getQueriedTime()));
        GraphBranchState newState = state.updateStartTime(minutesPastMidnight);
        branchState.setState(newState);
    }

    private GraphBranchState getState() {
        return branchState.getState();
    }
}
