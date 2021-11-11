package com.tramchester.dataimport.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.time.TramTime;

public class StopTimeData {

    // trip_id,arrival_time,departure_time,stop_id,stop_sequence,pickup_type,drop_off_type

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
    private String pickupType;
    @JsonProperty("drop_off_type")
    private String dropOffType;

    private StopTimeData(String tripId, TramTime arrivalTime, TramTime departureTime, String stopId,
                        int stopSequence, GTFSPickupDropoffType pickupType, GTFSPickupDropoffType dropOffType) {
        this.tripId = tripId;
        this.stopId = stopId;

        this.arrivalTime = arrivalTime.toPattern()+":00";
        this.departureTime = departureTime.toPattern()+":00";
        this.stopSequence = stopSequence;
        this.pickupType = pickupType.getText();
        this.dropOffType = dropOffType.getText();
    }

    public static StopTimeData forTestOnly(String tripId, TramTime arrivalTime, TramTime departureTime, String stopId,
                                           int stopSequence, GTFSPickupDropoffType pickupType, GTFSPickupDropoffType dropOffType)
    {
        return new StopTimeData(tripId, arrivalTime, departureTime,stopId, stopSequence, pickupType, dropOffType);
    }

    // for CSV parse via jackson
    public StopTimeData() {
        // deserialisation
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
        return StringIdFor.createId(tripId);
    }

    public TramTime getArrivalTime() {
        // TODO or else
        return TramTime.parse(arrivalTime).get();
    }

    public TramTime getDepartureTime() {
        // TODO or else
        return TramTime.parse(departureTime).get();
    }

    public String getStopId() {
        return stopId;
    }

    public int getStopSequence() {
        return stopSequence;
    }

    public GTFSPickupDropoffType getPickupType() {
        return GTFSPickupDropoffType.fromString(pickupType);
    }

    public GTFSPickupDropoffType getDropOffType() {
        return GTFSPickupDropoffType.fromString(dropOffType);
    }

    public IdFor<Platform> getPlatformId() {
        return StringIdFor.createId(stopId);
    }

    public boolean arriveDepartSameTime() {
        return arrivalTime.equals(departureTime);
    }
}
