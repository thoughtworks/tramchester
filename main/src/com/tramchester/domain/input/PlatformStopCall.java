package com.tramchester.domain.input;

import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Station;

public class PlatformStopCall extends StopCall {
    private final Platform callingPlatform;

    public PlatformStopCall(Trip trip, Platform platform, Station station, StopTimeData stopTimeData) {
        super(station, stopTimeData, trip);
        this.callingPlatform = platform;
    }

    public Platform getPlatform() {
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
