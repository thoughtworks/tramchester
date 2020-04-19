package com.tramchester.domain.places;


import com.tramchester.domain.HasId;

public class StationWalk {
    private final Station station;
    private final int cost;

    public StationWalk(Station station, int cost) {
        this.cost = cost;
        this.station = station;
    }

    public int getCost() {
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
