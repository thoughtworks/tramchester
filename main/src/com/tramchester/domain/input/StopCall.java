package com.tramchester.domain.input;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;

public abstract class StopCall {
    protected final Station station;
    protected final TramTime arrivalTime;
    protected final TramTime departureTime;
    protected final byte sequenceNumber;

    public StopCall(Station station, TramTime arrivalTime, TramTime departureTime, byte sequenceNumber) {
        this.station = station;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.sequenceNumber = sequenceNumber;
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

    public byte getGetSequenceNumber() {
        return sequenceNumber;
    }

    public abstract String getPlatformId();

}
