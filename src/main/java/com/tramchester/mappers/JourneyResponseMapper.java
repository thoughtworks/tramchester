package com.tramchester.mappers;

import com.tramchester.domain.*;
import com.tramchester.representations.JourneyPlanRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Int;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JourneyResponseMapper {
    private static final Logger logger = LoggerFactory.getLogger(JourneyResponseMapper.class);

    private TransportData transportData;

    public JourneyResponseMapper(TransportData transportData) {
        this.transportData = transportData;
    }

    public JourneyPlanRepresentation map(Set<Journey> journeys, int minutesFromMidnight) {
        Set<Station> stations = getStations(journeys);
        return new JourneyPlanRepresentation(decorateJourneys(journeys, stations, minutesFromMidnight), stations);
    }

    private Set<Journey> decorateJourneys(Set<Journey> journeys, Set<Station> stations, int originMinutesFromMidnight) {
        for (Journey journey : journeys) {
            int journeyClock = originMinutesFromMidnight;

            journey.setSummary(getJourneySummary(journey, stations));
            logger.info("Add services times for " + journey.toString());
            for (Stage stage : journey.getStages()) {
                logger.info(String.format("ServiceId: %s Journey clock is now %s ", stage.getServiceId(), journeyClock));

                List<ServiceTime> times = transportData.getTimes(stage.getServiceId(),
                        stage.getFirstStation(), stage.getLastStation(), journeyClock);
                stage.setServiceTimes(times);
                if (times.size() > 0) {
                    int departsAtMinutes = findEarliestDepartureTime(times);
                    int duration = stage.getDuration();
                    journeyClock = departsAtMinutes + duration;
                    logger.info(String.format("Previous stage duration was %s, earliest depart is %s, new offset is %s ",
                            duration, departsAtMinutes, journeyClock));
                } else {
                    logger.error("Cannot complete journey, no times for stage available");
                    break;
                }
            }
        }
        return journeys;
    }

    private int findEarliestDepartureTime(List<ServiceTime> times) {
        int earliest = Integer.MAX_VALUE;
        for (ServiceTime time : times) {
            if (time.getFromMidnight() < earliest) {
                earliest = time.getFromMidnight();
            }
        }
        return earliest;
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
