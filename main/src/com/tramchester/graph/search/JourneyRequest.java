package com.tramchester.graph.search;

import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;

import java.util.Objects;
import java.util.UUID;

public class JourneyRequest {
    private final TramServiceDate date;
    private final TramTime time;
    private final boolean arriveBy;
    private final int maxChanges;
    private final UUID uid;
    private boolean diagnostics;

    public JourneyRequest(TramServiceDate date, TramTime time, boolean arriveBy) {
        this(date, time, arriveBy, Integer.MAX_VALUE);
    }

    public JourneyRequest(TramServiceDate date, TramTime time, boolean arriveBy, int maxChanges) {
        this.date = date;
        this.time = time;
        this.arriveBy = arriveBy;
        this.maxChanges = maxChanges;
        this.uid = UUID.randomUUID();
        diagnostics = false;
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

    public int getMaxChanges() {
        return maxChanges;
    }

    @Override
    public String toString() {
        return "JourneyRequest{" +
                "uuid=" + uid.toString() +
                " date=" + date +
                ", time=" + time +
                ", arriveBy=" + arriveBy +
                ", maxChanges=" + maxChanges +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JourneyRequest that = (JourneyRequest) o;
        return arriveBy == that.arriveBy &&
                maxChanges == that.maxChanges &&
                date.equals(that.date) &&
                time.equals(that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, time, arriveBy, maxChanges);
    }

    public boolean getDiagnosticsEnabled() {
        return diagnostics;
    }

    public JourneyRequest setDiag(boolean flag) {
        diagnostics = flag;
        return this;
    }
}
