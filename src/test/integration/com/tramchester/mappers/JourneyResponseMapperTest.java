package com.tramchester.mappers;

import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.RouteCalculator;
import org.joda.time.LocalDate;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class JourneyResponseMapperTest {
    protected RouteCalculator routeCalculator;

    protected String findServiceId(String firstId, String secondId, LocalDate when, int queryTime) throws TramchesterException {
        List<Integer> queryTimes = Arrays.asList(new Integer[]{queryTime});

        Set<RawJourney> found = routeCalculator.calculateRoute(firstId, secondId, queryTimes, new TramServiceDate(when));
        RawJourney rawJourney = found.stream().findFirst().get();
        TransportStage rawStage = rawJourney.getStages().get(0);
        assertEquals(RawVehicleStage.class, rawStage.getClass());
        return ((RawVehicleStage)rawStage).getServiceId();
    }
}
