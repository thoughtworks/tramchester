package com.tramchester.geo;

import com.tramchester.domain.places.Station;

import java.util.List;

public class BoundingBoxWithStations extends BoundingBox {

    private final List<Station> stationsWithin;

    public BoundingBoxWithStations(BoundingBox box, List<Station> stationsWithin) {
        super(box);
        this.stationsWithin = stationsWithin;
    }

    public boolean hasStations() {
        return !stationsWithin.isEmpty();
    }

    public List<Station> getStaions() {
        return stationsWithin;
    }
}
