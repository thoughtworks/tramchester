package com.tramchester.domain.places;

import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.GraphPropertyKey;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MutableStation implements Station {

    private final String area;
    private final IdFor<Station> id;
    private final String name;
    private final LatLong latLong;
    private final GridPosition gridPosition;
    private final Set<Platform> platforms;
    private final Set<Route> servesRoutesPickup;
    private final Set<Route> servesRoutesDropoff;
    private final Set<Agency> servesAgencies;
    private final DataSourceID dataSourceID;
    private final boolean isMarkedInterchange;

    public MutableStation(IdFor<Station> id, String area, String stationName, LatLong latLong, GridPosition gridPosition,
                          DataSourceID dataSourceID) {
        this(id, area, stationName, latLong, gridPosition, dataSourceID, false);

    }

    // for some data sources we know if station is an interchange
    public MutableStation(IdFor<Station> id, String area, String stationName, LatLong latLong, GridPosition gridPosition,
                          DataSourceID dataSourceID, boolean isMarkedInterchange) {
        this.gridPosition = gridPosition;
        this.dataSourceID = dataSourceID;
        this.isMarkedInterchange = isMarkedInterchange;
        platforms = new HashSet<>();
        servesRoutesPickup = new HashSet<>();
        servesRoutesDropoff = new HashSet<>();
        servesAgencies = new HashSet<>();

        this.id = id;
        this.name = stationName;
        this.latLong = latLong;
        this.area = area;
    }

    @Override
    public IdFor<Station> getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LatLong getLatLong() {
        return latLong;
    }

    @Override
    public String getArea() {
        return area;
    }

    @Override
    public boolean hasPlatforms() {
        return !platforms.isEmpty();
    }

    @Override
    public boolean isComposite() {
        return false;
    }

    @Override
    public Set<TransportMode> getTransportModes() {
        return Stream.concat(servesRoutesDropoff.stream(), servesRoutesPickup.stream()).
                map(Route::getTransportMode).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public LocationType getLocationType() {
        return LocationType.Station;
    }

    @Override
    public DataSourceID getDataSourceID() {
        return dataSourceID;
    }

    @Override
    public Set<Platform> getPlatformsForRoute(Route route) {
        return platforms.stream().filter(platform -> platform.getRoutes().contains(route)).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean hasPlatform(IdFor<Platform> platformId) {
        return platforms.stream().map(Platform::getId).anyMatch(id -> id.equals(platformId));
    }

    @Deprecated
    @Override
    public Set<Route> getRoutes() {
        return Stream.concat(servesRoutesDropoff.stream(), servesRoutesPickup.stream()).collect(Collectors.toSet());
    }

    @Override
    public Set<Agency> getAgencies() {
        return Collections.unmodifiableSet(servesAgencies);
    }

    @Override
    public Set<Platform> getPlatforms() {
        return  Collections.unmodifiableSet(platforms);
    }

    @Override
    public boolean servesRoutePickup(Route route) {
        return servesRoutesPickup.stream().anyMatch(item -> item.equals(route));
    }

    @Override
    public boolean servesRouteDropoff(Route route) {
        return servesRoutesDropoff.stream().anyMatch(item -> item.equals(route));
    }

    @Override
    public GridPosition getGridPosition() {
        return gridPosition;
    }

    @Override
    public GraphPropertyKey getProp() {
        return GraphPropertyKey.STATION_ID;
    }

    @Override
    public boolean hasPlatformsForRoute(Route route) {
        return platforms.stream().anyMatch(platform -> platform.servesRoute(route));
    }

    public String forDTO()  {
        return getId().forDTO();
    }

    @Override
    public boolean serves(TransportMode mode) {
        return Stream.concat(servesRoutesDropoff.stream(), servesRoutesPickup.stream()).
                anyMatch(route -> route.getTransportMode().equals(mode));
    }

    @Override
    public boolean isMarkedInterchange() {
        return isMarkedInterchange;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        if (!(o instanceof MutableStation)) return false;

        MutableStation station = (MutableStation) o;

        return id.equals(station.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Station{" +
                "area='" + area + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", latLong=" + latLong +
                ", mode=" + getTransportModes() +
                ", platforms=" + HasId.asIds(platforms) +
                ", servesRoutesPickup=" + HasId.asIds(servesRoutesPickup) +
                ", servesRoutesDropoff=" + HasId.asIds(servesRoutesDropoff) +
                ", isMarkedInterchange=" + isMarkedInterchange +
                '}';
    }

    public void addPlatform(Platform platform) {
        platforms.add(platform);
    }

    @Deprecated
    public void addRoute(Route route) {
        servesRoutesDropoff.add(route);
        servesRoutesPickup.add(route);
        servesAgencies.add(route.getAgency());
    }

    public void addRouteDropOff(Route dropoffFromRoute) {
        servesAgencies.add(dropoffFromRoute.getAgency());
        servesRoutesDropoff.add(dropoffFromRoute);
    }

    public void addRoutePickUp(Route pickupFromRoute) {
        servesAgencies.add(pickupFromRoute.getAgency());
        servesRoutesPickup.add(pickupFromRoute);
    }
}
