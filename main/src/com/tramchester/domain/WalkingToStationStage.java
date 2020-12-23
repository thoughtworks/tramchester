package com.tramchester.domain;

import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;

public class WalkingToStationStage extends WalkingStage<MyLocation, Station> {

    public WalkingToStationStage(MyLocation start, Station destination, int duration, TramTime beginTime) {
        super(start, destination, duration, beginTime);
    }

    @Override
    public boolean getTowardsMyLocation() {
        return false;
    }

    @Override
    public String getHeadSign() {
        return "My Location";
    }

    @Override
    public Location<?> getActionStation() {
        return getLastStation();
    }

    @Override
    public IdFor<Trip> getTripId() {
        return IdFor.invalid();
    }

    @Override
    public String toString() {
        return "WalkingToStationStage{} " + super.toString();
    }
}
