package com.tramchester.unit;

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
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.StationRepositoryPublic;
import com.tramchester.testSupport.TestStations;
import com.tramchester.testSupport.reference.StationHelper;
import com.tramchester.testSupport.reference.TramStations;

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

    @Deprecated
    public static MutableStation forTest(String id, String area, String stationName, LatLong latLong, TransportMode mode, DataSourceID dataSourceID) {
        return StationHelper.forTest(id, area, stationName, latLong, mode, dataSourceID);
//        IdFor<NaptanArea> areaId = StringIdFor.createId(area);
//        return new MutableStation(Station.createId(id), areaId, stationName, latLong, CoordinateTransforms.getGridPosition(latLong), dataSourceID);
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

    /***
     * unsafe, use createFor or getFrom
     * @param enumValue the test station
     * @return The actual station in the enum
     */
    @Deprecated
    public static MutableStation of(TramStations enumValue) {
        return enumValue.fake();
//        GridPosition grid = CoordinateTransforms.getGridPosition(enumValue.getLatLong());
//
//        IdFor<NaptanArea> areaId = IdFor.invalid();
//        return new TestStation( enumValue.getRawId(), areaId, enumValue.getName(), enumValue.getLatLong(), grid, TransportMode.Tram, DataSourceID.tfgm);
    }

    @Override
    public MutableStation addPlatform(Platform platform) {
        super.addPlatform(platform);
        platformsAdded = true;
        return this;
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
