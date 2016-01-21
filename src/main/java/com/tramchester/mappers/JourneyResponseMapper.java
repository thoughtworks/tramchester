package com.tramchester.mappers;

import com.tramchester.domain.RawJourney;
import com.tramchester.domain.Station;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.JourneyPlanRepresentation;
import com.tramchester.repository.TransportDataFromFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;

public abstract class JourneyResponseMapper {
    private static final Logger logger = LoggerFactory.getLogger(JourneyResponseMapper.class);
    protected TransportDataFromFiles transportData;

    protected JourneyResponseMapper(TransportDataFromFiles transportData) {
        this.transportData = transportData;
    }

    protected abstract Journey createJourney(RawJourney rawJourney, int maxNumberOfTrips, int journeyClock);

    public JourneyPlanRepresentation map(Set<RawJourney> journeys, int minutesFromMidnight, int maxNumberOfTrips) throws TramchesterException {
        List<Station> stations = getStations(journeys);
        if (!stations.isEmpty()) {
            logger.info(format("Mapping journey from %s to %s at %s with max trips %s",
                    stations.get(0), stations.get(stations.size() - 1),
                    minutesFromMidnight, maxNumberOfTrips));
        }
        Set<Journey> decoratedJourneys = decorateJourneys(journeys, stations, minutesFromMidnight, maxNumberOfTrips);
        return new JourneyPlanRepresentation(decoratedJourneys, stations);
    }

    protected Set<Journey> decorateJourneys(Set<RawJourney> rawJourneys, List<Station> stations, int originMinutesFromMidnight, int maxNumberOfTrips) throws TramchesterException {
        logger.info("Decorating the discovered journeys " + rawJourneys.size());
        Set<Journey> journeys = new HashSet<>();
        rawJourneys.forEach(rawJourney -> {
            logger.info("Decorating journey " + rawJourney);
            int journeyClock = originMinutesFromMidnight;

            Journey journey = createJourney(rawJourney, maxNumberOfTrips, journeyClock);
            if (journey!=null) {
                journey.setSummary(getJourneySummary(journey, stations));
                journeys.add(journey);
            } else {
                logger.warn(format("Unable to map %s to journey", rawJourney));
            }
        });
        if (rawJourneys.size()!=journeys.size()) {
            throw new TramchesterException(format("Only mapped %s out of %s journeys", journeys.size(), rawJourneys.size()));
        }
        return journeys;
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

    private List<Station> getStations(Set<RawJourney> journeys) {
        List<Station> stations = new LinkedList<>();
        for (RawJourney journey : journeys) {
            journey.forEach(stage -> {
                stations.add(transportData.getStation(stage.getFirstStation()));
                stations.add(transportData.getStation(stage.getLastStation()));
            });
        }
        return stations;
    }
}
