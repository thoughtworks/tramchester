package com.tramchester.domain;

import java.time.LocalTime;
import java.util.Objects;

public class TimeWindow {
    private LocalTime queryTime;
    private int withinMins;

    public TimeWindow(LocalTime queryTime, int withinMins) {
        this.queryTime = queryTime;
        this.withinMins = withinMins;
    }

    public LocalTime queryTime() {
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

    public TimeWindow next(LocalTime elapsedTime) {
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
