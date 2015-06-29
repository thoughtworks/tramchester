package com.tramchester.graph;

import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.TramServiceDate;

public class GraphBranchState {
    private final String dest;
    private int time;
    private DaysOfWeek day;
    private TramServiceDate queryDate;

    public GraphBranchState(int time, DaysOfWeek day, String dest, TramServiceDate queryDate) {
        this.time = time;
        this.day = day;
        this.dest = dest;
        this.queryDate = queryDate;
    }

    public int getTime() {
        return time;
    }

    public DaysOfWeek getDay() {
        return day;
    }

    public String getDest() {
        return dest;
    }

    public TramServiceDate getQueryDate() {
        return queryDate;
    }
}
