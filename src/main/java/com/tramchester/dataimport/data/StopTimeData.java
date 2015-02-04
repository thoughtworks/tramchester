package com.tramchester.dataimport.data;

public class StopTimeData {
    private String tripId;
    private String arrivalTime;
    private String departureTime;
    private String stopId;
    private String stopSequence;
    private String pickupType;
    private String dropOffType;

    public StopTimeData(String tripId, String arrivalTime, String departureTime, String stopId, String stopSequence, String pickupType, String dropOffType) {

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

    public String getArrivalTime() {
        return arrivalTime;
    }

    public String getDepartureTime() {
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
