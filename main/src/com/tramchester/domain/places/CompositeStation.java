package com.tramchester.domain.places;

import com.google.common.collect.Streams;
import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.CompositeId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.GraphPropertyKey;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompositeStation implements Station {

    // TODO
    private final Set<Station> containedStations;
    private final String area;
    private final String name;
    private final int minChangeCost;
    private final IdFor<Station> id;
    private final LatLong latLong;
    private final DataSourceID dataSourceId;

    public CompositeStation(Set<Station> containedStations, String area, String name, int minChangeCost) {
        this.id = computeStationId(containedStations);
        this.latLong = computeLatLong(containedStations);
        this.dataSourceId = computeDataSourceId(containedStations);
        this.containedStations = containedStations;
        this.area = area;
        this.name = name;
        this.minChangeCost = minChangeCost;
    }

    public static Set<Station> expandStations(Collection<Station> stations) {
        return stations.stream().flatMap(CompositeStation::expandStation).collect(Collectors.toSet());
    }

    private static Stream<Station> expandStation(Station station) {
        if (!(station instanceof CompositeStation)) {
            return Stream.of(station);
        }

        CompositeStation compositeStation = (CompositeStation) station;
        return Streams.concat(compositeStation.getContained().stream(), Stream.of(station));
    }

    @Override
    public boolean hasPlatform(IdFor<Platform> platformId) {
        return anyMatch(station -> station.hasPlatform(platformId));
    }

    @Override
    public Set<Platform> getPlatforms() {
        return flatten(Station::getPlatforms);
    }

    @Override
    public Set<Platform> getPlatformsForRoute(Route route) {
        return flatten(station -> station.getPlatformsForRoute(route));
    }

    @Override
    public boolean hasPlatformsForRoute(Route route) {
        return anyMatch(station -> station.hasPlatformsForRoute(route));
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
        return anyMatch(Station::hasPlatforms);
    }

    @Override
    public Set<Route> getDropoffRoutes() {
        return flatten(Station::getDropoffRoutes);
    }

    @Override
    public Set<Route> getPickupRoutes() {
        return flatten(Station::getPickupRoutes);
    }

    @Override
    public boolean servesRoutePickup(Route route) {
        return anyMatch(station -> station.servesRoutePickup(route));
    }

    @Override
    public boolean servesRouteDropoff(Route route) {
        return anyMatch(station -> station.servesRouteDropoff(route));
    }

    @Override
    public GridPosition getGridPosition() {
        return CoordinateTransforms.getGridPosition(latLong);
    }

    @Override
    public boolean isComposite() {
        return true;
    }

    @Override
    public GraphPropertyKey getProp() {
        return GraphPropertyKey.STATION_ID;
    }

    @Override
    public LocationType getLocationType() {
        return LocationType.CompositeStation;
    }

    @Override
    public DataSourceID getDataSourceID() {
        return dataSourceId;
    }

    @Override
    public boolean hasPickup() {
        return anyMatch(Location::hasPickup);
    }

    @Override
    public boolean hasDropoff() {
        return anyMatch(Location::hasDropoff);
    }

    @Override
    public String forDTO() {
        return id.forDTO();
    }

    @Override
    public Set<TransportMode> getTransportModes() {
        return flatten(Station::getTransportModes);
    }

    @Override
    public boolean servesMode(TransportMode mode) {
        return anyMatch(station -> station.servesMode(mode));
    }

    @Override
    public boolean isMarkedInterchange() {
        return false;
    }

    @Override
    public int getMinimumChangeCost() {
        return minChangeCost;
    }

    @Override
    public Set<Agency> getAgencies() {
        return flatten(Station::getAgencies);
    }

    private boolean anyMatch(Predicate<Station> predicate) {
        return containedStations.stream().anyMatch(predicate);
    }

    private <R> Set<R> flatten(Function<Station, Collection<R>> map) {
        return containedStations.stream().
                flatMap(station -> map.apply(station).stream()).
                collect(Collectors.toUnmodifiableSet());
    }

    public Set<Station> getContained() {
        return containedStations;
    }

    private static DataSourceID computeDataSourceId(Set<Station> stations) {
        Set<DataSourceID> sourceIds = stations.stream().map(Station::getDataSourceID).collect(Collectors.toSet());
        if (sourceIds.size()!=1) {
            throw new RuntimeException("Composite stations must call come from same datasource, stations: " + stations);
        }
        return sourceIds.iterator().next();
    }

    private static LatLong computeLatLong(Set<Station> stations) {
        double lat = stations.stream().mapToDouble(station -> station.getLatLong().getLat()).
                average().orElse(Double.NaN);
        double lon = stations.stream().mapToDouble(station -> station.getLatLong().getLon()).
                average().orElse(Double.NaN);
        return new LatLong(lat, lon);
    }

    private static IdFor<Station> computeStationId(Set<Station> stations) {
        IdSet<Station> ids = stations.stream().map(Station::getId).collect(IdSet.idCollector());
        return new CompositeId<>(ids);
    }

}
