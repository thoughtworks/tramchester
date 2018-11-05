package com.tramchester.graph;

import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.exceptions.TramchesterException;

import java.time.LocalTime;

public class GraphBranchState {
    private final LocalTime queryTime;
    private LocalTime startTime;
    private TramServiceDate queryDate;
    private boolean hasStartTime = false;
    private DaysOfWeek day;

    public GraphBranchState(TramServiceDate queryDate, LocalTime queryTime) {
        this.queryDate = queryDate;
        this.queryTime = queryTime;
        day = queryDate.getDay(); // cached as called many many times during search...
    }

    public GraphBranchState updateStartTime(LocalTime startTime) throws TramchesterException {
        return new GraphBranchState(queryDate, queryTime).setStartTime(startTime);
    }

    public GraphBranchState setStartTime(LocalTime startTime) {
        this.startTime = startTime;
        this.hasStartTime = true;
        return this;
    }

    // time user queried for
    public LocalTime getQueriedTime() {
        return queryTime;
    }

    // time we were actually able to start journey
    public LocalTime getStartTime() throws TramchesterException {
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

    @Override
    public String toString() {
        return "GraphBranchState{" +
                "queryTime=" + queryTime +
                ", startTime=" + startTime +
                ", queryDate=" + queryDate +
                ", hasStartTime=" + hasStartTime +
                ", day=" + day +
                '}';
    }
}
