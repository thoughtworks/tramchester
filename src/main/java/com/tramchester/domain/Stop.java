package com.tramchester.domain;

import org.joda.time.LocalTime;

public class Stop extends TimeAsMinutes {
    private final Location station;
    private final LocalTime arrivalTime;
    private final LocalTime departureTime;
    private String stopId;
    private String routeId;
    private String serviceId;

    public Stop(String stopId, Location station, LocalTime arrivalTime, LocalTime departureTime, String routeId, String serviceId) {
        this.stopId = stopId;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.station = station;
        this.routeId = routeId;
        this.serviceId = serviceId;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public Location getStation() {
        return station;
    }

    @Override
    public String toString() {
        return "Stop{" +
                "station=" + station +
                ", arrivalTime=" + arrivalTime +
                ", departureTime=" + departureTime +
                ", stopId='" + stopId + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", routeId='" + routeId + '\'' +
                '}';
    }

    public int getDepartureMinFromMidnight() {
        return getMinutes(departureTime);
    }

    public int getArriveMinsFromMidnight() {
        return getMinutes(arrivalTime);
    }

    public String getId() {
        return stopId;
    }
}
