package com.tramchester.mappers;

import com.tramchester.domain.RawJourney;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.WalkingStage;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.TransportStage;
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

    protected abstract Optional<JourneyDTO> createJourney(TramServiceDate queryDate, RawJourney rawJourney, int withinMins);

    public SortedSet<JourneyDTO> map(TramServiceDate queryDate, Set<RawJourney> journeys, int withinMins) throws TramchesterException {
        logger.info(format("Mapping journey %s with window %s", journeys, withinMins));
        SortedSet<JourneyDTO> decoratedJourneys = decorateJourneys(queryDate, journeys, withinMins);
        return decoratedJourneys;
    }

    protected SortedSet<JourneyDTO> decorateJourneys(TramServiceDate queryDate, Set<RawJourney> rawJourneys, int withinMins)
            throws TramchesterException {
        logger.info("Decorating the discovered journeys " + rawJourneys.size());
        SortedSet<JourneyDTO> journeys = new TreeSet<>();
        rawJourneys.forEach(rawJourney -> {
            logger.info("Decorating journey " + rawJourney);

            Optional<JourneyDTO> journey = createJourney(queryDate, rawJourney, withinMins);
            if (journey.isPresent()) {
                journeys.add(journey.get());
                logger.info("Added journey " +journey);
            } else {
                logger.warn(format("Unable to map %s to journey", rawJourney));
            }
        });

        return journeys;
    }

    protected TravelAction decideAction(List<TransportStage> stagesSoFar) {
        if (stagesSoFar.isEmpty()) {
            return TravelAction.Board;
        }
        if ((stagesSoFar.get(stagesSoFar.size()-1) instanceof WalkingStage)) {
            return TravelAction.Board;
        }
        return TravelAction.Change;
    }

}
