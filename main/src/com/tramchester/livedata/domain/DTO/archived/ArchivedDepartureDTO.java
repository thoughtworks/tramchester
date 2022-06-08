package com.tramchester.livedata.domain.DTO.archived;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tramchester.mappers.serialisation.LocalTimeJsonDeserializer;

import java.time.LocalDateTime;
import java.time.LocalTime;

@SuppressWarnings("unused")
@JsonIgnoreProperties({"dueTime","transportMode"})
public class ArchivedDepartureDTO  {

    @JsonDeserialize(using = LocalTimeJsonDeserializer.class)
    private LocalTime when;

    private String from;
    private String destination;
    private String carriages;
    private String status;
    private int wait;

    public ArchivedDepartureDTO() {
        // deserialization
    }

    public ArchivedDepartureDTO(String from,
                                String destination,
                                String carriages,
                                String status,
                                LocalDateTime dueTime,
                                LocalTime when,
                                Integer wait) {
        this.from = from;
        this.destination = destination;
        this.status = status;
        this.when = when;
        this.wait = wait;
        this.carriages = carriages;
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

    public int getWait() {
        return wait;
    }

    public LocalTime getWhen() {
        return when;
    }

    @Override
    public String toString() {
        return "ArchivedDepartureDTO{" +
                "when=" + when +
                ", from='" + from + '\'' +
                ", destination='" + destination + '\'' +
                ", carriages='" + carriages + '\'' +
                ", status='" + status + '\'' +
                ", wait=" + wait +
                '}';
    }


}
