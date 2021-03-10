package com.tramchester.domain.time;

import com.tramchester.domain.places.Station;

public class StationTime {
    private final Station station;
    private final TramTime time;

    public StationTime(Station station, TramTime time) {
        this.station = station;
        this.time = time;
    }

    public static StationTime of(Station station, TramTime time) {
        return new StationTime(station, time);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StationTime that = (StationTime) o;

        if (!station.equals(that.station)) return false;
        return time.equals(that.time);
    }

    @Override
    public int hashCode() {
        int result = station.hashCode();
        result = 31 * result + time.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "StationTime{" +
                "station=" + station +
                ", time=" + time +
                '}';
    }
}
