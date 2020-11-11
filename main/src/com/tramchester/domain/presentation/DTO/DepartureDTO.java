package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.joda.deser.LocalDateTimeDeserializer;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.places.Station;
import com.tramchester.mappers.serialisation.LocalDateTimeJsonSerializer;
import com.tramchester.mappers.serialisation.LocalTimeJsonDeserializer;
import com.tramchester.mappers.serialisation.LocalTimeJsonSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

public class DepartureDTO implements Comparable<DepartureDTO> {

    // TODO Make from and destintaion StationRefDTO?
    private String from;
    private String destination;

    private String carriages;
    private String status;
    private LocalDateTime when;
    private int wait;

    public DepartureDTO(Station from, DueTram dueTram, LocalDate queryDate) {
        this.from = from.getName();
        this.when = dueTram.getWhen().toDate(queryDate);
        this.carriages = dueTram.getCarriages();
        this.status = dueTram.getStatus();
        this.destination = dueTram.getDestination().getName();
        this.wait = dueTram.getWait();
    }

    @SuppressWarnings("unused")
    public DepartureDTO() {
        // for deserialisation
    }

    @JsonSerialize(using = LocalDateTimeJsonSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    public LocalDateTime getDueTime() {
        return when;
    }

    @JsonProperty("when")
    @JsonSerialize(using = LocalTimeJsonSerializer.class)
    @JsonDeserialize(using = LocalTimeJsonDeserializer.class)
    public LocalTime getWhenForLiveUpload() {
        // for keeping upload of live data consistent
        return when.toLocalTime();
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
