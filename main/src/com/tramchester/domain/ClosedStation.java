package com.tramchester.domain;

import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;

import java.util.Objects;
import java.util.Set;

public class ClosedStation {
    private final Station station;
    private final DateRange dateRange;
    private final boolean fullyClosed;
    private final Set<Station> nearbyOpenStations;

    public ClosedStation(Station station, DateRange dateRange, boolean fullyClosed, Set<Station> nearbyOpenStations) {

        this.station = station;
        this.dateRange = dateRange;
        this.fullyClosed = fullyClosed;
        this.nearbyOpenStations = nearbyOpenStations;
    }

    public Station getStation() {
        return station;
    }

    public IdFor<Station> getStationId() {
        return station.getId();
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

    /***
     * @return Nearby stations that are available during the date range of this station closure
     */
    public Set<Station> getNearbyOpenStations() {
        return nearbyOpenStations;
    }
}
