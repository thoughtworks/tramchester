package com.tramchester.livedata.domain.liveUpdates;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;

import java.time.Duration;
import java.time.LocalTime;

public class UpcomingDeparture {

    private final Duration wait;
    private final String carriages; // double/single
    private final String status; // due, arrived, etc
    private final Station displayLocation;
    private final Station destination;
    private final TramTime when;

    public UpcomingDeparture(Station displayLocation,
                             Station destination, String status, Duration wait, String carriages, LocalTime updateTime) {
        this.displayLocation = displayLocation;
        this.destination = destination;
        this.status = status;
        this.wait = wait;
        this.carriages = carriages;
        this.when  = TramTime.ofHourMins(updateTime).plus(wait);
    }

    public Station getDestination() {
        return destination;
    }

    public String getStatus() {
        return status;
    }

    public Duration getWait() {
        return wait;
    }

    public String getCarriages() {
        return carriages;
    }

    public TramTime getWhen() {
        return when;
    }

    public Station getDisplayLocation() {
        return displayLocation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UpcomingDeparture dueTram = (UpcomingDeparture) o;

        if (!wait.equals(dueTram.wait)) return false;
        if (!carriages.equals(dueTram.carriages)) return false;
        if (!status.equals(dueTram.status)) return false;
        if (!displayLocation.equals(dueTram.displayLocation)) return false;
        if (!destination.equals(dueTram.destination)) return false;
        return when.equals(dueTram.when);
    }

    @Override
    public int hashCode() {
        int result = wait.hashCode();
        result = 31 * result + carriages.hashCode();
        result = 31 * result + status.hashCode();
        result = 31 * result + displayLocation.hashCode();
        result = 31 * result + destination.hashCode();
        result = 31 * result + when.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DueTram{" +
                "wait=" + wait +
                ", carriages='" + carriages + '\'' +
                ", status='" + status + '\'' +
                ", displayLocation=" + displayLocation +
                ", destination=" + destination +
                ", when=" + when +
                '}';
    }

}
