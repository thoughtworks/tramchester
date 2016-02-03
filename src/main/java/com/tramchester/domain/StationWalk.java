package com.tramchester.domain;


public class StationWalk {
    private String id;
    private int cost;

    public StationWalk(Station station, int cost) {
        this.cost = cost;
        this.id = station.getId();
    }

    public String getId() {
        return id;
    }

    public int getCost() {
        return cost;
    }
}
