package com.tramchester.domain;


import java.time.LocalTime;

import static java.lang.String.format;

public class Stop extends TimeAsMinutes {
    private final Station station;
    private final LocalTime arrivalTime;
    private final LocalTime departureTime;

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

    public Station getStation() {
        return station;
    }

    @Override
    public String toString() {
        return "Stop{" +
                "station=" + station +
                ", arrivalTime=" + arrivalTime +
                ", departureTime=" + departureTime +
                '}';
    }

    public int getDepartureMinFromMidnight() {
        return getMinutes(departureTime);
    }

    public int getArriveMinsFromMidnight() {
        return getMinutes(arrivalTime);
    }
}
