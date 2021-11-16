package com.tramchester.domain.places;

import com.tramchester.domain.*;
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

public class MutableStation implements Station {

    private final String area;
    private final IdFor<Station> id;
    private final String name;
    private final LatLong latLong;
    private final GridPosition gridPosition;
    private final Set<Platform> platforms;
    private final Set<Route> servesRoutes;
    private final Set<Agency> servesAgencies;
    private final DataSourceID dataSourceID;

    public MutableStation(IdFor<Station> id, String area, String stationName, LatLong latLong, GridPosition gridPosition,
                          DataSourceID dataSourceID) {
        this.gridPosition = gridPosition;
        this.dataSourceID = dataSourceID;
        platforms = new HashSet<>();
        servesRoutes = new HashSet<>();
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
        return servesRoutes.stream().map(Route::getTransportMode).collect(Collectors.toUnmodifiableSet());
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

    @Override
    public Set<Route> getRoutes() {
        return Collections.unmodifiableSet(servesRoutes);
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
    public boolean servesRoute(Route route) {
        return servesRoutes.contains(route);
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
        return servesRoutes.stream().anyMatch(route -> route.getTransportMode().equals(mode));
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
                ", servesRoutes=" + HasId.asIds(servesRoutes) +
                '}';
    }

    public void addPlatform(Platform platform) {
        platforms.add(platform);
    }

    public void addRoute(Route route) {
        servesRoutes.add(route);
        servesAgencies.add(route.getAgency());
    }

}
