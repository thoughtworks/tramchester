package com.tramchester.graph;

import com.tramchester.domain.TramchesterException;

public class UnknownStationException extends TramchesterException {

    public UnknownStationException(String stationId) {
        super("Unable to find station by id " + stationId);
    }
}
