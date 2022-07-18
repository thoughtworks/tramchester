package com.tramchester.domain.places;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.HasTransportModes;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RouteStationId;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphPropertyKey;

import java.util.Collections;
import java.util.Set;

public class RouteStation implements HasId<RouteStation>, GraphProperty, HasTransportModes, CoreDomain {
    // A station that serves a specific route

    private final Station station;
    private final Route route;
    private final IdFor<RouteStation> id;

    public RouteStation(Station station, Route route) {
        this.station = station;
        this.route = route;
        id = createId(station.getId(), route.getId());
    }

    public static IdFor<RouteStation> createId(IdFor<Station> station, IdFor<Route> route) {
        return RouteStationId.createId(route, station);
    }

    public IdFor<RouteStation> getId() {
        return id;
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

    /***
     * The single transport mode, see also getTransportMode()
     * @return Singleton containing the transport mode
     */
    @Override
    public Set<TransportMode> getTransportModes() {
        return Collections.singleton(route.getTransportMode());
    }

    @Override
    public GraphPropertyKey getProp() {
        return GraphPropertyKey.ROUTE_STATION_ID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RouteStation that = (RouteStation) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public boolean isActive() {
        return station.servesRouteDropOff(route) || station.servesRoutePickup(route);
    }
}
