package com.tramchester.mappers;

import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.StageWithTiming;
import com.tramchester.domain.presentation.TravelAction;
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
        logger.info(format("Mapping journey %s with window %s", journeys, window));
        SortedSet<Journey> decoratedJourneys = decorateJourneys(journeys, window);
        return new JourneyPlanRepresentation(decoratedJourneys);
    }

    protected SortedSet<Journey> decorateJourneys(Set<RawJourney> rawJourneys, TimeWindow window)
            throws TramchesterException {
        logger.info("Decorating the discovered journeys " + rawJourneys.size());
        SortedSet<Journey> journeys = new TreeSet<>();
        rawJourneys.forEach(rawJourney -> {
            logger.info("Decorating journey " + rawJourney);

            Journey journey = createJourney(rawJourney, window);
            if (journey!=null) {
                journey.setSummary(getJourneySummary(journey));
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

    private String getJourneySummary(Journey journey) {
        if (journey.getStages().size() < 2) {
            return "Direct";
        }
        Station station = journey.getStages().get(0).getLastStation();
        return format("Change at %s", station.getName());
    }

    protected TravelAction decideAction(List<StageWithTiming> stages, List<TransportStage> rawJourneyStages) {
        if (stages.isEmpty()) {
            return TravelAction.Board;
        }
        return TravelAction.Change;
    }

}
