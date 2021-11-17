package com.tramchester.domain.transportStages;

import com.tramchester.domain.MutablePlatform;
import com.tramchester.domain.MutableRoute;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;

import java.util.Collections;
import java.util.List;

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
        return MutableRoute.Walking;
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
    public MutablePlatform getBoardingPlatform() {
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
    public int getPassedStopsCount() {
        return 0;
    }

    @Override
    public List<StopCall> getCallingPoints() {
        return Collections.emptyList();
    }

    @Override
    public StringIdFor<Trip> getTripId() {
        return StringIdFor.invalid();
    }

    @Override
    public String toString() {
        return "ConnectingStage{" +
                "start=" + start.getId() +
                ", end=" + end.getId() +
                ", cost=" + cost +
                ", walkStartTime=" + walkStartTime +
                '}';
    }
}
