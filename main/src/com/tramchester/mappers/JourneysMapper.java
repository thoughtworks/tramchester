package com.tramchester.mappers;

import com.tramchester.domain.RawJourney;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.factory.JourneyDTOFactory;
import com.tramchester.domain.presentation.Journey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.lang.String.format;

public class JourneysMapper {
    private static final Logger logger = LoggerFactory.getLogger(JourneysMapper.class);

    private TramJourneyResponseWithTimesMapper mapper;

    public JourneysMapper(TramJourneyResponseWithTimesMapper mapper) {
        this.mapper = mapper;
    }

    public SortedSet<JourneyDTO> map(JourneyDTOFactory factory, Set<RawJourney> journeys) {
        logger.info(format("Mapping journey %s", journeys));
        return decorateJourneys(factory, journeys);
    }

    private SortedSet<JourneyDTO> decorateJourneys(JourneyDTOFactory factory, Set<RawJourney> rawJourneys) {
        logger.info("Decorating the discovered journeys " + rawJourneys.size());
        SortedSet<JourneyDTO> journeys = new TreeSet<>();
        rawJourneys.forEach(rawJourney -> {
            logger.info("Decorating journey " + rawJourney);

            Optional<Journey> journey = mapper.createJourney(rawJourney);
            if (journey.isPresent()) {
                try {
                    journeys.add(factory.build(journey.get()));
                    logger.info("Added journey " +journey);
                } catch (TramchesterException e) {
                    logger.warn(format("Unable to parse %s to journey", rawJourney),e);
                }
            } else {
                logger.warn(format("Unable to parse %s to journey", rawJourney));
            }
        });

        return journeys;
    }


}
