package com.tramchester.domain;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.PresentableStage;
import com.tramchester.graph.Nodes.TramNode;

import java.time.LocalTime;

public class WalkingStage implements PresentableStage {
    private final Location destination;
    private final Location start;
    private int cost;
    private int beginTime;

    public WalkingStage(int cost, Location start, Location destination, int beginTime) {
        this.cost = cost;
        this.destination = destination;
        this.start = start;
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
    public Location getLastStation() {
        return destination;
    }

    @Override
    public Location getFirstStation() {
        if (start instanceof MyLocation) {
            return destination;
        }
        return start;
    }

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
