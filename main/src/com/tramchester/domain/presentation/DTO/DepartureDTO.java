package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.mappers.serialisation.TramTimeJsonDeserializer;
import com.tramchester.mappers.serialisation.TramTimeJsonSerializer;

import java.util.Objects;

public class DepartureDTO implements Comparable<DepartureDTO> {

    // TODO Make from and destintaion StationRefDTO?
    private String from;
    private String destination;

    private String carriages;
    private String status;
    private TramTime when;
    private int wait;

    public DepartureDTO(Station from, DueTram dueTram) {
        this.from = from.getName();
        this.when = dueTram.getWhen();
        this.carriages = dueTram.getCarriages();
        this.status = dueTram.getStatus();
        this.destination = dueTram.getDestination().getName();
        this.wait = dueTram.getWait();
    }

    @SuppressWarnings("unused")
    public DepartureDTO() {
        // for deserialisation
    }

    @JsonSerialize(using = TramTimeJsonSerializer.class)
    @JsonDeserialize(using = TramTimeJsonDeserializer.class)
    public TramTime getWhen() {
        return when;
    }

    public String getFrom() {
        return from;
    }

    public String getCarriages() {
        return carriages;
    }

    public String getStatus() {
        return status;
    }

    public String getDestination() {
        return destination;
    }

    @Override
    public int compareTo(DepartureDTO other) {
        if (when.equals(other.when)) {
            // if same time use string ordering
            return destination.compareTo(other.destination);
        }
        // time ordering
        return when.compareTo(other.when);
    }

    @Override
    public String toString() {
        return "DepartureDTO{" +
                "from='" + from + '\'' +
                ", carriages='" + carriages + '\'' +
                ", status='" + status + '\'' +
                ", destination='" + destination + '\'' +
                ", when=" + when +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DepartureDTO that = (DepartureDTO) o;
        return from.equals(that.from) &&
                carriages.equals(that.carriages) &&
                status.equals(that.status) &&
                destination.equals(that.destination) &&
                when.equals(that.when);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, carriages, status, destination, when);
    }

    public int getWait() {
        return wait;
    }
}
