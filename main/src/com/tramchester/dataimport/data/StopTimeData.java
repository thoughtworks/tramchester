package com.tramchester.dataimport.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Platform;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.TramTime;
import com.tramchester.mappers.serialisation.TramTimeJsonDeserializer;

public class StopTimeData {

    @JsonProperty("trip_id")
    private String tripId;

    @JsonProperty("arrival_time")
    private String arrivalTime ;

    @JsonProperty("departure_time")
    private String departureTime;

    @JsonProperty("stop_id")
    private String stopId;
    @JsonProperty("stop_sequence")
    private int stopSequence;
    @JsonProperty("pickup_type")
    private GTFSPickupDropoffType pickupType;
    @JsonProperty("drop_off_type")
    private GTFSPickupDropoffType dropOffType;

    // supports testing only, TODO remove
    public StopTimeData(String tripId, TramTime arrivalTime, TramTime departureTime, String stopId,
                        int stopSequence, GTFSPickupDropoffType pickupType, GTFSPickupDropoffType dropOffType) {
        this.tripId = tripId;
        this.stopId = stopId;

        this.arrivalTime = arrivalTime.toPattern()+":00";
        this.departureTime = departureTime.toPattern()+":00";
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
        return TramTime.parse(arrivalTime).get();
    }

    public TramTime getDepartureTime() {
        return TramTime.parse(departureTime).get();
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
        return IdFor.createId(stopId);
    }
}
