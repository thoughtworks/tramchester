package com.tramchester.testSupport;

import com.tramchester.domain.*;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationBuilder;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.StationRepositoryPublic;

import java.util.HashSet;
import java.util.Set;

public class TestStation extends MutableStation {

    private final TransportMode initialMode;
    private boolean platformsAdded;
    private boolean routesAdded;

    public TestStation(String id, String area, String stationName, LatLong latLong, GridPosition gridPosition, TransportMode initialMode, DataSourceID dataSourceID) {
        super(StringIdFor.createId(id), area, stationName, latLong, gridPosition, dataSourceID);
        this.initialMode = initialMode;
        platformsAdded = false;
        routesAdded = false;
    }

    public static MutableStation forTest(String id, String area, String stationName, LatLong latLong, TransportMode mode, DataSourceID dataSourceID) {
        return new TestStation(id, area, stationName, latLong, CoordinateTransforms.getGridPosition(latLong), mode, dataSourceID);
    }

    @Deprecated
    @Override
    public StationBuilder getBuilder() {
        return new Builder(this);
    }

    private void guardPlatformsAddedIntent() {
        if (!platformsAdded) {
            throw new RuntimeException("No platforms for test station " + getName());
        }
    }

    private void guardRoutesAddedIntent() {
        if (!routesAdded) {
            throw new RuntimeException("No routes for test station " + getName());
        }
    }

    @Override
    public void addPlatform(Platform platform) {
        super.addPlatform(platform);
        platformsAdded = true;
    }

    @Override
    public void addRoute(Route route) {
        super.addRoute(route);
        routesAdded = true;
    }

    @Override
    public Set<TransportMode> getTransportModes() {
        Set<TransportMode> result = new HashSet<>();
        result.add(initialMode);
        if (routesAdded) {
           result.addAll(super.getTransportModes());
        }
        return result;
    }

    @Override
    public boolean hasPlatforms() {
        guardPlatformsAddedIntent();
        return super.hasPlatforms();
    }

    @Override
    public Set<Platform> getPlatforms() {
        guardPlatformsAddedIntent();
        return super.getPlatforms();
    }

    @Override
    public Set<Platform> getPlatformsForRoute(Route route) {
        throw new RuntimeException("Use real Station");
    }

    @Override
    public Set<Route> getRoutes() {
        guardRoutesAddedIntent();
        return super.getRoutes();
    }

    public static Station real(StationRepositoryPublic repository, TestStations hasId) {
        return repository.getStationById(hasId.getId());
    }

    public static class Builder implements StationBuilder {

        private final TestStation testStation;

        private Builder(TestStation testStation) {

            this.testStation = testStation;
        }

        @Override
        public void addRoute(Route route) {
            testStation.addRoute(route);
        }

        @Override
        public void addPlatform(Platform platform) {
            testStation.addPlatform(platform);
        }
    }

}
