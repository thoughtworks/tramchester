package com.tramchester.domain.input;

import com.tramchester.domain.Platform;
import com.tramchester.domain.Station;
import com.tramchester.domain.time.TramTime;

public class TramStopCall extends StopCall {
    private final Platform callingPlatform;

    public TramStopCall(Platform callingPlatform, Station station, byte sequenceNumber, TramTime arrivalTime, TramTime departureTime) {
        super(station, arrivalTime, departureTime, sequenceNumber);
        this.callingPlatform = callingPlatform;
    }

    public String getPlatformId() {
        return callingPlatform.getId();
    }

    @Override
    public String toString() {
        return "Stop{" +
                "station=" + station.getId() +
                ", arrivalTime=" + arrivalTime +
                ", departureTime=" + departureTime +
                ", callingPlatform='" + callingPlatform.getId() + '\'' +
                '}';
    }

}
