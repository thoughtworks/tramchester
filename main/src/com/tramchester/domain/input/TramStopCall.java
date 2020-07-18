package com.tramchester.domain.input;

import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.HasId;
import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.domain.time.TramTime;

public class TramStopCall extends StopCall {
    private final Platform callingPlatform;

    @Deprecated
    public TramStopCall(Platform callingPlatform, Station station, int sequenceNumber, ServiceTime arrivalTime, ServiceTime departureTime) {
        super(station, arrivalTime, departureTime, sequenceNumber);
        this.callingPlatform = callingPlatform;
    }

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
                "callingPlatform=" + HasId.asId(callingPlatform) +
                ", station=" + HasId.asId(station) +
                ", arrivalTime=" + arrivalTime +
                ", departureTime=" + departureTime +
                ", sequenceNumber=" + sequenceNumber +
                '}';
    }
}
