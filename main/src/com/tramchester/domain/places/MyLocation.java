package com.tramchester.domain.places;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphPropertyKey;

import java.util.Set;

public class MyLocation extends MapIdToDTOId<MyLocation> implements Location<MyLocation> {

    public static final String MY_LOCATION_PLACEHOLDER_ID = "MyLocationPlaceholderId";
    private static final IdFor<MyLocation> LocationPlaceHolder = IdFor.createId(MY_LOCATION_PLACEHOLDER_ID);

    private final String area;
    private final LatLong latLong;

    public static MyLocation create(ObjectMapper mapper, LatLong latLong) {
        try {
            return new MyLocation(mapper.writeValueAsString(latLong), latLong);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "MyLocation{" + area + '}';
    }

    public MyLocation(String area, LatLong latLong) {
        this.area = area;
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
    public String getArea() {
        return area;
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
    public TransportMode getTransportMode() {
        return TransportMode.Walk;
    }

    @Override
    public LocationType getLocationType() {
        return LocationType.Mobile;
    }

    @Override
    public GraphPropertyKey getProp() {
        return GraphPropertyKey.WALK_ID;
    }
}
