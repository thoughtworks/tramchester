package com.tramchester.domain.exceptions;

public class UnknownStationException extends TramchesterException {

    public UnknownStationException(String stationId) {
        super("Unable to find station by id " + stationId);
    }
}
