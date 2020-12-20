package com.tramchester.domain.input;

import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;

public class NoPlatformStopCall extends StopCall {
    private final TransportMode mode;

    public NoPlatformStopCall(Station station, StopTimeData stopTimeData, TransportMode mode) {
        super(station, stopTimeData);
        this.mode = mode;
    }

    @Override
    public Platform getPlatform() {
        throw new RuntimeException(mode + "  don't have platforms");
    }

    @Override
    public String toString() {
        return "NoPlatformStopCall{" +
                "mode=" + mode +
                "} " + super.toString();
    }

    @Override
    public boolean hasPlatfrom() {
        return false;
    }
}
