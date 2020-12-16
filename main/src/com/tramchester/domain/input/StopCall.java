package com.tramchester.domain.input;

import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.domain.time.TramTime;

public abstract class StopCall {
    protected final Station station;
    private final TramTime arrivalTime;
    private final TramTime departureTime;
    private final int sequenceNumber;
    private final GTFSPickupDropoffType pickupType;
    private final GTFSPickupDropoffType dropoffType;

    protected StopCall(Station station, StopTimeData stopTimeData) {
        this.station = station;
        this.arrivalTime = stopTimeData.getArrivalTime();
        this.departureTime = stopTimeData.getDepartureTime();
        this.sequenceNumber = stopTimeData.getStopSequence();
        this.pickupType = stopTimeData.getPickupType();
        this.dropoffType = stopTimeData.getDropOffType();
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

    public int getGetSequenceNumber() {
        return sequenceNumber;
    }

    public abstract Platform getPlatform();

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
