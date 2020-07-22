package com.tramchester.domain.places;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.HasId;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Platform;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.presentation.LatLong;

import java.util.List;

public class MyLocation extends MapIdToDTOId<MyLocation> implements Location, HasId<MyLocation> {

    public static final String MY_LOCATION_PLACEHOLDER_ID = "MyLocationPlaceholderId";
    public static final IdFor<MyLocation> LocationPlaceHolder = IdFor.createId(MY_LOCATION_PLACEHOLDER_ID);

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
    public List<Platform> getPlatforms() {
        return null;
    }

    @Override
    public TransportMode getTransportMode() {
        return TransportMode.Walk;
    }
}
