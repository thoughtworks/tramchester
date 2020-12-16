package com.tramchester.dataimport.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Platform;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.domain.time.TramTime;

public class StopTimeData {

    @JsonProperty("trip_id")
    private String tripId;
    @JsonProperty("arrival_time")
    private TramTime arrivalTime ;
    @JsonProperty("departure_time")
    private TramTime departureTime;

    @JsonProperty("stop_id")
    private String stopId;
    @JsonProperty("stop_sequence")
    private int stopSequence;
    private GTFSPickupDropoffType pickupType;
    private GTFSPickupDropoffType dropOffType;
    private String platformId;

    public StopTimeData(String tripId, TramTime arrivalTime, TramTime departureTime, String stopId,
                        int stopSequence, GTFSPickupDropoffType pickupType, GTFSPickupDropoffType dropOffType) {
        this.tripId = tripId;
        this.platformId = stopId;
        this.stopId = stopId;

        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.stopSequence = stopSequence;
        this.pickupType = pickupType;
        this.dropOffType = dropOffType;
    }

    // for CSV parse via jackson
    public StopTimeData() {

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
        return IdFor.createId(tripId);
    }

    public TramTime getArrivalTime() {
        return arrivalTime;
    }

    public TramTime getDepartureTime() {
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
        return IdFor.createId(platformId);
    }
}
