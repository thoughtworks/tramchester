package com.tramchester.domain.places;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.presentation.LatLong;

import javax.inject.Inject;

public class MyLocationFactory {

    private final ObjectMapper mapper;

    @Inject
    public MyLocationFactory(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public MyLocation create(LatLong latLong) {
        return MyLocation.create(mapper, latLong);
    }
}
