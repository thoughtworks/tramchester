package com.tramchester.domain.presentation;

import java.util.SortedSet;

public class JourneyPlanRepresentation {
    private final SortedSet<Journey> journeys;

    public JourneyPlanRepresentation(SortedSet<Journey> journeys) {
        this.journeys = journeys;
    }

    public SortedSet<Journey> getJourneys() {
        return journeys;
    }

}
