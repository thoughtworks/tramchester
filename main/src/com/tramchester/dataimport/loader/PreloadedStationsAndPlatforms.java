package com.tramchester.dataimport.loader;

import com.tramchester.dataimport.data.StopData;
import com.tramchester.domain.MutablePlatform;
import com.tramchester.domain.Platform;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.id.CompositeIdMap;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.GridPosition;

import java.util.Optional;

class PreloadedStationsAndPlatforms {
    private final CompositeIdMap<Station, MutableStation> stations;
    private final CompositeIdMap<Platform, MutablePlatform> platforms;
    private final TransportEntityFactory factory;

    PreloadedStationsAndPlatforms(TransportEntityFactory factory) {
        stations = new CompositeIdMap<>();
        platforms = new CompositeIdMap<>();
        this.factory = factory;
    }

    public boolean hasId(IdFor<Station> stationId) {
        return stations.hasId(stationId);
    }

    public int size() {
        return stations.size();
    }

    public void clear() {
        stations.clear();
    }

    public MutableStation get(IdFor<Station> stationId) {
        return stations.get(stationId);
    }

    public void createAndAdd(IdFor<Station> stationId, StopData stopData, GridPosition position) {
        MutableStation mutableStation = factory.createStation(stationId, stopData, position);

        Optional<MutablePlatform> possiblePlatform = factory.maybeCreatePlatform(stopData);
        possiblePlatform.ifPresent(platform -> {
            platforms.add(platform);
            mutableStation.addPlatform(platform);
        });

        stations.add(mutableStation);
    }

    public void updateStation(IdFor<Station> stationId, StopData stopData) {
        Optional<MutablePlatform> possiblePlatform = factory.maybeCreatePlatform(stopData);
        possiblePlatform.ifPresent(platform -> {
            platforms.add(platform);
            stations.get(stationId).addPlatform(platform);
        });
    }

    public MutablePlatform getPlatform(IdFor<Platform> id) {
        return platforms.get(id);
    }
}
