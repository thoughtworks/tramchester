package com.tramchester.domain;


public class StationWalk {
    private Location station;
    private int cost;

    public StationWalk(Location station, int cost) {
        this.cost = cost;
        this.station = station;
    }

    public String getStationId() {
        return station.getId();
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
}
