package com.tramchester.domain;

public class RouteStation {
    // A station that serves a specific route

    private final Station station;
    private final Route route;

    public RouteStation(Station station, Route route) {
        this.station = station;
        this.route = route;
    }

    public String getStationId() {
        return station.getId();
    }

    public String getRouteId() {
        return route.getId();
    }

    public String getId() {
        return formId(station, route);
    }

    public String getAgency() {
        return route.getAgency();
    }

    public boolean isTram() {
        return route.isTram();
    }

    @Override
    public String toString() {
        return "RouteStation{" +
                "station=" + station +
                ", route=" + route +
                '}';
    }

    public Route getRoute() {
        return route;
    }

    public static String formId(Location station, Route route) {
        return station.getId() + route.getId();
    }

}
