package com.tramchester.livedata.domain.liveUpdates;

import com.tramchester.domain.Agency;
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
    private final Agency agency;

    public UpcomingDeparture(Station displayLocation, Station destination, String status, Duration wait,
                             String carriages, LocalTime updateTime,
                             Agency agency) {
        this.displayLocation = displayLocation;
        this.destination = destination;
        this.status = status;
        this.wait = wait;
        this.carriages = carriages;
        this.agency = agency;
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

    public Agency getAgency() {
        return agency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UpcomingDeparture that = (UpcomingDeparture) o;

        if (!wait.equals(that.wait)) return false;
        if (!carriages.equals(that.carriages)) return false;
        if (!status.equals(that.status)) return false;
        if (!displayLocation.equals(that.displayLocation)) return false;
        if (!destination.equals(that.destination)) return false;
        if (!when.equals(that.when)) return false;
        return agency.equals(that.agency);
    }

    @Override
    public int hashCode() {
        int result = wait.hashCode();
        result = 31 * result + carriages.hashCode();
        result = 31 * result + status.hashCode();
        result = 31 * result + displayLocation.hashCode();
        result = 31 * result + destination.hashCode();
        result = 31 * result + when.hashCode();
        result = 31 * result + agency.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "UpcomingDeparture{" +
                "wait=" + wait +
                ", carriages='" + carriages + '\'' +
                ", status='" + status + '\'' +
                ", displayLocation=" + displayLocation +
                ", destination=" + destination +
                ", when=" + when +
                ", agency=" + agency +
                '}';
    }

}
