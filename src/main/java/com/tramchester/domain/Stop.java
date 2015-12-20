package com.tramchester.domain;


import java.time.LocalTime;

import static java.lang.String.format;

public class Stop {
    private final LocalTime arrivalTime;
    private final LocalTime departureTime;
    private final Station station;

    public Stop(Station station, LocalTime arrivalTime, LocalTime departureTime) {
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.station = station;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    private int getMinutes(LocalTime time) {
        int hour = time.getHour();
        int minute = time.getMinute();
        if(hour == 0){
            hour = 24;
        } else if(hour == 1){
            hour = 25;
        }
        return (hour * 60) + minute;
    }

    public Station getStation() {
        return station;
    }


    @Override
    public String toString() {
        return format("Stop arrivalTime=%s (%s) departureTime=%s (%s) station=%s",
                arrivalTime, getArriveMinsFromMidnight(),
                departureTime, getDepartureMinFromMidnight(), station);
    }

    public int getDepartureMinFromMidnight() {
        return getMinutes(departureTime);
    }

    public int getArriveMinsFromMidnight() {
        return getMinutes(arrivalTime);
    }
}
