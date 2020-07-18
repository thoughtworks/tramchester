package com.tramchester.domain.input;

import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.places.Station;

public class TrainStopCall extends StopCall {

    public TrainStopCall(Station station, StopTimeData stopTimeData) {
        super(station, stopTimeData);
    }

    @Override
    public String getPlatformId() {
        throw new RuntimeException("Bus stops don't have platforms");
    }
}
