package com.tramchester.domain.presentation;

import com.tramchester.domain.Station;

import java.util.Set;

public class JourneyPlanRepresentation {
    private final Set<Journey> journeys;
    private final Set<Station> stations;

    public JourneyPlanRepresentation(Set<Journey> journeys, Set<Station> stations) {
        this.journeys = journeys;
        this.stations = stations;
    }

    public Set<Journey> getJourneys() {
        return journeys;
    }

    public Set<Station> getStations() {
        return stations;
    }


}
