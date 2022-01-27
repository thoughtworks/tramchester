package com.tramchester.domain.places;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.GraphPropertyKey;

import java.util.Collections;
import java.util.Set;

public class MyLocation implements Location<MyLocation> {

    public static final String MY_LOCATION_PLACEHOLDER_ID = "MyLocationPlaceholderId";
    private static final IdFor<MyLocation> LocationPlaceHolder = StringIdFor.createId(MY_LOCATION_PLACEHOLDER_ID);

    private final LatLong latLong;

    public static MyLocation create(LatLong latLong) {
        return new MyLocation(latLong);
    }

    @Override
    public String toString() {
        return "MyLocation{" + latLong + '}';
    }

    public MyLocation(LatLong latLong) {
        this.latLong = latLong;
    }

    @Override
    public IdFor<MyLocation> getId() {
        return LocationPlaceHolder;
    }

    @Override
    public String getName() {
        return "My Location";
    }

    @Override
    public LatLong getLatLong() {
        return latLong;
    }

    @Override
    public GridPosition getGridPosition() {
        return CoordinateTransforms.getGridPosition(latLong);
    }

    @Override
    public IdFor<NaptanArea> getAreaId() {
        return StringIdFor.invalid();
    }

    @Override
    public boolean hasPlatforms() {
        return false;
    }

    @Override
    public Set<Platform> getPlatforms() {
        return null;
    }

    @Override
    public Set<TransportMode> getTransportModes() {
        return Collections.singleton(TransportMode.Walk);
    }

    @Override
    public LocationType getLocationType() {
        return LocationType.Mobile;
    }

    @Override
    public DataSourceID getDataSourceID() {
        return DataSourceID.internal;
    }

    @Override
    public boolean hasPickup() {
        return true;
    }

    @Override
    public boolean hasDropoff() {
        return true;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public GraphPropertyKey getProp() {
        return GraphPropertyKey.WALK_ID;
    }

    public String forDTO()  {
        return getId().forDTO();
    }

    public static boolean isUserLocation(String text) {
        return MY_LOCATION_PLACEHOLDER_ID.equals(text);
    }
}
