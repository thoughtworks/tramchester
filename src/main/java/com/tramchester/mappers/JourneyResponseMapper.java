package com.tramchester.mappers;

import com.tramchester.domain.*;
import com.tramchester.representations.JourneyPlanRepresentation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JourneyResponseMapper {

    private TransportData transportData;

    public JourneyResponseMapper(TransportData transportData) {
        this.transportData = transportData;
    }

    public JourneyPlanRepresentation map(Set<Journey> journeys, int minutesFromMidnight) {
        Set<Station> stations = getStations(journeys);
        return new JourneyPlanRepresentation(decorateJourneys(journeys, stations, minutesFromMidnight), stations);
    }

    private Set<Journey> decorateJourneys(Set<Journey> journeys, Set<Station> stations, int minutesFromMidnight) {
        int minutesPast = 0;
        for (Journey journey : journeys) {
            journey.setSummary(getJourneySummary(journey, stations));
            for (Stage stage : journey.getStages()) {
                List<ServiceTime> times = transportData.getTimes(stage.getServiceId(), stage.getFirstStation(), stage.getLastStation(), minutesFromMidnight + minutesPast);
                stage.setServiceTimes(times);
                minutesPast += stage.getDuration();
            }
        }
        return journeys;
    }

    private Set<Station> getStations(Set<Journey> journeys) {
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
