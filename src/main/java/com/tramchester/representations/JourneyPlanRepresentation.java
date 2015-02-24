package com.tramchester.representations;

import com.tramchester.domain.Journey;
import com.tramchester.domain.Station;

import java.util.List;
import java.util.Set;

public class JourneyPlanRepresentation {
    private final List<Journey> journeys;
    private final Set<Station> stations;

    public JourneyPlanRepresentation(List<Journey> journeys, Set<Station> stations) {
        this.journeys = journeys;
        this.stations = stations;
    }

    public List<Journey> getJourneys() {
        return journeys;
    }

    public Set<Station> getStations() {
        return stations;
    }
}
