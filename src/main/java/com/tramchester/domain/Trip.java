package com.tramchester.domain;

import java.util.ArrayList;
import java.util.List;

public class Trip {
    private String tripId;
    private String headSign;
    private List<Stop> stops = new ArrayList<>();

    public Trip(String tripId, String headSign) {
        this.tripId = tripId;
        this.headSign = headSign;
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

    public String getHeadSign() {
        return headSign;
    }
}
