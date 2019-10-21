package com.tramchester.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.presentation.LatLong;

public class MyLocationFactory {
    public static final String MY_LOCATION_PLACEHOLDER_ID = "MyLocationPlaceholderId";

    private final ObjectMapper mapper;

    public MyLocationFactory(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public MyLocation create(LatLong latLong) {
        return MyLocation.create(mapper, latLong);
    }
}
