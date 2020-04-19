package com.tramchester.domain.places;

import com.tramchester.domain.Platform;
import com.tramchester.domain.presentation.LatLong;

import java.util.List;

public class PostcodeLocation implements Location {

    private final LatLong LatLong;
    private final String id;

    public PostcodeLocation(com.tramchester.domain.presentation.LatLong latLong, String id) {
        LatLong = latLong;
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public LatLong getLatLong() {
        return LatLong;
    }

    @Override
    public boolean isTram() {
        return false;
    }

    @Override
    public String getArea() {
        return null;
    }

    @Override
    public boolean hasPlatforms() {
        return false;
    }

    @Override
    public List<Platform> getPlatforms() {
        return null;
    }
}
