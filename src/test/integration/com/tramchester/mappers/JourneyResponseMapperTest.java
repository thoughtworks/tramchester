package com.tramchester.mappers;

import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.RouteCalculator;
import org.joda.time.LocalDate;

import java.util.Set;

import static org.junit.Assert.assertEquals;

public class JourneyResponseMapperTest {
    protected RouteCalculator routeCalculator;

    protected String findServiceId(String firstId, String secondId, LocalDate when, int queryTime) throws TramchesterException {
        Set<RawJourney> found = routeCalculator.calculateRoute(firstId, secondId, queryTime, new TramServiceDate(when));
        RawJourney rawJourney = found.stream().findFirst().get();
        TransportStage rawStage = rawJourney.getStages().get(0);
        assertEquals(RawVehicleStage.class, rawStage.getClass());
        return ((RawVehicleStage)rawStage).getServiceId();
    }
}
