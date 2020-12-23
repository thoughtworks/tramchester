package com.tramchester.domain;

import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;

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
    public IdFor<Trip> getTripId() {
        return IdFor.invalid();
    }

    @Override
    public String toString() {
        return "WalkingFromStationStage{} " + super.toString();
    }
}
