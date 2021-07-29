package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.StationClosure;
import com.tramchester.domain.places.Station;
import com.tramchester.mappers.serialisation.LocalDateJsonDeserializer;
import com.tramchester.mappers.serialisation.LocalDateJsonSerializer;

import java.time.LocalDate;
import java.util.List;

public class StationClosureDTO {
    private List<StationRefDTO> stations;
    private LocalDate begin;
    private LocalDate end;

    public StationClosureDTO(LocalDate begin, LocalDate end, List<StationRefDTO> stations) {
        this.stations = stations;
        this.begin = begin;
        this.end = end;
    }

    @SuppressWarnings("unused")
    public StationClosureDTO() {
        // deserialisation
    }

    public List<StationRefDTO> getStations() {
        return stations;
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
