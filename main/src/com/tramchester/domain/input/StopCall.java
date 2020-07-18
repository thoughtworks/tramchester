package com.tramchester.domain.input;

import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ServiceTime;

public abstract class StopCall {
    protected final Station station;
    protected final ServiceTime arrivalTime;
    protected final ServiceTime departureTime;
    protected final int sequenceNumber;

    public StopCall(Station station, StopTimeData stopTimeData) {
        this.station = station;
        this.arrivalTime = stopTimeData.getArrivalTime();
        this.departureTime = stopTimeData.getDepartureTime();
        this.sequenceNumber = stopTimeData.getStopSequence();
    }

    @Deprecated
    public StopCall(Station station, ServiceTime arrivalTime, ServiceTime departureTime, int sequenceNumber) {
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

    public int getGetSequenceNumber() {
        return sequenceNumber;
    }

    public abstract String getPlatformId();

}
