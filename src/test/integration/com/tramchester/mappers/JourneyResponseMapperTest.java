package com.tramchester.mappers;

import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.UnknownStationException;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.graph.RouteCalculator;
import org.joda.time.LocalDate;

import java.util.Set;

import static org.junit.Assert.assertEquals;

public class JourneyResponseMapperTest {
    protected RouteCalculator routeCalculator;
    protected TramServiceDate today = new TramServiceDate(LocalDate.now());

    protected String findServiceId(String firstId, String secondId, int queryTime) throws UnknownStationException {
        Set<RawJourney> found = routeCalculator.calculateRoute(firstId, secondId, queryTime, DaysOfWeek.Monday, today);
        RawJourney rawJourney = found.stream().findFirst().get();
        RawStage rawStage = rawJourney.getStages().get(0);
        assertEquals(RawTravelStage.class, rawStage.getClass());
        return ((RawTravelStage)rawStage).getServiceId();
    }
}
