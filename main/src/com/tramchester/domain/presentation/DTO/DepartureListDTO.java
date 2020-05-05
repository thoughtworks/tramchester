package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.presentation.Note;

import java.util.List;
import java.util.SortedSet;

public class DepartureListDTO {

    private SortedSet<DepartureDTO> departures;
    private List<Note> notes;

    public DepartureListDTO() {
        // for deserialisation
    }

    public DepartureListDTO(SortedSet<DepartureDTO> departures, List<Note> notes) {
        this.departures = departures;
        this.notes = notes;
    }

    public SortedSet<DepartureDTO> getDepartures() {
        return departures;
    }

    public List<Note> getNotes() {
        return notes;
    }
}
