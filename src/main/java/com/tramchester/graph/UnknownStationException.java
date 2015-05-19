package com.tramchester.graph;

public class UnknownStationException extends Exception {
    private final String stationId;

    public UnknownStationException(String stationId) {
        super("Unable to find station by id " + stationId);
        this.stationId = stationId;
    }
}
