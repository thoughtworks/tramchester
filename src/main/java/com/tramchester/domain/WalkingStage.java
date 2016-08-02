package com.tramchester.domain;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.PresentableStage;

import java.time.LocalTime;

public class WalkingStage implements PresentableStage {
    RawWalkingStage rawWalkingStage;
    private int beginTime;

    public WalkingStage(RawWalkingStage rawWalkingStage, int beginTime) {
        this.rawWalkingStage = rawWalkingStage;
        this.beginTime = beginTime;
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
    public String getSummary() throws TramchesterException {
        return "Walking";
    }

    @Override
    public String getPrompt() throws TramchesterException {
        return "Walk to";
    }

    @Override
    public int getNumberOfServiceTimes() {
        return 1;
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

    @Override
    public LocalTime getFirstDepartureTime() {
        return LocalTime.ofSecondOfDay(beginTime*60);
    }

    @Override
    public LocalTime getExpectedArrivalTime() {
        return LocalTime.ofSecondOfDay((beginTime+rawWalkingStage.getDuration())*60);
    }

    @Override
    public int getDuration() {
        return rawWalkingStage.getDuration();
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
}
