package com.tramchester.domain;

import java.util.List;

public class StationClosureMessage {
    private List<String> closedStations;

    public StationClosureMessage(List<String> closedStations) {
        this.closedStations = closedStations;
    }

    public StationClosureMessage() {
    }

    public boolean isClosed() {
        return closedStations != null && closedStations.size() > 0;
    }

    public String getMessage() {
        if (closedStations.size() == 1) {
            return String.format("%s station temporary closure.", closedStations.get(0));
        } else if (closedStations.size() > 1) {
            String stations = "";
            for (String closedStation : closedStations) {
                stations += closedStation + ", ";
            }
            stations = stations.substring(0,stations.length() - 2);
            return String.format("%s stations temporary closure.", stations);
        }
        return "";
    }
}
