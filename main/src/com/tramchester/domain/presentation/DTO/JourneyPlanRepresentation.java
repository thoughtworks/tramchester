package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.presentation.Note;

import java.util.List;
import java.util.Set;

public class JourneyPlanRepresentation {
    private Set<JourneyDTO> journeys;
    private List<Note> notes;

    public JourneyPlanRepresentation() {
        // deserialisation
    }

    public JourneyPlanRepresentation(Set<JourneyDTO> journeys, List<Note> notes) {
        this.journeys = journeys;
        this.notes = notes;
    }

    public Set<JourneyDTO> getJourneys() {
        return journeys;
    }

    public List<Note> getNotes() {
        return notes;
    }
}
