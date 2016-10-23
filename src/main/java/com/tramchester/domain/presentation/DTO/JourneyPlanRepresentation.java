package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.presentation.DTO.JourneyDTO;

import java.util.SortedSet;

public class JourneyPlanRepresentation {
    private final SortedSet<JourneyDTO> journeys;

    public JourneyPlanRepresentation(SortedSet<JourneyDTO> journeys) {
        this.journeys = journeys;
    }

    public SortedSet<JourneyDTO> getJourneys() {
        return journeys;
    }

}
