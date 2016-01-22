package com.tramchester.domain.presentation;

import com.tramchester.domain.Station;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;

public class JourneyPlanRepresentation {
    private final SortedSet<Journey> journeys;
    private final List<Station> stations;

    public JourneyPlanRepresentation(SortedSet<Journey> journeys, List<Station> stations) {
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
