package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.StationClosure;
import com.tramchester.domain.places.Station;
import com.tramchester.mappers.serialisation.LocalDateJsonDeserializer;
import com.tramchester.mappers.serialisation.LocalDateJsonSerializer;

import java.time.LocalDate;

public class StationClosureDTO {
    private StationRefDTO station;
    private LocalDate begin;
    private LocalDate end;

    public StationClosureDTO(StationClosure stationClosure, Station closedStation) {
        this.station = new StationRefDTO(closedStation);
        this.begin = stationClosure.getBegin();
        this.end = stationClosure.getEnd();
    }

    @SuppressWarnings("unused")
    public StationClosureDTO() {
        // deserialisation
    }

    public StationRefDTO getStation() {
        return station;
    }

    @JsonSerialize(using = LocalDateJsonSerializer.class)
    @JsonDeserialize(using = LocalDateJsonDeserializer.class)
    public LocalDate getBegin() {
        return begin;
    }

    @JsonSerialize(using = LocalDateJsonSerializer.class)
    @JsonDeserialize(using = LocalDateJsonDeserializer.class)
    public LocalDate getEnd() {
        return end;
    }
}
