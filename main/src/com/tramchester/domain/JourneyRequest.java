package com.tramchester.domain;

import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public class JourneyRequest {
    private final TramServiceDate date;
    private final TramTime originalQueryTime;
    private final boolean arriveBy;
    private final int maxChanges;
    private final UUID uid;
    private final Duration maxJourneyDuration;
    private final long maxNumberOfJourneys;

    private final Set<TransportMode> requestedModes;

    private boolean diagnostics;
    private boolean warnIfNoResults;

    public JourneyRequest(LocalDate date, TramTime originalQueryTime, boolean arriveBy, int maxChanges, Duration maxJourneyDuration,
                          long maxNumberOfJourneys, Set<TransportMode> requestedModes) {
        this(new TramServiceDate(date), originalQueryTime, arriveBy, maxChanges, maxJourneyDuration, maxNumberOfJourneys, requestedModes);
    }

    public JourneyRequest(TramServiceDate date, TramTime originalQueryTime, boolean arriveBy, int maxChanges,
                          Duration maxJourneyDuration, long maxNumberOfJourneys, Set<TransportMode> requestedModes) {
        this.date = date;
        this.originalQueryTime = originalQueryTime;
        this.arriveBy = arriveBy;
        this.maxChanges = maxChanges;
        this.maxJourneyDuration = maxJourneyDuration;
        this.maxNumberOfJourneys = maxNumberOfJourneys;
        this.uid = UUID.randomUUID();
        this.requestedModes = requestedModes;

        diagnostics = false;
        warnIfNoResults = true;
    }

    public JourneyRequest(JourneyRequest originalRequest, TramTime computedDepartTime) {
        this(originalRequest.date, computedDepartTime, originalRequest.arriveBy, originalRequest.maxChanges,
                originalRequest.maxJourneyDuration, originalRequest.maxNumberOfJourneys, originalRequest.requestedModes);
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

        if (arriveBy != that.arriveBy) return false;
        if (maxChanges != that.maxChanges) return false;
        if (maxNumberOfJourneys != that.maxNumberOfJourneys) return false;
        if (!date.equals(that.date)) return false;
        if (!originalQueryTime.equals(that.originalQueryTime)) return false;
        return maxJourneyDuration.equals(that.maxJourneyDuration);
    }

    @Override
    public int hashCode() {
        int result = date.hashCode();
        result = 31 * result + originalQueryTime.hashCode();
        result = 31 * result + (arriveBy ? 1 : 0);
        result = 31 * result + maxChanges;
        result = 31 * result + maxJourneyDuration.hashCode();
        result = 31 * result + (int) (maxNumberOfJourneys ^ (maxNumberOfJourneys >>> 32));
        return result;
    }

    public boolean getDiagnosticsEnabled() {
        return diagnostics;
    }

    public JourneyRequest setDiag(boolean flag) {
        diagnostics = flag;
        return this;
    }

    public Duration getMaxJourneyDuration() {
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
                ", allowedModes=" + requestedModes +
                '}';
    }

    public Set<TransportMode> getRequestedModes() {
        return requestedModes;
    }

    public TimeRange getTimeRange() {
        return TimeRange.of(originalQueryTime, Duration.ZERO, maxJourneyDuration);
    }
}
