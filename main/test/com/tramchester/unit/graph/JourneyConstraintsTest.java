package com.tramchester.unit.graph;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.JourneyConstraints;
import com.tramchester.graph.search.LowestCostsForRoutes;
import com.tramchester.repository.RunningRoutesAndServices;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.*;

public class JourneyConstraintsTest extends EasyMockSupport {

    private JourneyConstraints journeyConstraints;
    private RunningRoutesAndServices runningRoutesAndServices;
    private TestConfigWithTramMode config;
    private LowestCostsForRoutes lowestCostForDest;

    @BeforeEach
    void beforeEachTestRuns() {
        config = new TestConfigWithTramMode();

        IdSet<Station> closedStations = IdSet.singleton(TramStations.Cornbrook.getId());
        lowestCostForDest = createMock(LowestCostsForRoutes.class);
        runningRoutesAndServices = createMock(RunningRoutesAndServices.class);

        TramServiceDate date = TramServiceDate.of(TestEnv.testDay());
        TramTime queryTime = TramTime.of(11,45);
        int maxChanges = 3;
        int maxDuration = 120;
        long maxJourneys = 5;
        JourneyRequest journeyRequest = new JourneyRequest(date, queryTime, false, maxChanges, maxDuration,
                maxJourneys);
        Set<Station> endStations = Collections.singleton(TramStations.of(TramStations.Bury));

        journeyConstraints = new JourneyConstraints(config, runningRoutesAndServices, journeyRequest,
               closedStations, endStations, lowestCostForDest);
    }

    @Test
    void shouldCarryBasicParams() {
        assertEquals(config.getMaxWalkingConnections(), journeyConstraints.getMaxWalkingConnections());
        assertEquals(config.getMaxNeighbourConnections(), journeyConstraints.getMaxNeighbourConnections());
        assertEquals(120, journeyConstraints.getMaxJourneyDuration());
    }

    @Test
    void shouldHaveProvidedLowestCostCalc() {
        assertSame(lowestCostForDest, journeyConstraints.getFewestChangesCalculator());
    }

    @Test
    void shouldCheckIfRouteRunning() {
        Route route = TestEnv.getTramTestRoute();
        TramTime time = TramTime.of(10,11);

        EasyMock.expect(runningRoutesAndServices.isRouteRunning(route.getId())).andReturn(true);

        replayAll();
        boolean result = journeyConstraints.isUnavailable(route, time);
        verifyAll();

        assertFalse(result);
    }

    @Test
    void shouldCheckIfServiceRunning() {
        IdFor<Service> serviceId = StringIdFor.createId("serviceA");

        EasyMock.expect(runningRoutesAndServices.isRunning(serviceId)).andReturn(true);

        replayAll();
        boolean result = journeyConstraints.isRunning(serviceId);
        verifyAll();

        assertTrue(result);
    }

    @Test
    void shouldCheckIfClosedStation() {
        Station stationA = TramStations.of(TramStations.Anchorage);
        Station stationB = TramStations.of(TramStations.Cornbrook);

        replayAll();
        assertFalse(journeyConstraints.isClosed(stationA));
        assertTrue(journeyConstraints.isClosed(stationB));
        verifyAll();
    }

    @Test
    void shouldGetEndStations() {

        Set<Station> result = journeyConstraints.getEndStations();
        assertEquals(1, result.size());
        assertTrue(result.contains(TramStations.of(TramStations.Bury)));
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
