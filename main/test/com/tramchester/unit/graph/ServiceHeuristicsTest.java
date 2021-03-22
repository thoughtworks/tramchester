package com.tramchester.unit.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.Service;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.CachedNodeOperations;
import com.tramchester.graph.NodeContentsRepository;
import com.tramchester.graph.search.*;
import com.tramchester.graph.search.states.HowIGotHere;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.ReachabilityRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Node;

import java.time.LocalTime;
import java.util.Collections;
import java.util.Set;

import static com.tramchester.testSupport.reference.TramStations.Bury;
import static com.tramchester.testSupport.reference.TramStations.Shudehill;
import static org.junit.jupiter.api.Assertions.*;

class ServiceHeuristicsTest extends EasyMockSupport {

    private static final int MAX_WAIT = 30;
    private static final int MAX_NUM_CHANGES = 5;
    private final Set<Station> endStations = Collections.singleton(TramStations.of(TramStations.Deansgate));

    private final TramchesterConfig config30MinsWait = new NeedMaxWaitConfig(MAX_WAIT);
    private NodeContentsRepository nodeOperations;
    private HowIGotHere path;
    private ReachabilityRepository reachabilityRepository;
    private StationRepository stationRepository;
    private ProvidesLocalNow providesLocalNow;
    private StringIdFor<Service> serviceIdA;
    private StringIdFor<Service> serviceIdB;
    private JourneyConstraints journeyConstraints;
    private int maxJourneyDuration;
    private final int numChanges = 3;

    @BeforeEach
    void beforeEachTestRuns() {
        maxJourneyDuration = config30MinsWait.getMaxJourneyDuration();
        providesLocalNow = new ProvidesLocalNow();
        serviceIdA = StringIdFor.createId("serviceIdA");
        serviceIdB = StringIdFor.createId("serviceIdB");

        nodeOperations = createMock(CachedNodeOperations.class);
        path = createMock(HowIGotHere.class);
        reachabilityRepository = createMock(ReachabilityRepository.class);
        stationRepository = createMock(StationRepository.class);

        int maxPathLength = 400;
        journeyConstraints = createMock(JourneyConstraints.class);
        EasyMock.expect(journeyConstraints.getMaxWait()).andStubReturn(MAX_WAIT);
        EasyMock.expect(journeyConstraints.getMaxPathLength()).andStubReturn(maxPathLength);
        //EasyMock.expect(journeyConstraints.getIsTramOnlyDestinations()).andStubReturn(true);
        EasyMock.expect(journeyConstraints.getEndStations()).andStubReturn(endStations);
        EasyMock.expect(journeyConstraints.getMaxJourneyDuration()).andStubReturn(maxJourneyDuration);
    }

    @NotNull
    private JourneyRequest getJourneyRequest(TramTime queryTime) {
        return new JourneyRequest(new TramServiceDate(TestEnv.nextSaturday()), queryTime, false,
                MAX_NUM_CHANGES, maxJourneyDuration);
    }

    @Test
    void shouldCheckNodeBasedOnServiceId() {
        TramTime queryTime = TramTime.of(8,1);
        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow, numChanges);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations,
                reachabilityRepository,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        EasyMock.expect(journeyConstraints.isRunning(serviceIdA)).andReturn(true);
        EasyMock.expect(journeyConstraints.isRunning(serviceIdB)).andReturn(false);

        Node node = createMock(Node.class);
        EasyMock.expect(nodeOperations.getServiceId(node)).andReturn(serviceIdA);
        EasyMock.expect(nodeOperations.getServiceId(node)).andReturn(serviceIdB);

        replayAll();
        ServiceReason result = serviceHeuristics.checkServiceDate(node, path, reasons);
        assertTrue(result.isValid());

        result = serviceHeuristics.checkServiceDate(node, path, reasons);
        assertEquals(ServiceReason.ReasonCode.NotOnQueryDate, result.getReasonCode());
        verifyAll();
    }

    @Test
    void shouldCheckNodeOpenStation() {
        TramTime queryTime = TramTime.of(8,1);
        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow, numChanges);

        RouteStation routeStationA = new RouteStation(TramStations.of(Bury), TestEnv.getTramTestRoute());
        RouteStation routeStationB = new RouteStation(TramStations.of(Shudehill), TestEnv.getTramTestRoute());

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations,
                reachabilityRepository,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        EasyMock.expect(journeyConstraints.isClosed(TramStations.of(Bury))).andReturn(false);
        EasyMock.expect(journeyConstraints.isClosed(TramStations.of(Shudehill))).andReturn(true);

        Node node = createMock(Node.class);
        EasyMock.expect(node.getProperty("route_station_id")).andReturn(routeStationA.getId().getGraphId());
        EasyMock.expect(stationRepository.getRouteStationById(routeStationA.getId())).andReturn(routeStationA);
        EasyMock.expect(node.getProperty("route_station_id")).andReturn(routeStationB.getId().getGraphId());
        EasyMock.expect(stationRepository.getRouteStationById(routeStationB.getId())).andReturn(routeStationB);

        replayAll();
        ServiceReason result = serviceHeuristics.checkStationOpen(node, path, reasons);
        assertTrue(result.isValid());

        result = serviceHeuristics.checkStationOpen(node, path, reasons);
        assertEquals(ServiceReason.ReasonCode.StationClosed, result.getReasonCode());
        verifyAll();

    }

    @Test
    void shouldBeInterestedInCorrectHours() {
        TramTime queryTime = TramTime.of(9,1);

        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow, numChanges);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, reachabilityRepository,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        // querytime + costSoFar + maxWait (for board) = latest time could arrive here
        // querytime + costSoFar + 0 = earlier time could arrive here

        int costSoFar = 58; // 9.59
        TramTime elapsed = queryTime.plusMinutes(costSoFar);
        Node node = createMock(Node.class);

        EasyMock.expect(nodeOperations.getHour(node)).andReturn(8);
        EasyMock.expect(nodeOperations.getHour(node)).andReturn(9);
        EasyMock.expect(nodeOperations.getHour(node)).andReturn(10);
        EasyMock.expect(nodeOperations.getHour(node)).andReturn(11);

        replayAll();
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());
        verifyAll();
    }

    @Test
    void shouldBeInterestedInCorrectHoursCrossesNextHour() {
        TramTime queryTime = TramTime.of(7,0);
        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow, numChanges);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, reachabilityRepository,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        TramTime elapsed = TramTime.of(10,29);
        Node node = createMock(Node.class);

        EasyMock.expect(nodeOperations.getHour(node)).andReturn(8);
        EasyMock.expect(nodeOperations.getHour(node)).andReturn(9);
        EasyMock.expect(nodeOperations.getHour(node)).andReturn(10);
        EasyMock.expect(nodeOperations.getHour(node)).andReturn(11);

        replayAll();
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());
        verifyAll();
    }

    @Test
    void shouldBeInterestedInCorrectHoursPriorToMidnight() {
        TramTime queryTime = TramTime.of(23,10);
        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow, numChanges);


        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, reachabilityRepository,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        int costSoFar = 15;  // 23.25
        TramTime elapsed = queryTime.plusMinutes(costSoFar);

        Node node = createMock(Node.class);

        EasyMock.expect(nodeOperations.getHour(node)).andReturn(22);
        EasyMock.expect(nodeOperations.getHour(node)).andReturn(23);
        EasyMock.expect(nodeOperations.getHour(node)).andReturn(0);
        EasyMock.expect(nodeOperations.getHour(node)).andReturn(1);

        replayAll();
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());
        verifyAll();
    }

    @Test
    void shouldBeInterestedInCorrectHoursPriorAcrossMidnight() {
        TramTime queryTime = TramTime.of(23,40);
        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow, numChanges);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, reachabilityRepository,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        int costSoFar = 15;  // 23.55
        TramTime elapsed = queryTime.plusMinutes(costSoFar);

        Node node = createMock(Node.class);

        EasyMock.expect(nodeOperations.getHour(node)).andReturn(22);
        EasyMock.expect(nodeOperations.getHour(node)).andReturn(23);
        EasyMock.expect(nodeOperations.getHour(node)).andReturn(0);
        EasyMock.expect(nodeOperations.getHour(node)).andReturn(1);

        replayAll();
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid()); // before
        assertTrue(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());

        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());
        verifyAll();
    }

    @Test
    void shouldBeInterestedInCorrectHoursEarlyMorning() {
        TramTime queryTime = TramTime.of(0,5);
        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow, numChanges);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, reachabilityRepository,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        int costSoFar = 15;  // 23.55
        TramTime elapsed = queryTime.plusMinutes(costSoFar);

        Node node = createMock(Node.class);

        EasyMock.expect(nodeOperations.getHour(node)).andReturn(23);
        EasyMock.expect(nodeOperations.getHour(node)).andReturn(0);
        EasyMock.expect(nodeOperations.getHour(node)).andReturn(1);

        replayAll();
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());
        verifyAll();
    }

    @Test
    void shouldBeInterestedInCorrectHoursEarlyMorningNextHour() {
        TramTime queryTime = TramTime.of(0,50);
        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow, numChanges);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, reachabilityRepository,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        int costSoFar = 15;  // 23.55
        TramTime elapsed = queryTime.plusMinutes(costSoFar);

        Node node = createMock(Node.class);

        EasyMock.expect(nodeOperations.getHour(node)).andReturn(23);
        EasyMock.expect(nodeOperations.getHour(node)).andReturn(0);
        EasyMock.expect(nodeOperations.getHour(node)).andReturn(1);

        replayAll();
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());
        verifyAll();
    }

    @Test
    void shouldCheckTimeAtNodeCorrectly() {
        TramTime queryTime = TramTime.of(7,0);
        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow, numChanges);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, reachabilityRepository,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        LocalTime nodeTime = LocalTime.of(8, 0);

        // 7.10 too early, too long to wait
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(10), nodeTime, false, reasons, false);
        // 7.29 too early, too long to wait
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(29), nodeTime, false, reasons, false);

        // 8
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(60), nodeTime, true, reasons, false);
        // 7.30
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(30), nodeTime, true, reasons, false);
        // 7.45
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(45), nodeTime, true, reasons, false);
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(59), nodeTime, true, reasons, false);

        // 8.01 - you missed it
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(61), nodeTime, false, reasons, false);
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(120), nodeTime, false, reasons, false);

    }

    @Test
    void shouldCheckTimeAtNodeCorrectlyOvermidnight() {
        TramTime queryTime = TramTime.of(23,50);
        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow, numChanges);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, reachabilityRepository,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        LocalTime nodeTime = LocalTime.of(0, 5);

        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(9), nodeTime, true, reasons, true);

        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(10), nodeTime, true, reasons, true);
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(11), nodeTime, true, reasons, true);
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(15), nodeTime, true, reasons, true);

        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(16), nodeTime, false, reasons, true);
    }

    private void checkForNodeTime(ServiceHeuristics serviceHeuristics, TramTime currentElapsed, LocalTime nodeTime,
                                  boolean expect, ServiceReasons reasons, Boolean nextDay) {
        resetAll();

        Node node = createMock(Node.class);

        TramTime tramTime = nextDay ? TramTime.nextDay(nodeTime.getHour(), nodeTime.getMinute()) : TramTime.of(nodeTime);

        EasyMock.expect(nodeOperations.getTime(node)).andReturn(tramTime);
        EasyMock.expect(journeyConstraints.getMaxWait()).andStubReturn(MAX_WAIT);

        replayAll();
        assertEquals(expect, serviceHeuristics.checkTime(path, node, currentElapsed, reasons).isValid());
        verifyAll();
    }

    @Test
    void shouldBeInterestedInCorrectHoursOverMidnightLongerJourney() {
        TramTime queryTime = TramTime.of(23,10);
        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow, numChanges);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, reachabilityRepository,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        TramTime elapsed = TramTime.of(0,1);
        Node node = createStrictMock(Node.class);

        EasyMock.expect(nodeOperations.getHour(node)).andReturn(22);
        EasyMock.expect(nodeOperations.getHour(node)).andReturn(23);
        EasyMock.expect(nodeOperations.getHour(node)).andReturn(0);
        EasyMock.expect(nodeOperations.getHour(node)).andReturn(1);

        replayAll();
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed, reasons).isValid());
        verifyAll();
    }

    @Test
    void shouldCheckMaximumDurationCorrectly() {
        TramTime queryTime = TramTime.of(11,20);
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(TestEnv.nextSaturday()), queryTime,
                false, 3, maxJourneyDuration);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow, numChanges);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, reachabilityRepository,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        int overallMaxLen = config30MinsWait.getMaxJourneyDuration();

        replayAll();

        assertTrue(serviceHeuristics.journeyDurationUnderLimit(5, path, reasons).isValid());
        assertTrue(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen-1, path, reasons).isValid());
        assertTrue(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen, path, reasons).isValid());
        assertFalse(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen+1, path, reasons).isValid());

        verifyAll();
    }

    @Test
    void shouldCheckChangeLimit() {
        TramTime queryTime = TramTime.of(11,20);
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(TestEnv.nextSaturday()), queryTime,
                false, 2, 160);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow, numChanges);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, reachabilityRepository,
                journeyConstraints, queryTime,
                2);

        assertTrue(serviceHeuristics.checkNumberChanges(0, path, reasons).isValid());
        assertTrue(serviceHeuristics.checkNumberChanges(1, path, reasons).isValid());
        assertTrue(serviceHeuristics.checkNumberChanges(2, path, reasons).isValid());
        assertFalse(serviceHeuristics.checkNumberChanges(3, path, reasons).isValid());
    }

    @Test
    void shouldCheckMaximumDurationCorrectlyAcrossMidnight() {
        TramTime queryTime = TramTime.of(23,20);
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(TestEnv.nextSaturday()), queryTime,
                false, 3, maxJourneyDuration);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow, numChanges);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, reachabilityRepository,
                journeyConstraints, queryTime,
                MAX_NUM_CHANGES);

        int overallMaxLen = config30MinsWait.getMaxJourneyDuration();

        replayAll();

        assertTrue(serviceHeuristics.journeyDurationUnderLimit(5, path, reasons).isValid());
        assertTrue(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen-1, path, reasons).isValid());
        assertTrue(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen, path, reasons).isValid());
        assertFalse(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen+1, path, reasons).isValid());

        verifyAll();
    }

    private static class NeedMaxWaitConfig extends IntegrationTramTestConfig {
        private final int maxWait;

        public NeedMaxWaitConfig(int maxWait) {
            this.maxWait = maxWait;
        }

        @Override
        public int getMaxWait() {
            return maxWait;
        }
    }
}
