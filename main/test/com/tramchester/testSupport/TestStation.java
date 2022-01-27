package com.tramchester.testSupport;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.StationRepositoryPublic;

import java.util.HashSet;
import java.util.Set;

@Deprecated
public class TestStation extends MutableStation {

    private final TransportMode initialMode;
    private boolean platformsAdded;
    private boolean routesAdded;
    private final String rawId;

    public TestStation(String id, IdFor<NaptanArea> areaId, String stationName, LatLong latLong, GridPosition gridPosition,
                       TransportMode initialMode, DataSourceID dataSourceID) {
        super(StringIdFor.createId(id), areaId, stationName, latLong, gridPosition, dataSourceID);
        rawId = id;

        this.initialMode = initialMode;
        platformsAdded = false;
        routesAdded = false;
    }

    public static MutableStation forTest(String id, String area, String stationName, LatLong latLong, TransportMode mode, DataSourceID dataSourceID) {
        IdFor<NaptanArea> areaId = StringIdFor.createId(area);
        return new TestStation(id, areaId, stationName, latLong, CoordinateTransforms.getGridPosition(latLong), mode, dataSourceID);
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
    public void addRouteDropOff(Route dropoffFromRoute) {
        super.addRouteDropOff(dropoffFromRoute);
        routesAdded = true;
    }

    @Override
    public void addRoutePickUp(Route pickupFromRoute) {
        super.addRoutePickUp(pickupFromRoute);
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
    public Set<Route> getDropoffRoutes() {
        guardRoutesAddedIntent();
        return super.getDropoffRoutes();
    }

    @Override
    public Set<Route> getPickupRoutes() {
        guardRoutesAddedIntent();
        return super.getPickupRoutes();
    }

    public static Station real(StationRepositoryPublic repository, TestStations hasId) {
        return repository.getStationById(hasId.getId());
    }

    public IdFor<Platform> getPlatformId(String platform) {
        return StringIdFor.createId(rawId+platform);
    }
}
