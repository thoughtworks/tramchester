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

    public JourneyPlanRepresentation map(List<Journey> journeys, int minutesFromMidnight) {
        Set<Station> stations = getStations(journeys);
        return new JourneyPlanRepresentation(decorateJourneys(journeys, stations, minutesFromMidnight), stations);
    }

    private List<Journey> decorateJourneys(List<Journey> journeys, Set<Station> stations, int minutesFromMidnight) {
        int minutesPast = 0;
        for (Journey journey : journeys) {
            journey.setSummary(getJourneySummary(journey, stations));
            for (Stage stage : journey.getStages()) {
                stage.setServiceTimes(transportData.getTimes(stage.getServiceId(), stage.getFirstStation(), stage.getLastStation(), minutesFromMidnight + minutesPast));
                minutesPast += stage.getDuration();
            }
        }
        return journeys;
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

    private String getJourneySummary(Journey journey, Set<Station> stations) {
        if (journey.getStages().size() < 2) {
            return "Direct";
        }
        String endStopId = journey.getStages().get(0).getLastStation();
        Station station = null;
        for (Station stop : stations) {
            if (stop.getId().equals(endStopId)) {
                station = stop;
                break;
            }
        }
        return String.format("Change at %s", station.getName());
    }

}
