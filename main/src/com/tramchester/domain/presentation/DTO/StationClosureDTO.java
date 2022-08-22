package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.mappers.serialisation.LocalDateJsonDeserializer;
import com.tramchester.mappers.serialisation.LocalDateJsonSerializer;

import java.time.LocalDate;
import java.util.List;

public class StationClosureDTO {
    private List<LocationRefDTO> stations;
    private LocalDate begin;
    private LocalDate end;

    public StationClosureDTO(TramDate begin, TramDate end, List<LocationRefDTO> stations) {
        this.stations = stations;
        this.begin = begin.toLocalDate();
        this.end = end.toLocalDate();
    }

    @SuppressWarnings("unused")
    public StationClosureDTO() {
        // deserialisation
    }

    public List<LocationRefDTO> getStations() {
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
