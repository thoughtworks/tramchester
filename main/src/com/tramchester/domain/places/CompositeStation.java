package com.tramchester.domain.places;

import com.google.common.collect.Streams;
import com.tramchester.domain.*;
import com.tramchester.domain.id.CompositeId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.graph.GraphPropertyKey;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompositeStation extends MutableStation {

    // TODO Should just be implements Location<Station> extends Station??

    private final Set<Station> containedStations;

    public CompositeStation(Set<Station> containedStations, String area, String name) {
        super(computeStationId(containedStations), area, name, computeLatLong(containedStations),
                CoordinateTransforms.getGridPosition(computeLatLong(containedStations)), computeDataSourceId(containedStations));
        this.containedStations = containedStations;
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

    private boolean anyMatch(Predicate<Station> predicate) {
        return containedStations.stream().anyMatch(predicate);
    }

    private <R> Set<R> flatten(Function<Station, Collection<R>> map) {
        return containedStations.stream().
                flatMap(station -> map.apply(station).stream()).
                collect(Collectors.toUnmodifiableSet());
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
    public boolean hasPlatforms() {
        return anyMatch(Station::hasPlatforms);
    }

    @Override
    public Set<Route> getRoutes() {
        return flatten(Station::getRoutes);
    }

//    @Override
//    public boolean servesRoute(Route route) {
//        return anyMatch(station -> station.servesRoute(route));
//    }

    @Override
    public boolean servesRoutePickup(Route route) {
        return anyMatch(station -> station.servesRoutePickup(route));
    }

    @Override
    public boolean servesRouteDropoff(Route route) {
        return anyMatch(station -> station.servesRouteDropoff(route));
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
        return LocationType.Station;
    }

    @Override
    public Set<TransportMode> getTransportModes() {
        return flatten(Station::getTransportModes);
    }

    @Override
    public boolean serves(TransportMode mode) {
        return anyMatch(station -> station.serves(mode));
    }

    @Override
    public Set<Agency> getAgencies() {
        return flatten(Station::getAgencies);
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
