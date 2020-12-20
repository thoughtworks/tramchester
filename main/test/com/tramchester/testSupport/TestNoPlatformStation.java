package com.tramchester.testSupport;

import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.GridPosition;

import java.util.Collections;
import java.util.Set;

public class TestNoPlatformStation extends TestStation {

    public TestNoPlatformStation(String id, String area, String stationName, LatLong latLong, GridPosition gridPosition, TransportMode mode) {
        super(id, area, stationName, latLong, gridPosition, mode);
    }

    @Override
    public void addPlatform(Platform platform) {
        throw new RuntimeException("Can not add platforms to this kind of station");
    }

    @Override
    public boolean hasPlatforms() {
        return false;
    }

    @Override
    public Set<Platform> getPlatforms() {
        return Collections.emptySet();
    }

    @Override
    public Set<Platform> getPlatformsForRoute(Route route) {
        return Collections.emptySet();
    }

}
