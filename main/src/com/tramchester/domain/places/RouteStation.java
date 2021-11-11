package com.tramchester.domain.places;

import com.tramchester.domain.*;
import com.tramchester.domain.id.MixedCompositeId;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.GraphPropertyKey;

import java.util.Collections;
import java.util.Set;

public class RouteStation implements HasId<RouteStation>, GraphProperty, Location<RouteStation> {
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
        return MixedCompositeId.createId(route, station);
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
    public String getName() {
        return station.getName();
    }

    @Override
    public LatLong getLatLong() {
        return station.getLatLong();
    }

    @Override
    public GridPosition getGridPosition() {
        return station.getGridPosition();
    }

    @Override
    public String getArea() {
        return station.getArea();
    }

    @Override
    public boolean hasPlatforms() {
        return station.hasPlatformsForRoute(route);
    }

    @Override
    public Set<Platform> getPlatforms() {
        return station.getPlatformsForRoute(route);
    }

    /***
     * The single transport mode, see also getTransportMode()
     * @return Singleton containing the transport mode
     */
    @Override
    public Set<TransportMode> getTransportModes() {
        return Collections.singleton(route.getTransportMode());
    }

    public TransportMode getTransportMode() {
        return route.getTransportMode();
    }

    @Override
    public LocationType getLocationType() {
        return LocationType.RouteStation;
    }

    @Override
    public DataSourceID getDataSourceID() {
        return station.getDataSourceID();
    }

    @Override
    public GraphPropertyKey getProp() {
        return GraphPropertyKey.ROUTE_STATION_ID;
    }

    @Override
    public String forDTO() {
        return id.forDTO();
    }
}
