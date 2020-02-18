package com.tramchester.mappers;

import com.tramchester.domain.Journey;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

import static java.lang.String.format;

public class JourneysMapper {
    private static final Logger logger = LoggerFactory.getLogger(JourneysMapper.class);

    private TramJourneyToDTOMapper mapper;

    public JourneysMapper(TramJourneyToDTOMapper mapper) {
        this.mapper = mapper;
    }

    public SortedSet<JourneyDTO> createJourneyDTOs(Stream<Journey> rawJourneys, TramServiceDate tramServiceDate, long limit) {
        logger.info("Creating journey DTOs");
        SortedSet<JourneyDTO> journeys = new TreeSet<>();

        // note: sort, then limit
        rawJourneys.map(rawJourney -> mapper.createJourneyDTO(rawJourney, tramServiceDate)).
                sorted(JourneyDTO::compareTo).
                limit(limit).
                forEachOrdered(journeys::add);

        return journeys;
    }

}
