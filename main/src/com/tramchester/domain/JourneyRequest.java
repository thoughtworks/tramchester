package com.tramchester.domain;

import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;

import java.time.LocalDate;
import java.util.UUID;

public class JourneyRequest {
    private final TramServiceDate date;
    private final TramTime originalQueryTime;
    private final boolean arriveBy;
    private final int maxChanges;
    private final UUID uid;
    private final int maxJourneyDuration;
    private final long maxNumberOfJourneys;

    private boolean diagnostics;
    private boolean warnIfNoResults;

    public JourneyRequest(LocalDate date, TramTime originalQueryTime, boolean arriveBy, int maxChanges, int maxJourneyDuration,
                          long maxNumberOfJourneys) {
        this(new TramServiceDate(date), originalQueryTime, arriveBy, maxChanges, maxJourneyDuration, maxNumberOfJourneys);
    }

    public JourneyRequest(TramServiceDate date, TramTime originalQueryTime, boolean arriveBy, int maxChanges,
                          int maxJourneyDuration, long maxNumberOfJourneys) {
        this.date = date;
        this.originalQueryTime = originalQueryTime;
        this.arriveBy = arriveBy;
        this.maxChanges = maxChanges;
        this.maxJourneyDuration = maxJourneyDuration;
        this.maxNumberOfJourneys = maxNumberOfJourneys;
        this.uid = UUID.randomUUID();
        
        diagnostics = false;
        warnIfNoResults = true;
    }

    public JourneyRequest(JourneyRequest originalRequest, TramTime computedDepartTime) {
        this(originalRequest.date, computedDepartTime, originalRequest.arriveBy, originalRequest.maxChanges,
                originalRequest.maxJourneyDuration, originalRequest.maxNumberOfJourneys);
        diagnostics = originalRequest.diagnostics;
        warnIfNoResults = originalRequest.warnIfNoResults;
    }

    public TramServiceDate getDate() {
        return date;
    }

    public TramTime getOriginalTime() {
        return originalQueryTime;
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
        return getOriginalTime().equals(that.getOriginalTime());
    }

    @Override
    public int hashCode() {
        int result = getDate().hashCode();
        result = 31 * result + getOriginalTime().hashCode();
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

    // TODO Return duration
    @Deprecated
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
                ", originalQueryTime=" + originalQueryTime +
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
