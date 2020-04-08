package com.tramchester.graph.search;

import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;

import java.util.Objects;

public class JourneyRequest {
    private final TramServiceDate date;
    private final TramTime time;
    private final boolean arriveBy;

    public JourneyRequest(TramServiceDate date, TramTime time) {
        this(date, time, false);
    }

    public JourneyRequest(TramServiceDate date, TramTime time, boolean arriveBy) {
        this.date = date;
        this.time = time;
        this.arriveBy = arriveBy;
    }

    public TramServiceDate getDate() {
        return date;
    }

    public TramTime getTime() {
        return time;
    }

    public boolean getArriveBy() {
        return arriveBy;
    }

    @Override
    public String toString() {
        return "JourneyRequest{" +
                "date=" + date +
                ", time=" + time +
                ", arriveBy=" + arriveBy +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JourneyRequest that = (JourneyRequest) o;
        return arriveBy == that.arriveBy &&
                date.equals(that.date) &&
                time.equals(that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, time, arriveBy);
    }
}
