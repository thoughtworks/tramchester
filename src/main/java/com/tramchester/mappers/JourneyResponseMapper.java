package com.tramchester.mappers;

import com.tramchester.domain.*;
import com.tramchester.representations.JourneyPlanRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;

public class JourneyResponseMapper {
    private static final Logger logger = LoggerFactory.getLogger(JourneyResponseMapper.class);

    private TransportDataFromFiles transportData;

    public JourneyResponseMapper(TransportDataFromFiles transportData) {
        this.transportData = transportData;
    }

    public JourneyPlanRepresentation map(Set<Journey> journeys, int minutesFromMidnight, int maxNumberOfTrips) throws TramchesterException {
        Set<Station> stations = getStations(journeys);
        Set<Journey> decoratedJourneys = decorateJourneys(journeys, stations, minutesFromMidnight, maxNumberOfTrips);
        return new JourneyPlanRepresentation(decoratedJourneys, stations);
    }

    private Set<Journey> decorateJourneys(Set<Journey> journeys, Set<Station> stations,
                                          int originMinutesFromMidnight, int maxNumberOfTrips) throws TramchesterException {
        logger.info("Decorating the discovered journeys " + journeys.size());
        for (Journey journey : journeys) {
            int journeyClock = originMinutesFromMidnight;

            journey.setSummary(getJourneySummary(journey, stations));
            logger.info("Add services times for " + journey.toString());
            decorateJourneysUsingStage(journey, maxNumberOfTrips, journeyClock);
        }
        return journeys;
    }

    private void decorateJourneysUsingStage(Journey journey, int maxNumOfSvcTimes, int journeyClock) throws TramchesterException {
        int minNumberOfTimes = Integer.MAX_VALUE;
        for (Stage stage : journey.getStages()) {
            String serviceId = stage.getServiceId();
            logger.info(format("ServiceId: %s Journey clock is now %s ", serviceId, journeyClock));

            String firstStation = stage.getFirstStation();
            String lastStation = stage.getLastStation();
            List<ServiceTime> times = transportData.getTimes(serviceId, firstStation, lastStation, journeyClock, maxNumOfSvcTimes);
            if (times.isEmpty()) {
                String message = format("Cannot complete journey. stage '%s' service '%s' clock '%s'",
                        stage, serviceId, journeyClock);
                logger.error(message);
                throw new TramchesterException(message);
            }

            if (times.size()<minNumberOfTimes) {
                minNumberOfTimes = times.size();
            }
            logger.info(format("Found %s times for service id %s", times.size(), serviceId));
            stage.setServiceTimes(times);
            int departsAtMinutes = findEarliestDepartureTime(times);
            int duration = stage.getDuration();
            journeyClock = departsAtMinutes + duration;
            logger.info(format("Previous stage duration was %s, earliest depart is %s, new offset is %s ",
                    duration, departsAtMinutes, journeyClock));
        }
        journey.setNumberOfTimes(minNumberOfTimes);
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
        return format("Change at %s", station.getName());
    }

}
