package com.tramchester.domain;


import com.tramchester.config.TramchesterConfig;

import java.util.List;

public class ClosedStations {
    private List<String> stations;

    // called via pico-container & Dependencies
    // TODO refactor tests to use this constructor
    public ClosedStations(TramchesterConfig config) {
        this(config.getClosedStations());
    }

    // for testing
    public ClosedStations(List<String> stations) {
        this.stations = stations;
    }

    public boolean isNotEmpty() {
        return !stations.isEmpty();
    }

    public boolean singleClosure() {
        return stations.size() == 1;
    }

    public boolean multipleClosures() {
        return stations.size() > 1;
    }

    public String firstClosure() {
        return stations.get(0);
    }

    public String closureList() {
        StringBuilder list = new StringBuilder();
        for (String closedStation : stations) {
            if (list.length()>0) {
                list.append(", ");
            }
            list.append(closedStation);
        }
       return list.toString();
    }

    public boolean contains(String stationName) {
        return stations.contains(stationName);
    }
}
