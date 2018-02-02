package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.mappers.LocalTimeJsonDeserializer;
import com.tramchester.mappers.LocalTimeJsonSerializer;
import org.joda.time.LocalTime;

public class DepartureDTO implements Comparable<DepartureDTO> {

    private String from;
    private LocalTime when;
    private String carriages;
    private String status;
    private String destination;

    public DepartureDTO(String from, DueTram dueTram) {
        this.from = from;
        this.when = dueTram.getWhen().toLocalTime();
        this.carriages = dueTram.getCarriages();
        this.status = dueTram.getStatus();
        this.destination = dueTram.getDestination();
    }

    public DepartureDTO() {
        // for deserialisation
    }

    @JsonSerialize(using = LocalTimeJsonSerializer.class)
    @JsonDeserialize(using = LocalTimeJsonDeserializer.class)
    public LocalTime getWhen() {
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
        return when.compareTo(other.when);
    }
}
