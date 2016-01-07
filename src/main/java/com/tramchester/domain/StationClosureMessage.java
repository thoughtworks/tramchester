package com.tramchester.domain;

public class StationClosureMessage {
    private ClosedStations closedStations;

    public StationClosureMessage(ClosedStations closedStations) {
        this.closedStations = closedStations;
    }

    public boolean isClosed() {
        return closedStations != null && closedStations.isNotEmpty();
    }

    public String getMessage() {
        if (closedStations.singleClosure()) {
            return String.format("%s station temporary closure.", closedStations.firstClosure());
        } else if (closedStations.multipleClosures()) {

            return String.format("%s stations temporary closure.", closedStations.closureList());
        }
        return "";
    }
}
