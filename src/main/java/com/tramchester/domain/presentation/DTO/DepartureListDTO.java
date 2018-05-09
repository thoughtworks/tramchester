package com.tramchester.domain.presentation.DTO;

import java.util.SortedSet;

public class DepartureListDTO {

    private SortedSet<DepartureDTO> departures;

    public DepartureListDTO() {
        // for deserialisation
    }

    public DepartureListDTO(SortedSet<DepartureDTO> departures) {
        this.departures = departures;
    }

    public SortedSet<DepartureDTO> getDepartures() {
        return departures;
    }
}
