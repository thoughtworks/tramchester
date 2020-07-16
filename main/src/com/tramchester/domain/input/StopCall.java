package com.tramchester.domain.input;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.domain.time.TramTime;

public abstract class StopCall {
    protected final Station station;
    protected final ServiceTime arrivalTime;
    protected final ServiceTime departureTime;
    protected final byte sequenceNumber;

    public StopCall(Station station, ServiceTime arrivalTime, ServiceTime departureTime, byte sequenceNumber) {
        this.station = station;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.sequenceNumber = sequenceNumber;
    }

    public ServiceTime getArrivalTime() {
        return arrivalTime;
    }

    public ServiceTime getDepartureTime() {
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
