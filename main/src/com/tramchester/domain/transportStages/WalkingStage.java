package com.tramchester.domain.transportStages;

import com.tramchester.domain.MutableRoute;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;

import java.time.Duration;

public abstract class  WalkingStage<FROM extends Location<?>, DEST extends Location<?>> implements TransportStage<FROM, DEST> {
    private final FROM start;
    protected final DEST destination;
    private final Duration duration;
    private final TramTime beginTime;

    public WalkingStage(FROM start, DEST destination, Duration duration, TramTime beginTime) {
        this.start = start;
        this.destination = destination;
        this.duration = duration;
        this.beginTime = beginTime;
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Walk;
    }
    
    public Duration getDuration() {
        return duration;
    }

    public abstract boolean getTowardsMyLocation();

    @Override
    public Route getRoute() {
        return MutableRoute.Walking;
    }

    @Override
    public DEST getLastStation() {
        return destination;
    }

    @Override
    public FROM getFirstStation() { return start; }

    @Override
    public TramTime getFirstDepartureTime() {
        return beginTime;
    }

    @Override
    public TramTime getExpectedArrivalTime() {
        return beginTime.plus(getDuration());
    }

    @Override
    public Platform getBoardingPlatform() {
        throw new RuntimeException("No platform");
    }

    @Override
    public boolean hasBoardingPlatform() {
        return false;
    }

    @Override
    public int getPassedStopsCount() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WalkingStage<?, ?> that = (WalkingStage<?, ?>) o;

        if (!start.equals(that.start)) return false;
        if (!destination.equals(that.destination)) return false;
        if (!duration.equals(that.duration)) return false;
        return beginTime.equals(that.beginTime);
    }

    @Override
    public int hashCode() {
        int result = start.hashCode();
        result = 31 * result + destination.hashCode();
        result = 31 * result + duration.hashCode();
        result = 31 * result + beginTime.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "WalkingStage{" +
                "start=" + start.getId() +
                ", destination=" + destination.getId() +
                ", duration=" + duration +
                ", beginTime=" + beginTime +
                '}';
    }


}
