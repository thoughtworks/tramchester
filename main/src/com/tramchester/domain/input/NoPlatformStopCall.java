package com.tramchester.domain.input;

import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Station;

public class NoPlatformStopCall extends StopCall {

    public NoPlatformStopCall(MutableTrip trip, Station station, StopTimeData stopTimeData) {
        super(station, stopTimeData, trip);
    }

    @Override
    public Platform getPlatform() {
        throw new RuntimeException(station + "  does not have platforms");
    }

    @Override
    public String toString() {
        return "NoPlatformStopCall{" +
                "} " + super.toString();
    }

    @Override
    public boolean hasPlatfrom() {
        return false;
    }
}
