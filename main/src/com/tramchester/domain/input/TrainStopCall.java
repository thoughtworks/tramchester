package com.tramchester.domain.input;

import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Station;

public class TrainStopCall extends StopCall {

    public TrainStopCall(Station station, StopTimeData stopTimeData) {
        super(station, stopTimeData);
    }

    @Override
    public IdFor<Platform> getPlatformId() {
        throw new RuntimeException("Bus stops don't have platforms");
    }

    @Override
    public String toString() {
        return "TrainStopCall{} " + super.toString();
    }

    @Override
    public boolean hasPlatfrom() {
        return false;
    }
}
