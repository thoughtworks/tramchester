package com.tramchester.dataimport.data;

import org.joda.time.DateTime;

public class StopTimeData {
    private String tripId;
    private DateTime arrivalTime;
    private DateTime departureTime;
    private String stopId;
    private String stopSequence;
    private String pickupType;
    private String dropOffType;
    private int minutesFromMidnight;

    public StopTimeData(String tripId, DateTime arrivalTime, DateTime departureTime, String stopId,
                        String stopSequence, String pickupType, String dropOffType, int minutesFromMidnight) {

        this.tripId = tripId;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.stopId = stopId;
        this.stopSequence = stopSequence;
        this.pickupType = pickupType;
        this.dropOffType = dropOffType;
        this.minutesFromMidnight = minutesFromMidnight;
    }

    private StopTimeData() {
    }

    public String getTripId() {
        return tripId;
    }

    public DateTime getArrivalTime() {
        return arrivalTime;
    }

    public DateTime getDepartureTime() {
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

    public int getMinutesFromMidnight() {
        return minutesFromMidnight;
    }
}
