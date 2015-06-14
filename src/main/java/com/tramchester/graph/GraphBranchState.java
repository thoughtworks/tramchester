package com.tramchester.graph;

import com.tramchester.domain.DaysOfWeek;

public class GraphBranchState {
    private final String dest;
    private int time;
    private DaysOfWeek day;

    public GraphBranchState(int time, DaysOfWeek day, String dest) {
        this.time = time;
        this.day = day;
        this.dest = dest;
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
}
