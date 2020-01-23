package com.tramchester.mappers;

import com.tramchester.domain.Journey;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.lang.String.format;

public class JourneysMapper {
    private static final Logger logger = LoggerFactory.getLogger(JourneysMapper.class);

    private TramJourneyToDTOMapper mapper;

    public JourneysMapper(TramJourneyToDTOMapper mapper) {
        this.mapper = mapper;
    }

    public SortedSet<JourneyDTO> createJourneyDTOs(Set<Journey> rawJourneys, TramServiceDate tramServiceDate) {
        logger.info("Creating journey DTOs " + rawJourneys.size());
        SortedSet<JourneyDTO> journeys = new TreeSet<>();
        rawJourneys.forEach(rawJourney -> {
            Optional<JourneyDTO> journey = mapper.createJourneyDTO(rawJourney, tramServiceDate);
            if (journey.isPresent()) {
                journeys.add(journey.get());
                logger.info("Added journey " +journey);
            } else {
                logger.warn(format("Unable to parse %s to journey", rawJourney));
            }
        });

        return journeys;
    }

}
