package com.tramchester.domain.places;

public enum LocationType {
    Station(false),
    Postcode(true),
    MyLocation(true),
    StationGroup(false);

    private final boolean walk;

    LocationType(boolean walk) {
        this.walk = walk;
    }

    public boolean isWalk() {
        return walk;
    }
}
