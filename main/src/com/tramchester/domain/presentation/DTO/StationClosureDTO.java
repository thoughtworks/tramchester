package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.mappers.serialisation.LocalDateJsonDeserializer;
import com.tramchester.mappers.serialisation.LocalDateJsonSerializer;

import java.time.LocalDate;
import java.util.List;

public class StationClosureDTO {
    private List<LocationRefDTO> stations;
    private LocalDate begin;
    private LocalDate end;
    private Boolean fullyClosed;

    public StationClosureDTO(DateRange dateRange, List<LocationRefDTO> refs, boolean fullyClosed) {
        this(dateRange.getStartDate(), dateRange.getEndDate(), refs, fullyClosed);
    }

    private StationClosureDTO(TramDate begin, TramDate end, List<LocationRefDTO> stations, boolean fullyClosed) {
        this.stations = stations;
        this.begin = begin.toLocalDate();
        this.end = end.toLocalDate();
        this.fullyClosed = fullyClosed;
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

    public Boolean getFullyClosed() {
        return fullyClosed;
    }
}
