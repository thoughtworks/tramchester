package com.tramchester.domain;

import java.util.ArrayList;
import java.util.List;

public class Trip {
    private String tripId;
    private List<Stop> stops = new ArrayList<>();

    public Trip(String tripId) {

        this.tripId = tripId;
    }

    public String getTripId() {
        return tripId;
    }

    public List<Stop> getStops() {
        return stops;
    }

    public void addStop(Stop stop) {
        stops.add(stop);
    }
}
