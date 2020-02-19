package com.tramchester.domain.input;

import com.tramchester.domain.Station;
import com.tramchester.domain.time.TramTime;

public class Stop {
    private final Station station;
    private final TramTime arrivalTime;
    private final TramTime departureTime;
    private final String stopId;
    private final int sequenceNumber;

    public Stop(String stopId, Station station, int sequenceNumber, TramTime arrivalTime, TramTime departureTime) {
        this.stopId = stopId.intern();
        this.sequenceNumber = sequenceNumber;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.station = station;
    }

    public TramTime getArrivalTime() {
        return arrivalTime;
    }

    public TramTime getDepartureTime() {
        return departureTime;
    }

    public Station getStation() {
        return station;
    }

    @Override
    public String toString() {
        return "Stop{" +
                "station=" + station +
                ", arrivalTime=" + arrivalTime +
                ", departureTime=" + departureTime +
                ", stopId='" + stopId + '\'' +
                '}';
    }

    public String getId() {
        return stopId;
    }

    public int getGetSequenceNumber() {
        return sequenceNumber;
    }
}
