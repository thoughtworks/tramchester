package com.tramchester.domain;

import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.places.Station;

import java.util.Objects;

public class ClosedStation {
    private final Station station;
    private final DateRange dateRange;
    private final boolean fullyClosed;

    public ClosedStation(Station station, DateRange dateRange, boolean fullyClosed) {

        this.station = station;
        this.dateRange = dateRange;
        this.fullyClosed = fullyClosed;
    }

    public Station getStation() {
        return station;
    }

    public DateRange getDateRange() {
        return dateRange;
    }

    public boolean isFullyClosed() {
        return fullyClosed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClosedStation that = (ClosedStation) o;
        return fullyClosed == that.fullyClosed && station.equals(that.station) && dateRange.equals(that.dateRange);
    }

    @Override
    public int hashCode() {
        return Objects.hash(station, dateRange, fullyClosed);
    }

    @Override
    public String toString() {
        return "ClosedStation{" +
                "station=" + station.getId() +
                ", dateRange=" + dateRange +
                ", fullyClosed=" + fullyClosed +
                '}';
    }

}
