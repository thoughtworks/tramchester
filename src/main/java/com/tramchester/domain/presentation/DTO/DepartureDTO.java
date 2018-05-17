package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.mappers.TramTimeJsonDeserializer;
import com.tramchester.mappers.TramTimeJsonSerializer;
import org.joda.time.LocalTime;

public class DepartureDTO implements Comparable<DepartureDTO> {

    private String from;
    private TramTime when;
    private String carriages;
    private String status;
    private String destination;

    public DepartureDTO(String from, DueTram dueTram) {
        this.from = from;
        this.when = dueTram.getWhen();
        this.carriages = dueTram.getCarriages();
        this.status = dueTram.getStatus();
        this.destination = dueTram.getDestination();
    }

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
}
