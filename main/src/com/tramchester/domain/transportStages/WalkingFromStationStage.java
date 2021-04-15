package com.tramchester.domain.transportStages;

import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.WalkingStage;

import java.util.Collections;
import java.util.List;

public class WalkingFromStationStage extends WalkingStage<Station, MyLocation> {
    public WalkingFromStationStage(Station start, MyLocation destination, int duration, TramTime beginTime) {
        super(start, destination, duration, beginTime);
    }

    @Override
    public boolean getTowardsMyLocation() {
        return true;
    }

    @Override
    public String getHeadSign() {
        return destination.getName();
    }

    @Override
    public Location<?> getActionStation() {
        return getFirstStation();
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
        return "WalkingFromStationStage{} " + super.toString();
    }
}
