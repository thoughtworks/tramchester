package com.tramchester.livedata.domain.liveUpdates;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;

import java.time.LocalTime;
import java.util.Objects;

public class DueTram {

    private final int wait;
    private final String carriages; // double/single
    private final String status; // due, arrived, etc
    private final Station destination;
    private final TramTime when;

    public DueTram(Station destination, String status, int wait, String carriages, LocalTime updateTime) {
        this.destination = destination;
        this.status = status;
        this.wait = wait;
        this.carriages = carriages;
        this.when  = TramTime.of(updateTime).plusMinutes(wait);
    }

    public Station getDestination() {
        return destination;
    }

    public String getStatus() {
        return status;
    }

    public int getWait() {
        return wait;
    }

    public String getCarriages() {
        return carriages;
    }

    public TramTime getWhen() {
        return when;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DueTram dueTram = (DueTram) o;
        return wait == dueTram.wait &&
                Objects.equals(carriages, dueTram.carriages) &&
                Objects.equals(status, dueTram.status) &&
                Objects.equals(destination, dueTram.destination) &&
                Objects.equals(when, dueTram.when);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wait, carriages, status, destination, when);
    }

    @Override
    public String toString() {
        return "DueTram{" +
                "status='" + status + '\'' +
                ", destination='" + HasId.asId(destination) + '\'' +
                ", wait=" + wait +
                ", carriages='" + carriages + '\'' +
                '}';
    }
}
