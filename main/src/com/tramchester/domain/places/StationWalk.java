package com.tramchester.domain.places;


import com.tramchester.domain.places.Station;

public class StationWalk {
    private Station station;
    private int cost;

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
                "station=" + station +
                ", cost=" + cost +
                '}';
    }

    public Station getStation() {
        return station;
    }
}
