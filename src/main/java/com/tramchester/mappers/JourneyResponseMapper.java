package com.tramchester.mappers;

import com.tramchester.domain.Journey;
import com.tramchester.domain.Stage;
import com.tramchester.domain.Station;
import com.tramchester.domain.TransportData;
import com.tramchester.representations.JourneyPlanRepresentation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JourneyResponseMapper {

    private TransportData transportData;

    public JourneyResponseMapper(TransportData transportData) {
        this.transportData = transportData;
    }

    public JourneyPlanRepresentation map(List<Journey> journeys) {
        Set<Station> stations = getStations(journeys);
        return new JourneyPlanRepresentation(journeys, stations);
    }

    private Set<Station> getStations(List<Journey> journeys) {
        Set<Station> stations = new HashSet<>();
        for (Journey journey : journeys) {
            List<Stage> stages = journey.getStages();
            for (Stage stage : stages) {
                stations.add(transportData.getStation(stage.getFirstStation()));
                stations.add(transportData.getStation(stage.getLastStation()));
            }
        }
        return stations;
    }
}
