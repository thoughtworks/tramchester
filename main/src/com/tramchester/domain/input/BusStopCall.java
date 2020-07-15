package com.tramchester.domain.input;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;

public class BusStopCall extends StopCall {
    public BusStopCall(Station station, byte sequenceNumber, TramTime arrivalTime, TramTime departureTime) {
        super(station, arrivalTime, departureTime, sequenceNumber);
    }

    @Override
    public String getPlatformId() {
        throw new RuntimeException("Bus stops don't have platforms");
    }
}
