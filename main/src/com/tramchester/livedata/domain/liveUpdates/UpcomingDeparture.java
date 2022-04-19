package com.tramchester.livedata.domain.liveUpdates;

import com.tramchester.domain.Agency;
import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

public class UpcomingDeparture {

    private final LocalDate date;
    private final Duration wait;
    private final String carriages; // double/single
    private final String status; // due, arrived, etc
    private final Station displayLocation;
    private final Station destination;
    private final TramTime when;
    private final Agency agency;
    private final TransportMode mode;
    private Platform platform;

    public UpcomingDeparture(LocalDate date, Station displayLocation, Station destination, String status, Duration wait,
                             String carriages, LocalTime updateTime,
                             Agency agency, TransportMode mode) {
        this.date = date;
        this.displayLocation = displayLocation;
        this.destination = destination;
        this.status = status;
        this.wait = wait;
        this.carriages = carriages;
        this.agency = agency;
        this.mode = mode;
        this.when  = TramTime.ofHourMins(updateTime).plus(wait);
        platform = null;
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

    public TransportMode getMode() {
        return mode;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public boolean hasPlatform() {
        return platform!=null;
    }

    public Platform getPlatform() {
        return platform;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UpcomingDeparture that = (UpcomingDeparture) o;

        if (!date.equals(that.date)) return false;
        if (!wait.equals(that.wait)) return false;
        if (!carriages.equals(that.carriages)) return false;
        if (!status.equals(that.status)) return false;
        if (!displayLocation.equals(that.displayLocation)) return false;
        if (!destination.equals(that.destination)) return false;
        if (!when.equals(that.when)) return false;
        if (!agency.equals(that.agency)) return false;
        return mode == that.mode;
    }

    @Override
    public int hashCode() {
        int result = date.hashCode();
        result = 31 * result + wait.hashCode();
        result = 31 * result + carriages.hashCode();
        result = 31 * result + status.hashCode();
        result = 31 * result + displayLocation.hashCode();
        result = 31 * result + destination.hashCode();
        result = 31 * result + when.hashCode();
        result = 31 * result + agency.hashCode();
        result = 31 * result + mode.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "UpcomingDeparture{" +
                "date=" + date +
                ", wait=" + wait +
                ", carriages='" + carriages + '\'' +
                ", status='" + status + '\'' +
                ", displayLocation=" + displayLocation.getId() +
                ", destination=" + destination.getId() +
                ", when=" + when +
                ", agency=" + agency +
                ", mode=" + mode +
                ", platform=" + platform +
                '}';
    }

}
