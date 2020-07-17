package com.tramchester.domain.places;

import com.tramchester.domain.*;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;

public class RouteStation implements HasId, HasTransportMode {
    // A station that serves a specific route

    private final Station station;
    private final Route route;
    private final String id;

    public RouteStation(Station station, Route route) {
        this.station = station;
        this.route = route;
        id = formId(station, route);
    }

    public String getId() {
        return id;
    }

    public Agency getAgency() {
        return route.getAgency();
    }

    @Override
    public String toString() {
        return "RouteStation{" +
                "stationId=" + station.getId() +
                ", routeId=" + route.getId() +
                '}';
    }

    public Route getRoute() {
        return route;
    }

    public static String formId(Location station, Route route) {
        return station.getId() + route.getId().replaceAll(" ","");
    }

    // Use get getStation
    @Deprecated
    public String getStationId() {
        return station.getId();
    }

    public Station getStation() {
        return station;
    }

    @Override
    public TransportMode getTransportMode() {
        return route.getTransportMode();
    }
}
