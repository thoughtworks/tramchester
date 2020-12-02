package com.tramchester.dataimport.data;

import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Platform;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.ServiceTime;

public class StopTimeData {
    private final IdFor<Trip> tripId;
    private final ServiceTime arrivalTime ;
    private final ServiceTime departureTime;
    private final String stopId;
    private final int stopSequence;
    private final GTFSPickupDropoffType pickupType;
    private final GTFSPickupDropoffType dropOffType;
    private final IdFor<Platform> platformId;

    public StopTimeData(String tripId, ServiceTime arrivalTime, ServiceTime departureTime, String stopId,
                        int stopSequence, GTFSPickupDropoffType pickupType, GTFSPickupDropoffType dropOffType) {
        this.tripId = IdFor.createId(tripId);
        this.stopId = stopId;
        this.platformId = IdFor.createId(stopId);

        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.stopSequence = stopSequence;
        this.pickupType = pickupType;
        this.dropOffType = dropOffType;
    }

    @Override
    public String toString() {
        return "StopTimeData{" +
                "tripId='" + tripId + '\'' +
                ", arrivalTime=" + arrivalTime +
                ", departureTime=" + departureTime +
                ", stopId='" + stopId + '\'' +
                ", stopSequence='" + stopSequence + '\'' +
                ", pickupType='" + pickupType + '\'' +
                ", dropOffType='" + dropOffType + '\'' +
                '}';
    }

    public IdFor<Trip> getTripId() {
        return tripId;
    }

    public ServiceTime getArrivalTime() {
        return arrivalTime;
    }

    public ServiceTime getDepartureTime() {
        return departureTime;
    }

    public String getStopId() {
        return stopId;
    }

    public int getStopSequence() {
        return stopSequence;
    }

    public GTFSPickupDropoffType getPickupType() {
        return pickupType;
    }

    public GTFSPickupDropoffType getDropOffType() {
        return dropOffType;
    }

    public IdFor<Platform> getPlatformId() {
        return platformId;
    }
}
