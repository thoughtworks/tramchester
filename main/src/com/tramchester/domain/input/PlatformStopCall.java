package com.tramchester.domain.input;

import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.MutablePlatform;
import com.tramchester.domain.places.Station;

public class PlatformStopCall extends StopCall {
    private final MutablePlatform callingPlatform;

    public PlatformStopCall(Trip trip, MutablePlatform platform, Station station, StopTimeData stopTimeData) {
        super(station, stopTimeData, trip);
        this.callingPlatform = platform;
    }

    public MutablePlatform getPlatform() {
        return callingPlatform;
    }

    @Override
    public String toString() {
        return "TramStopCall{" +
                "callingPlatform=" + callingPlatform +
                "} " + super.toString();
    }

    @Override
    public boolean hasPlatfrom() {
        return true;
    }
}
