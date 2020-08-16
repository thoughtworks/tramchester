package com.tramchester.domain.time;

import java.util.Objects;

public class TimeWindow {
    private final TramTime queryTime;
    private final int withinMins;

    public TimeWindow(TramTime queryTime, int withinMins) {
        this.queryTime = queryTime;
        this.withinMins = withinMins;
    }

    public TramTime queryTime() {
        return queryTime;
    }

    public int withinMins() {
        return withinMins;
    }

    @Override
    public String toString() {
        return "TimeWindow{" +
                "queryTime=" + queryTime +
                ", withinMins=" + withinMins +
                '}';
    }

    public TimeWindow next(TramTime elapsedTime) {
        return new TimeWindow(elapsedTime, withinMins);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeWindow that = (TimeWindow) o;
        return withinMins == that.withinMins &&
                Objects.equals(queryTime, that.queryTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(queryTime, withinMins);
    }
}
