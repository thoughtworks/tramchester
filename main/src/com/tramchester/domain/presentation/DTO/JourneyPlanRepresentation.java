package com.tramchester.domain.presentation.DTO;

import java.util.Set;

public class JourneyPlanRepresentation {

    private Set<JourneyDTO> journeys;

    public JourneyPlanRepresentation() {
        // deserialisation
    }

    public JourneyPlanRepresentation(Set<JourneyDTO> journeys) {
        this.journeys = journeys;
    }

    public Set<JourneyDTO> getJourneys() {
        return journeys;
    }

}
