package com.tramchester.unit.graph;

import com.tramchester.TestConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.StationWalk;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.graph.RouteCalculatorArriveBy;
import com.tramchester.graph.TramRouteReachable;
import com.tramchester.integration.Stations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class RouteCalculatorArriveByTest extends EasyMockSupport {

    private RouteCalculatorArriveBy routeCalculatorArriveBy;
    private RouteCalculator routeCalculator;
    private TramRouteReachable routeReachable;
    private int costBetweenStartDest;

    @Before
    public void onceBeforeEachTestRuns() {
        routeReachable = createStrictMock(TramRouteReachable.class);
        routeCalculator = createStrictMock(RouteCalculator.class);
        routeCalculatorArriveBy = new RouteCalculatorArriveBy(routeReachable, routeCalculator);
        costBetweenStartDest = 15;
    }

    @Test
    public void shouldArriveByTramNoWalk() {
        TramTime arriveBy = TramTime.of(14,35);
        LocalDate localDate = TestConfig.nextTuesday(0);

        String startId = Stations.Bury.getId();
        String destinationId = Stations.Cornbrook.getId();
        TramServiceDate serviceDate = TramServiceDate.of(localDate);

        Stream<Journey> journeyStream = Stream.empty();

        EasyMock.expect(routeReachable.getApproxCostBetween(startId, destinationId)).andReturn(costBetweenStartDest);
        EasyMock.expect(routeCalculator.calculateRoute(startId, destinationId, arriveBy.minusMinutes(costBetweenStartDest), serviceDate)).andReturn(journeyStream);

        replayAll();
        Stream<Journey> result = routeCalculatorArriveBy.calculateRoute(startId, destinationId, arriveBy, serviceDate);
        verifyAll();
        assertTrue(journeyStream==result);
    }

}