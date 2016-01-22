package com.tramchester.domain;

public class TimeWindow {
    private int minsFromMidnight;
    private int withinMins;

    public TimeWindow(int minsFromMidnight, int withinMins) {
        this.minsFromMidnight = minsFromMidnight;
        this.withinMins = withinMins;
    }

    public int minsFromMidnight() {
        return minsFromMidnight;
    }

    public int withinMins() {
        return withinMins;
    }

    @Override
    public String toString() {
        return "TimeWindow{" +
                "minsFromMidnight=" + minsFromMidnight +
                ", withinMins=" + withinMins +
                '}';
    }

    public TimeWindow next(int elapsedTime) {
        return new TimeWindow(elapsedTime, withinMins);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TimeWindow that = (TimeWindow) o;

        if (minsFromMidnight != that.minsFromMidnight) return false;
        return withinMins == that.withinMins;

    }

    @Override
    public int hashCode() {
        int result = minsFromMidnight;
        result = 31 * result + withinMins;
        return result;
    }
}
