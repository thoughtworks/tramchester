package com.tramchester.domain.places;

import com.tramchester.domain.*;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.graphbuild.GraphLabel;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MutableStation implements Station {

    private final IdFor<NaptanArea> areaId;
    private final IdFor<Station> id;
    private final String name;
    private final LatLong latLong;
    private final GridPosition gridPosition;
    private final Set<Platform> platforms;
    private final Set<Route> servesRoutesPickup;
    private final Set<Route> servesRoutesDropoff;
    private final Set<Route> passedByRoute; // i.e. a station being passed by a train, but the train does not stop
    private final Set<Agency> servesAgencies;
    private final DataSourceID dataSourceID;
    private final boolean isMarkedInterchange;
    private final Set<TransportMode> modes;

    public MutableStation(IdFor<Station> id, IdFor<NaptanArea> areaId, String stationName, LatLong latLong, GridPosition gridPosition,
                          DataSourceID dataSourceID) {
        this(id, areaId, stationName, latLong, gridPosition, dataSourceID, false);

    }

    // for some data sources we know if station is an interchange
    public MutableStation(IdFor<Station> id, IdFor<NaptanArea> areaId, String stationName, LatLong latLong, GridPosition gridPosition,
                          DataSourceID dataSourceID, boolean isMarkedInterchange) {
        this.areaId = areaId;
        this.gridPosition = gridPosition;
        this.dataSourceID = dataSourceID;
        this.isMarkedInterchange = isMarkedInterchange;
        platforms = new HashSet<>();
        servesRoutesPickup = new HashSet<>();
        servesRoutesDropoff = new HashSet<>();
        servesAgencies = new HashSet<>();
        passedByRoute = new HashSet<>();

        this.id = id;
        this.name = stationName;
        this.latLong = latLong;
        modes = new HashSet<>();
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
    public IdFor<NaptanArea> getAreaId() {
        return areaId;
    }

    @Override
    public boolean hasPlatforms() {
        return !platforms.isEmpty();
    }

    @Override
    public Set<TransportMode> getTransportModes() {
        return modes;
    }

    @Override
    public boolean servesMode(TransportMode mode) {
        return Stream.concat(servesRoutesDropoff.stream(), servesRoutesPickup.stream()).
                anyMatch(route -> route.getTransportMode().equals(mode));
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
    public boolean hasPickup() {
        return !servesRoutesPickup.isEmpty();
    }

    @Override
    public boolean hasDropoff() {
        return !servesRoutesDropoff.isEmpty();
    }

    @Override
    public boolean isActive() {
        return hasPickup() || hasDropoff();
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
    public Set<Route> getDropoffRoutes() {
        return Collections.unmodifiableSet(servesRoutesDropoff);
    }

    @Override
    public Set<Route> getPickupRoutes() {
        return Collections.unmodifiableSet(servesRoutesPickup);
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
        return servesRoutesPickup.contains(route);
    }

    @Override
    public boolean servesRouteDropoff(Route route) {
        return servesRoutesDropoff.contains(route);
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
    public GraphLabel getNodeLabel() {
        return GraphLabel.STATION;
    }

    public String forDTO()  {
        return getId().forDTO();
    }

    @Override
    public boolean isMarkedInterchange() {
        return isMarkedInterchange;
    }

    @Override
    public int getMinimumChangeCost() {
        // TODO
        return 1;
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
                "areaId='" + areaId + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", latLong=" + latLong +
                ", mode=" + getTransportModes() +
                ", platforms=" + HasId.asIds(platforms) +
                ", servesRoutesPickup=" + HasId.asIds(servesRoutesPickup) +
                ", servesRoutesDropoff=" + HasId.asIds(servesRoutesDropoff) +
                ", passedByRoute=" + HasId.asIds(passedByRoute) +
                ", isMarkedInterchange=" + isMarkedInterchange +
                '}';
    }

    public MutableStation addPlatform(Platform platform) {
        platforms.add(platform);
        return this;
    }

    public void addRouteDropOff(Route dropoffFromRoute) {
        modes.add(dropoffFromRoute.getTransportMode());
        servesAgencies.add(dropoffFromRoute.getAgency());
        servesRoutesDropoff.add(dropoffFromRoute);
    }

    public void addRoutePickUp(Route pickupFromRoute) {
        modes.add(pickupFromRoute.getTransportMode());
        servesAgencies.add(pickupFromRoute.getAgency());
        servesRoutesPickup.add(pickupFromRoute);
    }

    /***
     * Station is passed by a route i.e. a station being passed by a train, but the train does not stop
     * @param route the passing route
     */
    public void addPassingRoute(MutableRoute route) {
        modes.add(route.getTransportMode());
        passedByRoute.add(route);
    }
}
