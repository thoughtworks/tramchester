package com.tramchester.domain;

import com.tramchester.domain.presentation.TransportStage;

import java.util.Optional;

public class WalkingStage implements TransportStage {
    private RawWalkingStage rawWalkingStage;
    private TramTime beginTime;

    public WalkingStage(RawWalkingStage rawWalkingStage, TramTime beginTimeMins) {
        this.rawWalkingStage = rawWalkingStage;
        this.beginTime = beginTimeMins;
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Walk;
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
    public Location getActionStation() {
        return rawWalkingStage.getDestination();
    }

    @Override
    public Location getLastStation() {
        return rawWalkingStage.getDestination();
    }

    @Override
    public Location getFirstStation() { return rawWalkingStage.getStart(); }

    public TramTime getFirstDepartureTime() {
        return beginTime;
    }

    public TramTime getExpectedArrivalTime() {
        return beginTime.plusMinutes(getDuration());
    }

    @Override
    public int getDuration() {
        return rawWalkingStage.getDuration();
    }

    @Override
    public String getDisplayClass() {
        return "RouteWalking";
    }

    @Override
    public Optional<Platform> getBoardingPlatform() {
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "WalkingStage{" +
                "destination=" + rawWalkingStage.getDestination() +
                ", start=" + rawWalkingStage.getStart() +
                ", cost=" + rawWalkingStage.getDuration() +
                ", beginTime=" + beginTime +
                '}';
    }

    @Override
    public int getPassedStops() {
        return 0;
    }
}
