package com.tramchester.mappers;

import com.tramchester.domain.Journey;
import com.tramchester.domain.Stage;
import com.tramchester.domain.Station;
import com.tramchester.domain.TransportData;
import com.tramchester.representations.JourneyPlanRepresentation;

import java.util.ArrayList;
import java.util.List;

public class JourneyResponseMapper {

    private TransportData transportData;

    public JourneyResponseMapper(TransportData transportData) {
        this.transportData = transportData;
    }

    public JourneyPlanRepresentation map(List<Journey> journeys) {
        List<Station> stations = getStations(journeys);
        return new JourneyPlanRepresentation(journeys, stations);
    }

    private List<Station> getStations(List<Journey> journeys) {
        List<Station> stations = new ArrayList<>();
        for (Journey journey : journeys) {
            List<Stage> stages = journey.getStages();
            for (Stage stage : stages) {
                stations.add(transportData.getStation(stage.getFirstStation()));
            }
        }
        return stations;
    }
}
