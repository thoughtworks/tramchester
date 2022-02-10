package com.tramchester.domain.transportStages;

import com.tramchester.domain.MutableRoute;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class ConnectingStage<FROM extends Location<?>, DEST extends Location<?>> implements TransportStage<FROM, DEST>  {
    private final FROM start;
    private final DEST end;
    private final Duration cost;
    private final TramTime walkStartTime;

    public ConnectingStage(FROM start, DEST end, Duration cost, TramTime walkStartTime) {
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
    public Location<?> getActionStation() {
        return start;
    }

    @Override
    public DEST getLastStation() {
        return end;
    }

    @Override
    public FROM getFirstStation() {
        return start;
    }

    @Override
    public TramTime getFirstDepartureTime() {
        return walkStartTime;
    }

    @Override
    public TramTime getExpectedArrivalTime() {
        return walkStartTime.plus(getDuration());
    }

    @Override
    public Duration getDuration() {
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
