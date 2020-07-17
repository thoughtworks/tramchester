package com.tramchester.domain.places;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.Platform;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.presentation.LatLong;

import java.util.List;

public class MyLocation implements Location {

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

    private MyLocation(String area, LatLong latLong) {
        this.area = area;
        this.latLong = latLong;
    }

    @Override
    public String getId() {
        return MyLocationFactory.MY_LOCATION_PLACEHOLDER_ID;
    }

    @Override
    public String getName() {
        return "My Location";
    }

    @Override
    public LatLong getLatLong() {
        return latLong;
    }

    @Deprecated
    @Override
    public boolean isTram() {
        return false;
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
    public List<Platform> getPlatforms() {
        return null;
    }

    @Override
    public TransportMode getTransportMode() {
        return TransportMode.Walk;
    }
}
