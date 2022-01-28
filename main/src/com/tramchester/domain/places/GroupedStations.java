package com.tramchester.domain.places;

import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.graphbuild.GraphLabel;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// TODO Should the ID here be NaptanArea Id not station ID?

/***
 * Stations grouped togther as in same naptan area id
 *
 * see also class: com.tramchester.graph.GraphQuery::getGroupedNode
 */
public class GroupedStations implements Location<GroupedStations> {
    private final IdFor<GroupedStations> id;
    private final IdFor<NaptanArea> areaId;
    private final Set<Station> groupedStations;
    private final String name;
    private final int minChangeCost;

    private final LatLong latLong;
    private final DataSourceID dataSourceId;

    public GroupedStations(Set<Station> groupedStations, IdFor<NaptanArea> areaId, String name, int minChangeCost) {
        this.id = StringIdFor.convert(areaId);
        this.latLong = computeLatLong(groupedStations);
        this.dataSourceId = computeDataSourceId(groupedStations);
        this.groupedStations = groupedStations;
        this.areaId = areaId;
        this.name = name;
        this.minChangeCost = minChangeCost;
    }

//    public static Set<Station> expandStations(Collection<Station> stations) {
//        return stations.stream().flatMap(GroupedStations::expandStation).collect(Collectors.toSet());
//    }

//    private static Stream<Station> expandStation(Station station) {
//        if (!(station instanceof GroupedStations)) {
//            return Stream.of(station);
//        }
//
//        GroupedStations compositeStation = (GroupedStations) station;
//        return Streams.concat(compositeStation.getContained().stream(), Stream.of(station));
//    }

//    @Override
    public boolean hasPlatform(IdFor<Platform> platformId) {
        return anyMatch(station -> station.hasPlatform(platformId));
    }

    @Override
    public Set<Platform> getPlatforms() {
        return flatten(Station::getPlatforms);
    }

//    @Override
//    public Set<Platform> getPlatformsForRoute(Route route) {
//        return flatten(station -> station.getPlatformsForRoute(route));
//    }

//    @Override
//    public boolean hasPlatformsForRoute(Route route) {
//        return anyMatch(station -> station.hasPlatformsForRoute(route));
//    }

    @Override
    public IdFor<GroupedStations> getId() {
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
        return anyMatch(Station::hasPlatforms);
    }

//    @Override
    public Set<Route> getDropoffRoutes() {
        return flatten(Station::getDropoffRoutes);
    }

//    @Override
    public Set<Route> getPickupRoutes() {
        return flatten(Station::getPickupRoutes);
    }

//    @Override
//    public boolean servesRoutePickup(Route route) {
//        return anyMatch(station -> station.servesRoutePickup(route));
//    }
//
//    @Override
//    public boolean servesRouteDropoff(Route route) {
//        return anyMatch(station -> station.servesRouteDropoff(route));
//    }

    @Override
    public GridPosition getGridPosition() {
        return CoordinateTransforms.getGridPosition(latLong);
    }

//    @Override
    public boolean isStationGroup() {
        return true;
    }

    @Override
    public GraphPropertyKey getProp() {
        return GraphPropertyKey.AREA_ID;
    }

    @Override
    public GraphLabel getNodeLabel() {
        return GraphLabel.GROUPED;
    }

    @Override
    public LocationType getLocationType() {
        return LocationType.StationGroup;
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
    public boolean isActive() {
        return anyMatch(Location::isActive);
    }



    @Override
    public String forDTO() {
        return id.forDTO();
    }

    @Override
    public Set<TransportMode> getTransportModes() {
        return flatten(Station::getTransportModes);
    }

//    @Override
    public boolean servesMode(TransportMode mode) {
        return anyMatch(station -> station.servesMode(mode));
    }

//    @Override
    public boolean isMarkedInterchange() {
        return false;
    }

//    @Override
    public int getMinimumChangeCost() {
        return minChangeCost;
    }

//    @Override
    public Set<Agency> getAgencies() {
        return flatten(Station::getAgencies);
    }

    private boolean anyMatch(Predicate<Station> predicate) {
        return groupedStations.stream().anyMatch(predicate);
    }

    private <R> Set<R> flatten(Function<Station, Collection<R>> map) {
        return groupedStations.stream().
                flatMap(station -> map.apply(station).stream()).
                collect(Collectors.toUnmodifiableSet());
    }

    public Set<Station> getContained() {
        return groupedStations;
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

//    public int numberContained() {
//        return groupedStations.size();
//    }

    @Override
    public String toString() {
        return "GroupedStations{" +
                "areaId=" + areaId +
                ", groupedStations=" + HasId.asIds(groupedStations) +
                ", name='" + name + '\'' +
                ", minChangeCost=" + minChangeCost +
                ", id=" + id +
                ", latLong=" + latLong +
                ", dataSourceId=" + dataSourceId +
                '}';
    }
}
