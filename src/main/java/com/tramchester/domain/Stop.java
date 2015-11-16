package com.tramchester.domain;


import com.tramchester.services.DateTimeService;

import java.time.LocalTime;

public class Stop {
    private final LocalTime arrivalTime;
    private final LocalTime departureTime;
    private final Station station;

    private int minutesFromMidnight;

    public Stop(LocalTime arrivalTime, LocalTime departureTime, Station station, int minutesFromMidnight) {
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.station = station;
        this.minutesFromMidnight = minutesFromMidnight;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public Station getStation() {
        return station;
    }

    public int getMinutesFromMidnight() {
        return minutesFromMidnight;
    }


    @Override
    public String toString() {
        return "Stop{" +
                "arrivalTime=" + DateTimeService.formatTime(arrivalTime) +
                ", departureTime=" + DateTimeService.formatTime(departureTime) +
                ", station=" + station +
                ", minutesFromMidnight=" + minutesFromMidnight +
                '}';
    }
}
