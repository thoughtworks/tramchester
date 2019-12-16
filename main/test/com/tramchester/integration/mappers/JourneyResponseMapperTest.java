package com.tramchester.integration.mappers;

import com.tramchester.domain.*;
import com.tramchester.graph.RouteCalculator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class JourneyResponseMapperTest {
    protected RouteCalculator routeCalculator;

    // used to find a valid service id only
    protected String findServiceId(String firstId, String secondId, LocalDate when, TramTime queryTime) {
        List<TramTime> queryTimes = Collections.singletonList(queryTime);

        Stream<RawJourney> found = routeCalculator.calculateRoute(firstId, secondId, queryTimes, new TramServiceDate(when), RouteCalculator.MAX_NUM_GRAPH_PATHS);
        RawJourney rawJourney = found.findFirst().get();
        RawStage rawStage = rawJourney.getStages().get(0);
        assertEquals(RawVehicleStage.class, rawStage.getClass());

        RawVehicleStage rawVehicleStage = (RawVehicleStage) rawStage;
        return rawVehicleStage.getServiceId();
    }
}
