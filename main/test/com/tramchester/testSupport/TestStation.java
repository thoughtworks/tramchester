package com.tramchester.testSupport;

import com.tramchester.domain.*;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.StationRepository;

import java.util.List;
import java.util.Set;

public class TestStation extends Station {

    private final TransportMode mode;

    public TestStation(String id, String area, String stationName, LatLong latLong, GridPosition gridPosition, TransportMode mode) {
        super(IdFor.createId(id), area, stationName, latLong, gridPosition);
        this.mode = mode;
    }

    @Override
    public TransportMode getTransportMode() {
        return mode;
    }

    @Override
    public boolean hasPlatforms() {
        throw new RuntimeException("Use real Station");
    }

    @Override
    public List<Platform> getPlatforms() {
        throw new RuntimeException("Use real Station");
    }

    @Override
    public List<Platform> getPlatformsForRoute(Route route) {
        throw new RuntimeException("Use real Station");
    }

    @Override
    public Set<Route> getRoutes() {
        throw new RuntimeException("Use real Station");
    }

    public static Station real(StationRepository repository, HasId<Station> hasId) {
        return repository.getStationById(hasId.getId());
    }

}
