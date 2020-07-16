package com.tramchester.domain.input;

import com.tramchester.domain.HasId;
import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.domain.time.TramTime;

public class TramStopCall extends StopCall {
    private final Platform callingPlatform;

    public TramStopCall(Platform callingPlatform, Station station, byte sequenceNumber, ServiceTime arrivalTime, ServiceTime departureTime) {
        super(station, arrivalTime, departureTime, sequenceNumber);
        this.callingPlatform = callingPlatform;
    }

    public String getPlatformId() {
        return callingPlatform.getId();
    }

    @Override
    public String toString() {
        return "TramStopCall{" +
                "callingPlatform=" + HasId.asId(callingPlatform) +
                ", station=" + HasId.asId(station) +
                ", arrivalTime=" + arrivalTime +
                ", departureTime=" + departureTime +
                ", sequenceNumber=" + sequenceNumber +
                '}';
    }
}
