package com.tramchester.graph;

import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.exceptions.TramchesterException;

public class GraphBranchState {
    private final int queryTime;
    private int startTime;
    private DaysOfWeek day;
    private TramServiceDate queryDate;
    private boolean hasStartTime = false;

    public GraphBranchState(DaysOfWeek day, TramServiceDate queryDate, int queryTime) {
        this.day = day;
        this.queryDate = queryDate;
        this.queryTime = queryTime;
    }

    public GraphBranchState updateStartTime(int startTime) {
        return new GraphBranchState(day, queryDate, queryTime).setStartTime(startTime);
    }

    private GraphBranchState setStartTime(int startTime) {
        this.startTime = startTime;
        this.hasStartTime = true;
        return this;
    }

    // time user queried for
    public int getQueriedTime() {
        return queryTime;
    }

    // time we were actually able to start journey
    public int getStartTime() throws TramchesterException {
        if (!hasStartTime) {
            throw new TramchesterException("start time not set");
        }
        return startTime;
    }

    public DaysOfWeek getDay() {
        return day;
    }

    public TramServiceDate getQueryDate() {
        return queryDate;
    }

    public boolean hasStartTime() {
        return hasStartTime;
    }
}
