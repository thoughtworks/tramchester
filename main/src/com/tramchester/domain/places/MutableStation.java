package com.tramchester.domain.places;

import com.tramchester.domain.*;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.graphbuild.GraphLabel;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MutableStation implements Station {

    // TODO into config?
    public static final int DEFAULT_MIN_CHANGE_TIME = 1;

    private final IdFor<NaptanArea> areaId;
    private final IdFor<Station> id;
    private final String name;
    private final LatLong latLong;
    private final GridPosition gridPosition;
    private final Set<Platform> platforms;
    private final Set<RouteAndService> servesRoutesPickup;
    private final Set<RouteAndService> servesRoutesDropoff;
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
        servesRoutesPickup = new HashSet<>();
        servesRoutesDropoff = new HashSet<>();
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
        return routesFrom(servesRoutesDropoff);
    }

    @Override
    public Set<Route> getPickupRoutes() {
        return routesFrom(servesRoutesPickup);
    }

    private Set<Route> routesFrom(Set<RouteAndService> routeAndServices) {
        return routeAndServices.stream().
                map(RouteAndService::getRoute).
                collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<Route> getDropoffRoutes(LocalDate date) {
        return getRoutesFor(servesRoutesDropoff, date);
    }

    @Override
    public Set<Route> getPickupRoutes(LocalDate date) {
        return getRoutesFor(servesRoutesPickup, date);
    }

    private Set<Route> getRoutesFor(Set<RouteAndService> routeAndServices, LocalDate date) {
        return routeAndServices.stream().
                filter(routeAndService -> routeAndService.isAvailableOn(date)).
                map(RouteAndService::getRoute).
                collect(Collectors.toSet());
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
        return RouteAndService.contains(servesRoutesPickup, route);
    }

    @Override
    public boolean servesRouteDropoff(Route route) {
        return RouteAndService.contains(servesRoutesDropoff, route);
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

    public void addRouteDropOff(Route dropoffFromRoute, Service service) {
        modes.add(dropoffFromRoute.getTransportMode());
        servesAgencies.add(dropoffFromRoute.getAgency());
        servesRoutesDropoff.add(new RouteAndService(dropoffFromRoute, service));
    }

    public void addRoutePickUp(Route pickupFromRoute, Service service) {
        modes.add(pickupFromRoute.getTransportMode());
        servesAgencies.add(pickupFromRoute.getAgency());
        servesRoutesPickup.add(new RouteAndService(pickupFromRoute, service));
    }

    /***
     * Station is passed by a route i.e. a station being passed by a train, but the train does not stop
     * @param route the passing route
     */
    public void addPassingRoute(MutableRoute route) {
        modes.add(route.getTransportMode());
        passedByRoute.add(route);
    }

    private static class RouteAndService {

        private final Route route;
        private final Service service;

        public RouteAndService(Route route, Service service) {

            this.route = route;
            this.service = service;
        }

        public static boolean contains(Set<RouteAndService> routeAndServices, Route route) {
            return routeAndServices.stream().
                    anyMatch(routeAndService -> routeAndService.getRoute().equals(route));
        }

        public TransportMode getTransportMode() {
            return route.getTransportMode();
        }

        public boolean isAvailableOn(LocalDate date) {
            if (!route.isAvailableOn(date)) {
                return false;
            }
            return service.getCalendar().operatesOn(date);
        }

        public Route getRoute() {
            return route;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RouteAndService that = (RouteAndService) o;
            return route.equals(that.route) && service.equals(that.service);
        }

        @Override
        public int hashCode() {
            return Objects.hash(route, service);
        }

        @Override
        public String toString() {
            return "RouteAndService{" +
                    "route=" + route.getId() +
                    ", service=" + service.getId() +
                    '}';
        }
    }
}
