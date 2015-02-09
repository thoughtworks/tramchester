package com.tramchester.domain;


import org.joda.time.DateTime;

public class Stop {
    private final DateTime arrivalTime;
    private final DateTime departureTime;
    private final Station station;
    private final String stopSequence;
    private final StopType stopType;
    private int minutesFromMidnight;

    public Stop(DateTime arrivalTime, DateTime departureTime, Station station, String stopSequence, StopType stopType, int minutesFromMidnight) {

        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.station = station;
        this.stopSequence = stopSequence;
        this.stopType = stopType;
        this.minutesFromMidnight = minutesFromMidnight;
    }

    public DateTime getArrivalTime() {
        return arrivalTime;
    }

    public DateTime getDepartureTime() {
        return departureTime;
    }

    public Station getStation() {
        return station;
    }

    public String getStopSequence() {
        return stopSequence;
    }

    public StopType getStopType() {
        return stopType;
    }

    public int getMinutesFromMidnight() {
        return minutesFromMidnight;
    }
}
