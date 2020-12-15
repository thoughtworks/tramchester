package com.tramchester.domain;

import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;

public class ConnectingStage implements TransportStage<Station, Station>  {
    private final Station start;
    private final Station end;
    private final int cost;
    private final TramTime walkStartTime;

    public ConnectingStage(Station start, Station end, int cost, TramTime walkStartTime) {
        this.start = start;
        this.end = end;
        this.cost = cost;
        this.walkStartTime = walkStartTime;
    }

    @Override
    public String getHeadSign() {
        return end.getName();
    }

    @Override
    public Route getRoute() {
        return Route.Walking;
    }

    @Override
    public Location<Station> getActionStation() {
        return start;
    }

    @Override
    public Station getLastStation() {
        return end;
    }

    @Override
    public Station getFirstStation() {
        return start;
    }

    @Override
    public TramTime getFirstDepartureTime() {
        return walkStartTime;
    }

    @Override
    public TramTime getExpectedArrivalTime() {
        return walkStartTime.plusMinutes(getDuration());
    }

    @Override
    public int getDuration() {
        return cost;
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
    public TransportMode getMode() {
        return TransportMode.Connect;
    }

    @Override
    public int getPassedStops() {
        return 0;
    }

    @Override
    public String toString() {
        return "ConnectingStage{" +
                "start=" + start.getName() +
                ", end=" + end.getName() +
                ", cost=" + cost +
                ", walkStartTime=" + walkStartTime +
                '}';
    }
}
