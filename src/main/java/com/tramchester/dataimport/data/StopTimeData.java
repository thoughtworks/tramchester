package com.tramchester.dataimport.data;

import java.time.LocalTime;

public class StopTimeData {
    private String tripId;
    private LocalTime arrivalTime;
    private LocalTime departureTime;
    private String stopId;
    private String stopSequence;
    private String pickupType;
    private String dropOffType;

    public StopTimeData(String tripId, LocalTime arrivalTime, LocalTime departureTime, String stopId,
                        String stopSequence, String pickupType, String dropOffType) {
        this.tripId = tripId;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.stopId = stopId;
        this.stopSequence = stopSequence;
        this.pickupType = pickupType;
        this.dropOffType = dropOffType;
    }

    private StopTimeData() {
    }

    public String getTripId() {
        return tripId;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public String getStopId() {
        return stopId;
    }

    public String getStopSequence() {
        return stopSequence;
    }

    public String getPickupType() {
        return pickupType;
    }

    public String getDropOffType() {
        return dropOffType;
    }

}
