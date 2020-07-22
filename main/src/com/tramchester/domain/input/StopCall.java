package com.tramchester.domain.input;

import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.GTFSPickupDropoffType;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ServiceTime;

public abstract class StopCall {
    protected final Station station;
    private final ServiceTime arrivalTime;
    protected final ServiceTime departureTime;
    private final int sequenceNumber;
    private final GTFSPickupDropoffType pickupType;
    private final GTFSPickupDropoffType dropoffType;

    public StopCall(Station station, StopTimeData stopTimeData) {
        this.station = station;
        this.arrivalTime = stopTimeData.getArrivalTime();
        this.departureTime = stopTimeData.getDepartureTime();
        this.sequenceNumber = stopTimeData.getStopSequence();
        this.pickupType = stopTimeData.getPickupType();
        this.dropoffType = stopTimeData.getDropOffType();
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

    public abstract IdFor<Platform> getPlatformId();

    public GTFSPickupDropoffType getPickupType() {
        return pickupType;
    }

    public GTFSPickupDropoffType getDropoffType() {
        return dropoffType;
    }

    @Override
    public String toString() {
        return "StopCall{" +
                "station=" + station +
                ", arrivalTime=" + arrivalTime +
                ", departureTime=" + departureTime +
                ", sequenceNumber=" + sequenceNumber +
                ", pickupType=" + pickupType +
                ", dropoffType=" + dropoffType +
                '}';
    }

    public abstract boolean hasPlatfrom();
}
