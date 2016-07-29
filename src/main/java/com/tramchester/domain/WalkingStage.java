package com.tramchester.domain;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.PresentableStage;

import java.time.LocalTime;

public class WalkingStage implements PresentableStage {
    private final Location start;
    private final Location destination;
    private int cost;
    private int beginTime;

//    public WalkingStage(Location start, Location destination, int beginTime, int cost) {
//        this.cost = cost;
//        this.destination = destination;
//        this.start = start;
//        this.beginTime = beginTime;
//    }

    public WalkingStage(RawWalkingStage stage, int beginTime) {
        this.cost = stage.getDuration();
        this.start = stage.getStart();
        this.destination = stage.getDestination();
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
        return destination;
    }

    @Override
    public Location getLastStation() {
        return destination;
    }

    @Override
    public Location getFirstStation() { return start; }

    @Override
    public LocalTime getFirstDepartureTime() {
        return LocalTime.ofSecondOfDay(beginTime*60);
    }

    @Override
    public LocalTime getExpectedArrivalTime() {
        return LocalTime.ofSecondOfDay((beginTime+cost)*60);
    }

    @Override
    public int getDuration() {
        return cost;
    }

    @Override
    public String toString() {
        return "WalkingStage{" +
                "destination=" + destination +
                ", start=" + start +
                ", cost=" + cost +
                ", beginTime=" + beginTime +
                '}';
    }
}
