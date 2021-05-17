package com.tramchester.domain.transportStages;

import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;

public abstract class  WalkingStage<FROM extends Location<?>, DEST extends Location<?>> implements TransportStage<FROM, DEST> {
    private final FROM start;
    protected final DEST destination;
    private final int duration;
    private final TramTime beginTime;

    public WalkingStage(FROM start, DEST destination, int duration, TramTime beginTime) {
        this.start = start;
        this.destination = destination;
        this.duration = duration;
        this.beginTime = beginTime;
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Walk;
    }
    
    public int getDuration() {
        return duration;
    }

    public DEST getDestination() {
        return destination;
    }

    public abstract boolean getTowardsMyLocation();

    @Override
    public Route getRoute() {
        return Route.Walking;
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
        return beginTime.plusMinutes(getDuration());
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

        if (getDuration() != that.getDuration()) return false;
        if (!start.equals(that.start)) return false;
        if (!destination.equals(that.destination)) return false;
        return beginTime.equals(that.beginTime);
    }

    @Override
    public int hashCode() {
        int result = start.hashCode();
        result = 31 * result + destination.hashCode();
        result = 31 * result + duration;
        result = 31 * result + beginTime.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "WalkingStage{" +
                "start=" + start +
                ", destination=" + destination.getId() +
                ", duration=" + duration +
                ", beginTime=" + beginTime +
                '}';
    }


}
