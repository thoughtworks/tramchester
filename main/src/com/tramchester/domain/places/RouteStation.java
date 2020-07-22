package com.tramchester.domain.places;

import com.tramchester.domain.*;

public class RouteStation implements HasId<RouteStation>, HasTransportMode {
    // A station that serves a specific route

    private final Station station;
    private final Route route;
    private final IdFor<RouteStation> id;

    public RouteStation(Station station, Route route) {
        this.station = station;
        this.route = route;
        id = IdFor.createId(station, route);
    }

    public static IdFor<RouteStation> formId(Station station, Route route) {
        return IdFor.createId(station, route);
    }

    public IdFor<RouteStation> getId() {
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

    public IdFor<Station> getStationId() {
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
