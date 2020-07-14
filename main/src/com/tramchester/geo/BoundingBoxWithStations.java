package com.tramchester.geo;

import com.tramchester.domain.places.Station;

import java.util.Set;

public class BoundingBoxWithStations extends BoundingBox {

    private final Set<Station> stationsWithin;

    public BoundingBoxWithStations(BoundingBox box, Set<Station> stationsWithin) {
        super(box);
        this.stationsWithin = stationsWithin;
    }

    public boolean hasStations() {
        return !stationsWithin.isEmpty();
    }

    public Set<Station> getStaions() {
        return stationsWithin;
    }
}
