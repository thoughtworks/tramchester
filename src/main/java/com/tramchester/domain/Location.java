package com.tramchester.domain;

public interface Location {
    String getId();

    String getName();

    double getLatitude();

    double getLongitude();

    boolean isTram();
}
