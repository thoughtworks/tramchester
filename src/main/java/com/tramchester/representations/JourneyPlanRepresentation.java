package com.tramchester.representations;

import com.tramchester.domain.Journey;
import com.tramchester.domain.Station;

import java.util.List;

public class JourneyPlanRepresentation {
    private final List<Journey> journeys;
    private final List<Station> stations;

    public JourneyPlanRepresentation(List<Journey> journeys, List<Station> stations) {
        this.journeys = journeys;
        this.stations = stations;
    }

    public List<Journey> getJourneys() {
        return journeys;
    }

    public List<Station> getStations() {
        return stations;
    }
}
