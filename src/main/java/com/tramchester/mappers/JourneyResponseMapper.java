package com.tramchester.mappers;

import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.JourneyPlanRepresentation;
import com.tramchester.repository.TransportDataFromFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.lang.String.format;

public abstract class JourneyResponseMapper {
    private static final Logger logger = LoggerFactory.getLogger(JourneyResponseMapper.class);
    protected TransportDataFromFiles transportData;

    protected JourneyResponseMapper(TransportDataFromFiles transportData) {
        this.transportData = transportData;
    }

    protected abstract Journey createJourney(RawJourney rawJourney, TimeWindow window);

    public JourneyPlanRepresentation map(Set<RawJourney> journeys, TimeWindow window) throws TramchesterException {
        List<Station> stations = getStations(journeys);
        if (!stations.isEmpty()) {
            logger.info(format("Mapping journey from %s to %s with %s",
                    stations.get(0), stations.get(stations.size() - 1),
                    window));
        }
        SortedSet<Journey> decoratedJourneys = decorateJourneys(journeys, stations, window);
        return new JourneyPlanRepresentation(decoratedJourneys, stations);
    }

    protected SortedSet<Journey> decorateJourneys(Set<RawJourney> rawJourneys, List<Station> stations, TimeWindow window)
            throws TramchesterException {
        logger.info("Decorating the discovered journeys " + rawJourneys.size());
        SortedSet<Journey> journeys = new TreeSet<>();
        rawJourneys.forEach(rawJourney -> {
            logger.info("Decorating journey " + rawJourney);

            Journey journey = createJourney(rawJourney, window);
            if (journey!=null) {
                journey.setSummary(getJourneySummary(journey, stations));
                journeys.add(journey);
                logger.info("Added journey " +journey);
            } else {
                logger.warn(format("Unable to map %s to journey", rawJourney));
            }
        });
        if (rawJourneys.size()!=journeys.size()) {
            logger.warn(format("Only mapped %s out of %s journeys", journeys.size(), rawJourneys.size()));
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
            journey.forEach(raw -> {
                if (raw.getMode().equals(TransportMode.Bus) || raw.getMode().equals(TransportMode.Tram)) {
                    RawTravelStage stage = (RawTravelStage) raw;
                    stations.add(stage.getFirstStation());
                    stations.add(stage.getLastStation());
                }
            });
        }
        return stations;
    }
}
