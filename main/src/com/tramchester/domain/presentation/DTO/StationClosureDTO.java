package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.StationClosure;

import java.time.LocalDate;

public class StationClosureDTO {
    private StationRefDTO station;
    private LocalDate begin;
    private LocalDate end;

    public StationClosureDTO(StationClosure stationClosure) {
        this.station = new StationRefDTO(stationClosure.getStation());
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

    public LocalDate getBegin() {
        return begin;
    }

    public LocalDate getEnd() {
        return end;
    }
}
