package com.tramchester.domain.places;

import com.tramchester.domain.id.CompositeId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.graph.GraphPropertyKey;

import java.util.Set;

public class CompositeStation extends Station {

    // TODO Should just be implements Location<Station> extends Station??

    private final Set<Station> stations;

    public CompositeStation(Set<Station> stations, String area, String name) {
        super(computeId(stations), area, name, computeLatLong(stations), CoordinateTransforms.getGridPosition(computeLatLong(stations)));
        this.stations = stations;
        stations.forEach(station -> {
            station.getRoutes().forEach(this::addRoute);
            station.getPlatforms().forEach(this::addPlatform);
        });
    }

    private static LatLong computeLatLong(Set<Station> stations) {
        double lat = stations.stream().mapToDouble(station -> station.getLatLong().getLat()).
                average().orElse(Double.NaN);
        double lon = stations.stream().mapToDouble(station -> station.getLatLong().getLon()).
                average().orElse(Double.NaN);
        return new LatLong(lat, lon);
    }

    private static IdFor<Station> computeId(Set<Station> stations) {
        IdSet<Station> ids = stations.stream().map(Station::getId).collect(IdSet.idCollector());
        return new CompositeId<>(ids);
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

    public Set<Station> getContained() {
        return stations;
    }
}
