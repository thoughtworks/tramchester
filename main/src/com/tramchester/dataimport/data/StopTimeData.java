package com.tramchester.dataimport.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
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

    // when dealing with millions of rows parsing of times became a bottleneck, so cache results
    private TramTime parsedArrivalTime = null;
    private TramTime parsedDepartureTime = null;

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

    // TODO Rework handling of types here
    public StopTimeData(IdFor<Trip> id, TramTime arrivalTime, TramTime departureTime, IdFor<Station> stopId, int seqNumber,
                        GTFSPickupDropoffType pickup, GTFSPickupDropoffType dropoff) {
        this(id.forDTO(), arrivalTime, departureTime, stopId.forDTO(), seqNumber, pickup, dropoff);
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
        if (parsedArrivalTime==null) {
            parsedArrivalTime = TramTime.parse(arrivalTime);
        }
        return parsedArrivalTime;
    }

    public TramTime getDepartureTime() {
        if (parsedDepartureTime==null) {
            parsedDepartureTime = TramTime.parse(departureTime);
        }
        return parsedDepartureTime;
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

    public boolean isValid() {
        return getArrivalTime().isValid() && getDepartureTime().isValid();
    }
}
