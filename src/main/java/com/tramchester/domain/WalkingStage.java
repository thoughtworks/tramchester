package com.tramchester.domain;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.PresentableStage;
import com.tramchester.mappers.TimeJsonSerializer;

import java.time.LocalTime;

public class WalkingStage implements PresentableStage {
    private RawWalkingStage rawWalkingStage;
    private int beginTimeMins;

    public WalkingStage(RawWalkingStage rawWalkingStage, int beginTimeMins) {
        this.rawWalkingStage = rawWalkingStage;
        this.beginTimeMins = beginTimeMins;
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

    @Override
    public String getSummary() throws TramchesterException {
        return "Walking";
    }

    @Override
    public String getPrompt() throws TramchesterException {
        return "Walk to";
    }

    @Override
    public String getHeadSign() {
        return "WalkingHeadSign";
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

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getFirstDepartureTime() {
        return LocalTime.ofSecondOfDay(beginTimeMins*60);
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getExpectedArrivalTime() {
        return LocalTime.ofSecondOfDay((beginTimeMins+getDuration())*60);
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
                ", beginTime=" + beginTimeMins +
                '}';
    }
}
