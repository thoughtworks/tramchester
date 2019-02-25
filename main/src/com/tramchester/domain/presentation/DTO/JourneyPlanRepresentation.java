package com.tramchester.domain.presentation.DTO;

import java.util.List;
import java.util.SortedSet;

public class JourneyPlanRepresentation {
    private SortedSet<JourneyDTO> journeys;
    private List<String> notes;

    public JourneyPlanRepresentation() {
        // deserialisation
    }

    public JourneyPlanRepresentation(SortedSet<JourneyDTO> journeys, List<String> notes) {
        this.journeys = journeys;
        this.notes = notes;
    }

    public SortedSet<JourneyDTO> getJourneys() {
        return journeys;
    }

    public List<String> getNotes() {
        return notes;
    }
}
