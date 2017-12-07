package com.tramchester.domain;

import com.tramchester.domain.presentation.LatLong;

import java.util.List;

public class MyLocation implements Location {

    private LatLong latLong;

    @Override
    public String toString() {
        return "MyLocation{" +
                "latLong=" + latLong +
                '}';
    }

    public MyLocation(LatLong latLong) {
        this.latLong = latLong;
    }

    @Override
    public String getId() {
        return latLong.toString();
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
    public boolean isTram() {
        return false;
    }

    @Override
    public String getArea() {
        return "My Area";
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
