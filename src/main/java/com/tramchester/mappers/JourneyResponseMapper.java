package com.tramchester.mappers;

import com.tramchester.domain.Journey;
import com.tramchester.domain.Station;
import com.tramchester.domain.TransportData;
import com.tramchester.representations.JourneyPlanRepresentation;

import java.util.List;

public class JourneyResponseMapper {

    private TransportData transportData;

    public JourneyResponseMapper(TransportData transportData) {
        this.transportData = transportData;
    }

    public JourneyPlanRepresentation map(List<Journey> journeys) {
        List<Station> stations = 
        return new JourneyPlanRepresentation(journeys, null);
    }
}
