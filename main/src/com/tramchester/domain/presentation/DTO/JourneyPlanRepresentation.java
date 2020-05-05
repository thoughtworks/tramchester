package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.presentation.Note;

import java.util.List;
import java.util.SortedSet;

public class JourneyPlanRepresentation {
    private SortedSet<JourneyDTO> journeys;
    private List<Note> notes;

    public JourneyPlanRepresentation() {
        // deserialisation
    }

    public JourneyPlanRepresentation(SortedSet<JourneyDTO> journeys, List<Note> notes) {
        this.journeys = journeys;
        this.notes = notes;
    }

    public SortedSet<JourneyDTO> getJourneys() {
        return journeys;
    }

    public List<Note> getNotes() {
        return notes;
    }
}
