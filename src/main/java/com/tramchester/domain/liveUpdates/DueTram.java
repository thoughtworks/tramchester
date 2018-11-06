package com.tramchester.domain.liveUpdates;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.TramTime;
import com.tramchester.mappers.serialisation.TramTimeJsonDeserializer;
import com.tramchester.mappers.serialisation.TramTimeJsonSerializer;

import java.time.LocalTime;
import java.util.Objects;

public class DueTram {

    private int wait;

    private String carriages;
    private String status;
    private String destination;
    private TramTime when;

    public DueTram() {
        // deserialisation
    }

    public DueTram(String destination, String status, int wait, String carriages, LocalTime updateTime) {
        this.destination = destination;
        this.status = status;
        this.wait = wait;
        this.carriages = carriages;
        this.when  = TramTime.of(updateTime.plusMinutes(wait)); //.plusMinutes(wait);
    }

    public String getDestination() {
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

    @JsonSerialize(using = TramTimeJsonSerializer.class)
    @JsonDeserialize(using = TramTimeJsonDeserializer.class)
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
                ", destination='" + destination + '\'' +
                ", wait=" + wait +
                ", carriages='" + carriages + '\'' +
                '}';
    }
}
