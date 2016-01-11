package com.tramchester.mappers;

import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.ServiceTime;
import com.tramchester.domain.presentation.Stage;
import com.tramchester.repository.TransportDataFromFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;

public class TramJourneyResponseMapper implements JourneyResponseMapper {
    private static final Logger logger = LoggerFactory.getLogger(TramJourneyResponseMapper.class);

    private TransportDataFromFiles transportData;

    public TramJourneyResponseMapper(TransportDataFromFiles transportData) {
        this.transportData = transportData;
    }

    public JourneyPlanRepresentation map(Set<RawJourney> journeys, int minutesFromMidnight, int maxNumberOfTrips) throws TramchesterException {
        List<Station> stations = getStations(journeys);
        logger.info(format("Mapping journey from %s to %s at %s with max trips %s",
                stations.get(0), stations.get(stations.size()-1),
                minutesFromMidnight, maxNumberOfTrips));
        Set<Journey> decoratedJourneys = decorateJourneys(journeys, stations, minutesFromMidnight, maxNumberOfTrips);
        return new JourneyPlanRepresentation(decoratedJourneys, stations);
    }

    private Set<Journey> decorateJourneys(Set<RawJourney> rawJourneys, List<Station> stations,
                                          int originMinutesFromMidnight, int maxNumberOfTrips) throws TramchesterException {
        logger.info("Decorating the discovered journeys " + rawJourneys.size());
        Set<Journey> journeys = new HashSet<>();
        rawJourneys.forEach(rawJourney -> {
            logger.info("Decorating journey " + rawJourney);
            int journeyClock = originMinutesFromMidnight;

            Journey journey = decorateJourneysUsingStage(rawJourney, maxNumberOfTrips, journeyClock);
            if (journey!=null) {
                journey.setSummary(getJourneySummary(journey, stations));
                journeys.add(journey);
            } else {
                logger.warn(format("Unable to map %s to journey", rawJourney));
            }
        });
//        for (Journey journey : rawJourneys) {
//            logger.info("Decorating journey " + journey);
//            int journeyClock = originMinutesFromMidnight;
//
//            journey.setSummary(getJourneySummary(journey, stations));
//            decorateJourneysUsingStage(journey, maxNumberOfTrips, journeyClock);
//        }
        if (rawJourneys.size()!=journeys.size()) {
            throw new TramchesterException(format("Only mapped %s out of %s journeys", journeys.size(), rawJourneys.size()));
        }
        return journeys;
    }

    private Journey decorateJourneysUsingStage(RawJourney rawJourney, int maxNumOfSvcTimes, int journeyClock) {
        int minNumberOfTimes = Integer.MAX_VALUE;
        List<Stage> stages = new LinkedList<>();
        for (RawStage rawStage : rawJourney.getStages()) {
            String serviceId = rawStage.getServiceId();
            logger.info(format("ServiceId: %s Journey clock is now %s ", serviceId, journeyClock));

            String firstStation = rawStage.getFirstStation();
            String lastStation = rawStage.getLastStation();
            List<ServiceTime> times = transportData.getTimes(serviceId, firstStation, lastStation, journeyClock, maxNumOfSvcTimes);
            if (times.isEmpty()) {
                String message = format("Cannot complete journey. stage '%s' service '%s' clock '%s'",
                        rawStage, serviceId, journeyClock);
                logger.error(message);
                return null;
            }

            if (times.size()<minNumberOfTimes) {
                minNumberOfTimes = times.size();
            }
            logger.info(format("Found %s times for service id %s", times.size(), serviceId));
            Stage stage = new Stage(rawStage);
            stage.setServiceTimes(times);
            stages.add(stage);
            int departsAtMinutes = findEarliestDepartureTime(times);
            int duration = stage.getDuration();
            journeyClock = departsAtMinutes + duration;
            logger.info(format("Previous stage duration was %s, earliest depart is %s, new offset is %s ",
                    duration, departsAtMinutes, journeyClock));
        }
        Journey journey = new Journey(stages);
        journey.setNumberOfTimes(minNumberOfTimes);
        return journey;
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

    private List<Station> getStations(Set<RawJourney> journeys) {
        List<Station> stations = new LinkedList<>();
        for (RawJourney journey : journeys) {
            journey.forEach(stage -> {
                stations.add(transportData.getStation(stage.getFirstStation()));
                stations.add(transportData.getStation(stage.getLastStation()));});
        }
        return stations;
    }

    private String getJourneySummary(Journey journey, List<Station> stations) {
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
