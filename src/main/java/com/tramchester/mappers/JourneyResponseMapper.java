package com.tramchester.mappers;

import com.tramchester.domain.Journey;
import com.tramchester.representations.JourneyPlanRepresentation;

import java.util.List;

public class JourneyResponseMapper {
    public JourneyPlanRepresentation map(List<Journey> journeys) {
        return new JourneyPlanRepresentation(journeys, null);
    }
}
