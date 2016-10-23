package com.tramchester.domain.presentation.DTO;

import java.util.SortedSet;

public class JourneyPlanRepresentation {
    private SortedSet<JourneyDTO> journeys;

    public JourneyPlanRepresentation(SortedSet<JourneyDTO> journeys) {
        this.journeys = journeys;
    }

    public SortedSet<JourneyDTO> getJourneys() {
        return journeys;
    }

    public JourneyPlanRepresentation() {
        // deserialisation
    }

}
