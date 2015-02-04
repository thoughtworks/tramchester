package com.tramchester.domain;


public class Stop {
    private final String arrivalTime;
    private final String departureTime;
    private final Station station;
    private final String stopSequence;
    private final StopType stopType;

    public Stop(String arrivalTime, String departureTime, Station station, String stopSequence, StopType stopType) {

        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.station = station;
        this.stopSequence = stopSequence;
        this.stopType = stopType;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public String getDepartureTime() {
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
}
