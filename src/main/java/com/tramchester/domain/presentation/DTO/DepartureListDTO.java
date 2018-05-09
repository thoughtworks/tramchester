package com.tramchester.domain.presentation.DTO;

import java.util.List;
import java.util.SortedSet;

public class DepartureListDTO {

    private SortedSet<DepartureDTO> departures;
    private List<String> notes;

    public DepartureListDTO() {
        // for deserialisation
    }

    public DepartureListDTO(SortedSet<DepartureDTO> departures, List<String> notes) {
        this.departures = departures;
        this.notes = notes;
    }

    public SortedSet<DepartureDTO> getDepartures() {
        return departures;
    }

    public List<String> getNotes() {
        return notes;
    }
}
