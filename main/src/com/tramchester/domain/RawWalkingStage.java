package com.tramchester.domain;

import java.util.Objects;

public class RawWalkingStage implements RawStage {
    private final Location start;
    private final Location destination;
    private final int duration;

    public RawWalkingStage(Location start, Location destination, int duration) {
        this.start = start;
        this.destination = destination;
        this.duration = duration;
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Walk;
    }

    @Override
    public boolean getIsAVehicle() {
        return false;
    }

    @Override
    public boolean isWalk() {
        return true;
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
    public String toString() {
        return "RawWalkingStage{" +
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
        RawWalkingStage that = (RawWalkingStage) o;
        return duration == that.duration &&
                start.equals(that.start) &&
                destination.equals(that.destination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, destination, duration);
    }
}
