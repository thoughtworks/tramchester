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

    public boolean isAfter(int minutesFromMidnight, String stationId) {
        for (Stop stop : stops) {
            if (stop.getStation().getId().equals(stationId) && stop.getMinutesFromMidnight() > minutesFromMidnight) {
                return true;
            }
        }
        return false;
    }

    public Stop getStop(String stationId) {
        for (Stop stop : stops) {
            if (stop.getStation().getId().equals(stationId)) {
                return stop;
            }
        }
        return null;
    }
}
