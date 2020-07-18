package com.tramchester.domain.input;

import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.HasId;
import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.domain.time.TramTime;

public class TramStopCall extends StopCall {
    private final Platform callingPlatform;

    public TramStopCall(Platform platform, Station station, StopTimeData stopTimeData) {
        super(station, stopTimeData);
        this.callingPlatform = platform;
    }

    public String getPlatformId() {
        return callingPlatform.getId();
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
