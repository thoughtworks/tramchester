package com.tramchester.domain;

import com.tramchester.domain.presentation.TransportStage;

import java.util.Objects;
import java.util.Optional;

public class WalkingStage implements TransportStage {
    private final Location start;
    private final Location destination;
    private final int duration;
    private final TramTime beginTime;

    public WalkingStage(Location start, Location destination, int duration, TramTime beginTime) {
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

    public Location getStart() {
        return start;
    }

    public Location getDestination() {
        return destination;
    }

    @Override
    public String getHeadSign() {
        return "WalkingHeadSign";
    }

    @Override
    public String getRouteName() {
        return "Walk";
    }

    @Override
    public String getDisplayClass() {
        return "RouteWalking";
    }

    @Override
    public Location getActionStation() {
        return getDestination();
    }

    @Override
    public Location getLastStation() {
        return getDestination();
    }

    @Override
    public Location getFirstStation() { return getStart(); }

    @Override
    public TramTime getFirstDepartureTime() {
        return beginTime;
    }

    @Override
    public TramTime getExpectedArrivalTime() {
        return beginTime.plusMinutes(getDuration());
    }

    @Override
    public Optional<Platform> getBoardingPlatform() {
        return Optional.empty();
    }


    @Override
    public String toString() {
        return "WalkingStage{" +
                "start=" + start +
                ", destination=" + destination +
                ", duration=" + duration +
                '}';
    }

    @Override
    public int getPassedStops() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WalkingStage that = (WalkingStage) o;
        return duration == that.duration &&
                start.equals(that.start) &&
                destination.equals(that.destination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, destination, duration);
    }
}
