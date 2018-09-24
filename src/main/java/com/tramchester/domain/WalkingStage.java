package com.tramchester.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.mappers.TramTimeJsonSerializer;
import org.joda.time.LocalTime;

import java.io.IOException;
import java.util.Optional;

public class WalkingStage implements TransportStage {
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
    public String getSummary() {
        return "Walking";
    }

    @Override
    public String getPrompt()  {
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

    @JsonSerialize(using = TramTimeJsonSerializer.class)
    public TramTime getFirstDepartureTime() {
        try {
            return TramTime.fromMinutes(beginTimeMins);
        } catch (TramchesterException e) {
            throw new IllegalArgumentException("Unable to create TramTime from "+beginTimeMins,e);
        }
    }

    @JsonSerialize(using = TramTimeJsonSerializer.class)
    public TramTime getExpectedArrivalTime() {
        int minutes = beginTimeMins + getDuration();
        try {
            return TramTime.fromMinutes(minutes);
        } catch (TramchesterException e) {
            throw new IllegalArgumentException("Unable to create TramTime from "+minutes,e);
        }
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
                ", beginTime=" + beginTimeMins +
                '}';
    }
}
