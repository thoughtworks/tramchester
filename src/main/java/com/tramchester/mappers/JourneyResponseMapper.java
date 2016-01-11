package com.tramchester.mappers;

import com.tramchester.domain.RawJourney;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.JourneyPlanRepresentation;

import java.util.Set;

public interface JourneyResponseMapper {
    JourneyPlanRepresentation map(Set<RawJourney> journeys, int minutesFromMidnight, int maxNumberOfTrips) throws TramchesterException;
}
