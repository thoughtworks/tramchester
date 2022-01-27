package com.tramchester.testSupport;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.GridPosition;

import java.util.Collections;
import java.util.Set;

public class TestNoPlatformStation extends TestStation {

    public TestNoPlatformStation(String id, String area, IdFor<NaptanArea> areaId, String stationName, LatLong latLong,
                                 GridPosition gridPosition, TransportMode mode, DataSourceID dataSourceID) {
        super(id, "", areaId, stationName, latLong, gridPosition, mode, dataSourceID);
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
