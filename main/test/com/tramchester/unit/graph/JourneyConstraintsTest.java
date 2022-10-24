package com.tramchester.unit.graph;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.JourneyConstraints;
import com.tramchester.graph.search.LowestCostsForDestRoutes;
import com.tramchester.repository.RunningRoutesAndServices;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.TestEnv.assertMinutesEquals;
import static org.junit.jupiter.api.Assertions.*;

public class JourneyConstraintsTest extends EasyMockSupport {

    private JourneyConstraints journeyConstraints;
    private RunningRoutesAndServices.FilterForDate filterForDate;
    private TestConfigWithTramMode config;
    private LowestCostsForDestRoutes lowestCostForDest;

    @BeforeEach
    void beforeEachTestRuns() {
        config = new TestConfigWithTramMode();

        IdSet<Station> closedStations = IdSet.singleton(TramStations.Cornbrook.getId());

        lowestCostForDest = createMock(LowestCostsForDestRoutes.class);
        filterForDate = createMock(RunningRoutesAndServices.FilterForDate.class);

        LocationSet endStations = LocationSet.singleton(TramStations.Bury.fake());

        final Duration maxJourneyDuration = Duration.ofMinutes(config.getMaxJourneyDuration());
        journeyConstraints = new JourneyConstraints(config, filterForDate,
                closedStations, endStations, lowestCostForDest, maxJourneyDuration);
    }

    @Test
    void shouldCarryBasicParams() {
        assertEquals(config.getMaxWalkingConnections(), journeyConstraints.getMaxWalkingConnections());
//        assertEquals(config.getMaxNeighbourConnections(), journeyConstraints.getMaxNumberWalkingConnections());
        assertMinutesEquals(config.getMaxJourneyDuration(), journeyConstraints.getMaxJourneyDuration());
    }

    @Test
    void shouldHaveProvidedLowestCostCalc() {
        assertSame(lowestCostForDest, journeyConstraints.getFewestChangesCalculator());
    }

    @Test
    void shouldCheckIfRouteRunning() {
        Route route = TestEnv.getTramTestRoute();
        TramTime time = TramTime.of(10,11);

        EasyMock.expect(filterForDate.isRouteRunning(route.getId(), false)).andReturn(true);

        replayAll();
        boolean result = journeyConstraints.isUnavailable(route, time);
        verifyAll();

        assertFalse(result);
    }

    @Test
    void shouldCheckIfServiceRunningOnDate() {
        IdFor<Service> serviceId = StringIdFor.createId("serviceA");

        TramTime visitTime = TramTime.of(13,56);

        EasyMock.expect(filterForDate.isServiceRunningByDate(serviceId, false)).andReturn(true);

        replayAll();
        boolean result = journeyConstraints.isRunningOnDate(serviceId, visitTime);
        verifyAll();

        assertTrue(result);
    }

    @Test
    void shouldCheckIfServiceRunningOnDateNextDay() {
        IdFor<Service> serviceId = StringIdFor.createId("serviceA");

        TramTime visitTime = TramTime.nextDay(13,56);

        EasyMock.expect(filterForDate.isServiceRunningByDate(serviceId, true)).andReturn(true);

        replayAll();
        boolean result = journeyConstraints.isRunningOnDate(serviceId, visitTime);
        verifyAll();

        assertTrue(result);
    }

    @Test
    void shouldCheckIfServiceRunningAtTime() {
        IdFor<Service> serviceId = StringIdFor.createId("serviceA");

        TramTime visitTime = TramTime.of(13,56);

        int maxWait = config.getMaxWait();

        EasyMock.expect(filterForDate.isServiceRunningByTime(serviceId, visitTime, maxWait)).andReturn(true);

        replayAll();
        boolean result = journeyConstraints.isRunningAtTime(serviceId, visitTime, maxWait);
        verifyAll();

        assertTrue(result);
    }

    @Test
    void shouldCheckIfClosedStation() {
        Station stationA = TramStations.Anchorage.fake();
        Station stationB = TramStations.Cornbrook.fake();

        replayAll();
        assertFalse(journeyConstraints.isClosed(stationA));
        assertTrue(journeyConstraints.isClosed(stationB));
        verifyAll();
    }

    @Test
    void shouldGetEndStations() {

        LocationSet result = journeyConstraints.getEndStations();
        assertEquals(1, result.size());
        assertTrue(result.contains(TramStations.Bury.fake()));
    }

    @Test
    void shouldCheckLongestPath() {
        assertEquals(400, journeyConstraints.getMaxPathLength());
    }

    private static class TestConfigWithTramMode extends TestConfig {
        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return Collections.emptyList();
        }

        @Override
        public Set<TransportMode> getTransportModes() {
            return Collections.singleton(Tram);
        }
    }
}
