package com.tramchester.unit.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.InvalidDurationException;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.graph.search.RouteCalculatorArriveBy;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Transaction;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertSame;

class RouteCalculatorArriveByTest extends EasyMockSupport {

    private RouteCalculatorArriveBy routeCalculatorArriveBy;
    private RouteCalculator routeCalculator;
    private RouteCostCalculator costCalculator;
    private int costBetweenStartDest;
    private TramchesterConfig config;
    private Transaction txn;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        txn = createStrictMock(Transaction.class);
        costCalculator = createStrictMock(RouteCostCalculator.class);
        routeCalculator = createStrictMock(RouteCalculator.class);
        config = createStrictMock(TramchesterConfig.class);
        routeCalculatorArriveBy = new RouteCalculatorArriveBy(costCalculator, routeCalculator, config);
        costBetweenStartDest = 15;
    }

    @Test
    void shouldArriveByTramNoWalk() throws InvalidDurationException {
        long maxNumberOfJourneys = 3;
        TramTime arriveByTime = TramTime.of(14,35);
        TramDate localDate = TestEnv.testDay();

        Station start = TramStations.Bury.fake();
        Station destinationId = TramStations.Cornbrook.fake();
        //TramServiceDate serviceDate = new TramServiceDate(localDate);

        Stream<Journey> journeyStream = Stream.empty();

        Duration duration = Duration.ofMinutes(15);
        EasyMock.expect(costCalculator.getAverageCostBetween(txn, start, destinationId, localDate)).andReturn(duration);
        TramTime requiredDepartTime = arriveByTime.minusMinutes(costBetweenStartDest).minusMinutes(17); // 17 = 34/2

        Set<TransportMode> modes = Collections.emptySet();
        JourneyRequest updatedWithComputedDepartTime = new JourneyRequest(localDate, requiredDepartTime, true,
                5, Duration.ofMinutes(120), maxNumberOfJourneys, modes);
        EasyMock.expect(routeCalculator.calculateRoute(txn, start, destinationId, updatedWithComputedDepartTime)).andReturn(journeyStream);
        EasyMock.expect(config.getInitialMaxWaitFor(DataSourceID.tfgm)).andReturn(Duration.ofMinutes(34));

        replayAll();
        JourneyRequest originalRequest = new JourneyRequest(localDate, arriveByTime, true, 5,
                Duration.ofMinutes(120), maxNumberOfJourneys, modes);
        Stream<Journey> result = routeCalculatorArriveBy.calculateRoute(txn, start, destinationId, originalRequest);
        verifyAll();
        assertSame(journeyStream, result);
    }

}