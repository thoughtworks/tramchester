package com.tramchester.domain.places;


import com.tramchester.domain.id.HasId;

import java.time.Duration;

public class StationWalk {
    private final Station station;
    private final Duration cost;

    public StationWalk(Station station, Duration cost) {
        this.cost = cost;
        this.station = station;
    }

    public Duration getCost() {
        return cost;
    }

    @Override
    public String toString() {
        return "StationWalk{" +
                "station=" + HasId.asId(station) +
                ", cost=" + cost +
                '}';
    }

    public Station getStation() {
        return station;
    }
}
