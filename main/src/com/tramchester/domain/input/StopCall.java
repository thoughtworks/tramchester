package com.tramchester.domain.input;

import com.tramchester.domain.Platform;
import com.tramchester.domain.Station;
import com.tramchester.domain.time.TramTime;

public class StopCall {
    private final Station station;
    private final Platform callingPlatform;
    private final TramTime arrivalTime;
    private final TramTime departureTime;
    private final byte sequenceNumber;

    public StopCall(Platform callingPlatform, Station station, byte sequenceNumber, TramTime arrivalTime, TramTime departureTime) {
        this.callingPlatform = callingPlatform;
        this.sequenceNumber = sequenceNumber;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.station = station;
    }

    public TramTime getArrivalTime() {
        return arrivalTime;
    }

    public TramTime getDepartureTime() {
        return departureTime;
    }

    public Station getStation() {
        return station;
    }

    public String getPlatformId() {
        return callingPlatform.getId();
    }

    public int getGetSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public String toString() {
        return "Stop{" +
                "station=" + station +
                ", arrivalTime=" + arrivalTime +
                ", departureTime=" + departureTime +
                ", callingPlatform='" + callingPlatform + '\'' +
                '}';
    }

}
