package com.tramchester.graph;

import com.tramchester.domain.DaysOfWeek;

public class GraphBranchState {
    private int time;
    private DaysOfWeek day;

    public GraphBranchState(int time, DaysOfWeek day) {
        this.time = time;
        this.day = day;
    }

    public int getTime() {
        return time;
    }

    public DaysOfWeek getDay() {
        return day;
    }
}
