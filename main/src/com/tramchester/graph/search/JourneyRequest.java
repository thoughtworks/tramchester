package com.tramchester.graph.search;

import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;

import java.time.LocalDate;
import java.util.UUID;

public class JourneyRequest {
    private final TramServiceDate date;
    private final TramTime time;
    private final boolean arriveBy;
    private final int maxChanges;
    private final UUID uid;
    private final int maxJourneyDuration;
    private final long maxNumberOfJourneys;

    private boolean diagnostics;
    private boolean warnIfNoResults;

    public JourneyRequest(LocalDate date, TramTime time, boolean arriveBy, int maxChanges, int maxJourneyDuration,
                          long maxNumberOfJourneys) {
        this(new TramServiceDate(date), time, arriveBy, maxChanges, maxJourneyDuration, maxNumberOfJourneys);
    }

    public JourneyRequest(TramServiceDate date, TramTime time, boolean arriveBy, int maxChanges,
                          int maxJourneyDuration, long maxNumberOfJourneys) {
        this.date = date;
        this.time = time;
        this.arriveBy = arriveBy;
        this.maxChanges = maxChanges;
        this.maxJourneyDuration = maxJourneyDuration;
        this.maxNumberOfJourneys = maxNumberOfJourneys;
        this.uid = UUID.randomUUID();
        
        diagnostics = false;
        warnIfNoResults = true;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JourneyRequest that = (JourneyRequest) o;

        if (getArriveBy() != that.getArriveBy()) return false;
        if (getMaxChanges() != that.getMaxChanges()) return false;
        if (getMaxJourneyDuration() != that.getMaxJourneyDuration()) return false;
        if (!getDate().equals(that.getDate())) return false;
        return getTime().equals(that.getTime());
    }

    @Override
    public int hashCode() {
        int result = getDate().hashCode();
        result = 31 * result + getTime().hashCode();
        result = 31 * result + (getArriveBy() ? 1 : 0);
        result = 31 * result + getMaxChanges();
        result = 31 * result + getMaxJourneyDuration();
        return result;
    }

    public boolean getDiagnosticsEnabled() {
        return diagnostics;
    }

    public JourneyRequest setDiag(boolean flag) {
        diagnostics = flag;
        return this;
    }

    public int getMaxJourneyDuration() {
        return maxJourneyDuration;
    }

    public boolean getWarnIfNoResults() {
        return warnIfNoResults;
    }

    public void setWarnIfNoResults(boolean flag) {
        warnIfNoResults = flag;
    }

    public UUID getUid() {
        return uid;
    }

    public long getMaxNumberOfJourneys() {
        return maxNumberOfJourneys;
    }

    @Override
    public String toString() {
        return "JourneyRequest{" +
                "date=" + date +
                ", time=" + time +
                ", arriveBy=" + arriveBy +
                ", maxChanges=" + maxChanges +
                ", uid=" + uid +
                ", maxJourneyDuration=" + maxJourneyDuration +
                ", maxNumberOfJourneys=" + maxNumberOfJourneys +
                ", diagnostics=" + diagnostics +
                ", warnIfNoResults=" + warnIfNoResults +
                '}';
    }
}
