package com.tramchester.mappers;

import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.RawJourney;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.exceptions.UnknownStationException;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.graph.RouteCalculator;
import org.joda.time.LocalDate;

import java.util.Set;

public class JourneyResponseMapperTest {
    protected RouteCalculator routeCalculator;
    protected TramServiceDate today = new TramServiceDate(LocalDate.now());

    protected String findServiceId(String firstId, String secondId, int queryTime) throws UnknownStationException {
        Set<RawJourney> found = routeCalculator.calculateRoute(firstId, secondId, queryTime, DaysOfWeek.Monday, today);
        return found.stream().findFirst().get().getFirstServiceId();
    }
}
