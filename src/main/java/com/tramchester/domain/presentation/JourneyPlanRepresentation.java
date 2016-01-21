package com.tramchester.domain.presentation;

import com.tramchester.domain.Station;

import java.util.List;
import java.util.Set;

public class JourneyPlanRepresentation {
    private final Set<Journey> journeys;
    private final List<Station> stations;

    public JourneyPlanRepresentation(Set<Journey> journeys, List<Station> stations) {
        this.journeys = journeys;
        this.stations = stations;
    }

    public Set<Journey> getJourneys() {
        return journeys;
    }

    // TODO is this even used in the .js front end?
    public List<Station> getStations() {
        return stations;
    }


}
