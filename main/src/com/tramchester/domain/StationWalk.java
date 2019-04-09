package com.tramchester.domain;


public class StationWalk {
    private String stationId;
    private int cost;

    public StationWalk(Location station, int cost) {
        this.cost = cost;
        this.stationId = station.getId();
    }

    public String getStationId() {
        return stationId;
    }

    public int getCost() {
        return cost;
    }
}
