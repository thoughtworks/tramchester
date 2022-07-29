package com.tramchester.domain.places;

import com.tramchester.domain.*;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.graphbuild.GraphLabel;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class MutableStation implements Station {

    // TODO into config?
    public static final int DEFAULT_MIN_CHANGE_TIME = 1;

    private final IdFor<NaptanArea> areaId;
    private final IdFor<Station> id;
    private final String name;
    private final LatLong latLong;
    private final GridPosition gridPosition;
    private final Set<Platform> platforms;
    private final ServedRoute servesRoutesPickup;
    private final ServedRoute servesRoutesDropoff;
    private final Set<Route> passedByRoute; // i.e. a station being passed by a train, but the train does not stop
    private final Set<Agency> servesAgencies;
    private final DataSourceID dataSourceID;
    private final boolean isMarkedInterchange;
    private final Set<TransportMode> modes;
    private final Duration changeTimeNeeded;

    public MutableStation(IdFor<Station> id, IdFor<NaptanArea> areaId, String stationName, LatLong latLong, GridPosition gridPosition,
                          DataSourceID dataSourceID) {
        // todo default change duration from config for the data source?
        this(id, areaId, stationName, latLong, gridPosition, dataSourceID, false,
                Duration.ofMinutes(DEFAULT_MIN_CHANGE_TIME));
    }

    // for some data sources we know if station is an interchange
    public MutableStation(IdFor<Station> id, IdFor<NaptanArea> areaId, String stationName, LatLong latLong, GridPosition gridPosition,
                          DataSourceID dataSourceID, boolean isMarkedInterchange, Duration changeTimeNeeded) {
        this.areaId = areaId;
        this.gridPosition = gridPosition;
        this.dataSourceID = dataSourceID;
        this.isMarkedInterchange = isMarkedInterchange;
        this.changeTimeNeeded = changeTimeNeeded;
        platforms = new HashSet<>();
        servesRoutesPickup = new ServedRoute();
        servesRoutesDropoff = new ServedRoute();
        servesAgencies = new HashSet<>();
        passedByRoute = new HashSet<>();

        this.id = id;
        this.name = stationName;
        this.latLong = latLong;
        modes = new HashSet<>();
    }

    public static Station Unknown(DataSourceID dataSourceID) {
        return new MutableStation(StringIdFor.createId("unknown"), NaptanArea.Invalid().getId(), "Unknown",
                LatLong.Invalid, GridPosition.Invalid, dataSourceID, false, Duration.ZERO);
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
        return servesRoutesPickup.serves(mode) || servesRoutesDropoff.serves(mode);

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
        return platforms.stream().
                filter(platform -> platform.getDropoffRoutes().contains(route) || platform.getPickupRoutes().contains(route)).
                collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean hasPlatform(IdFor<Platform> platformId) {
        return platforms.stream().map(Platform::getId).anyMatch(id -> id.equals(platformId));
    }

    @Override
    public Set<Route> getDropoffRoutes() {
        return servesRoutesDropoff.getRoutes();
    }

    @Override
    public Set<Route> getPickupRoutes() {
        return servesRoutesPickup.getRoutes();
    }

    @Override
    public Set<Route> getDropoffRoutes(LocalDate date, TimeRange timeRange) {
        return servesRoutesDropoff.getRoutes(date, timeRange);
    }

    @Override
    public Set<Route> getPickupRoutes(LocalDate date, TimeRange timeRange) {
        return servesRoutesPickup.getRoutes(date, timeRange);
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
    public boolean servesRouteDropOff(Route route) {
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
    public Duration getMinChangeDuration() {
        return changeTimeNeeded;
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
                ", servesRoutesPickup=" + servesRoutesPickup +
                ", servesRoutesDropoff=" + servesRoutesDropoff +
                ", passedByRoute=" + HasId.asIds(passedByRoute) +
                ", isMarkedInterchange=" + isMarkedInterchange +
                '}';
    }

    public MutableStation addPlatform(Platform platform) {
        platforms.add(platform);
        return this;
    }

    @Deprecated
    public void addRouteDropOff(Route dropoffFromRoute, Service service, TramTime dropOffTime) {
        modes.add(dropoffFromRoute.getTransportMode());
        servesAgencies.add(dropoffFromRoute.getAgency());
        servesRoutesDropoff.add(dropoffFromRoute, service, dropOffTime);
    }

    @Deprecated
    public void addRoutePickUp(Route pickupFromRoute, Service service, TramTime pickupTime) {
        modes.add(pickupFromRoute.getTransportMode());
        servesAgencies.add(pickupFromRoute.getAgency());
        servesRoutesPickup.add(pickupFromRoute, service, pickupTime);
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
