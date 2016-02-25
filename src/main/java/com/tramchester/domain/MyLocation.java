package com.tramchester.domain;

import com.tramchester.domain.presentation.LatLong;

public class MyLocation implements Location {

    private LatLong latLong;

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
    public double getLatitude() {
        return latLong.getLat();
    }

    @Override
    public double getLongitude() {
        return latLong.getLon();
    }

    @Override
    public boolean isTram() {
        return false;
    }
}
