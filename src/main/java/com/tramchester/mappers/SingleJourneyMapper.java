package com.tramchester.mappers;

import com.tramchester.domain.RawJourney;
import com.tramchester.domain.presentation.Journey;

import java.util.Optional;

public interface SingleJourneyMapper {
    Optional<Journey> createJourney(RawJourney rawJourney, int withinMins);

}
