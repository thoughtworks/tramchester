package com.tramchester.domain.places;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.graphbuild.GraphLabel;

import java.time.Duration;
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
public class StationGroup implements Location<StationGroup> {
    private final IdFor<StationGroup> id;
    private final IdFor<NaptanArea> areaId;
    private final Set<Station> groupedStations;
    private final String name;
    private final Duration minChangeCost;

    private final LatLong latLong;
    private final DataSourceID dataSourceId;

    public StationGroup(Set<Station> groupedStations, IdFor<NaptanArea> areaId, String name, Duration changeTimeNeeded) {
        this.id = StringIdFor.convert(areaId);
        this.latLong = computeLatLong(groupedStations);
        this.dataSourceId = computeDataSourceId(groupedStations);
        this.groupedStations = groupedStations;
        this.areaId = areaId;
        this.name = name;
        this.minChangeCost = changeTimeNeeded;
    }

    public Set<Station> getContained() {
        return groupedStations;
    }

    @Override
    public Set<Platform> getPlatforms() {
        return flatten(Station::getPlatforms);
    }

    @Override
    public IdFor<StationGroup> getId() {
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

    @Override
    public Set<Route> getDropoffRoutes() {
        return flatten(Station::getDropoffRoutes);
    }

    @Override
    public Set<Route> getPickupRoutes() {
        return flatten(Station::getPickupRoutes);
    }

    @Override
    public boolean isMarkedInterchange() {
        return anyMatch(Station::isMarkedInterchange);
    }

    @Override
    public GridPosition getGridPosition() {
        return CoordinateTransforms.getGridPosition(latLong);
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

    public Duration getMinimumChangeCost() {
        return minChangeCost;
    }

    private boolean anyMatch(Predicate<Station> predicate) {
        return groupedStations.stream().anyMatch(predicate);
    }

    private <R> Set<R> flatten(Function<Station, Collection<R>> map) {
        return groupedStations.stream().
                flatMap(station -> map.apply(station).stream()).
                collect(Collectors.toUnmodifiableSet());
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
