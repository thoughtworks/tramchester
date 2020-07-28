package com.tramchester.domain;

import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramTime;

import java.util.Optional;

public class ConnectingStage implements TransportStage  {
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
    public String getRouteName() {
        return "Walk to";
    }

    @Override
    public String getRouteShortName() {
        return "Walk";
    }

    @Override
    public Location getActionStation() {
        return start;
    }

    @Override
    public Location getLastStation() {
        return end;
    }

    @Override
    public Location getFirstStation() {
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
    public Optional<Platform> getBoardingPlatform() {
        return Optional.empty();
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Connect;
    }

    @Override
    public int getPassedStops() {
        return 0;
    }
}
