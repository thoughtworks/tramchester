package com.tramchester.unit.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.CachedNodeOperations;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.NodeContentsRepository;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.ServiceHeuristics;
import com.tramchester.graph.search.ServiceReason;
import com.tramchester.graph.search.ServiceReasons;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.RunningServices;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TramReachabilityRepository;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;

import java.time.LocalTime;
import java.util.Collections;
import java.util.Set;

import static com.tramchester.graph.GraphStaticKeys.HOUR;
import static com.tramchester.graph.GraphStaticKeys.SERVICE_ID;
import static org.junit.jupiter.api.Assertions.*;

class ServiceHeuristicsTest extends EasyMockSupport {

    private static final int MAX_WAIT = 30;
    private final Set<Station> endStationIds = Collections.singleton(Stations.Deansgate);

    private final TramchesterConfig config30MinsWait = new NeedMaxWaitConfig(MAX_WAIT);
    private NodeContentsRepository nodeOperations;
    private RunningServices runningServices;
    private Path path;
    private TramReachabilityRepository tramReachabilityRepository;
    private final int maxPathLength = 400;
    private StationRepository stationRepository;
    private ProvidesLocalNow providesLocalNow;

    @BeforeEach
    void beforeEachTestRuns() {
        nodeOperations = new CachedNodeOperations();
        runningServices = createMock(RunningServices.class);
        path = createMock(Path.class);
        providesLocalNow = new ProvidesLocalNow();
        tramReachabilityRepository = createMock(TramReachabilityRepository.class);
        stationRepository = createMock(StationRepository.class);
    }

    @Test
    void shouldCheckNodeBasedOnServiceId() {
        TramTime queryTime = TramTime.of(8,1);

        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(TestEnv.nextSaturday()), queryTime, false);
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository, config30MinsWait,
                queryTime, runningServices, endStationIds, new ServiceReasons(journeyRequest, queryTime, providesLocalNow), maxPathLength, 5);

        EasyMock.expect(runningServices.isRunning("serviceIdA")).andReturn(true);
        EasyMock.expect(runningServices.isRunning("serviceIdB")).andReturn(false);

        Node node = createMock(Node.class);
        EasyMock.expect(node.getId()).andReturn(42L);
        EasyMock.expect(node.getProperty(SERVICE_ID)).andReturn("serviceIdA");
        EasyMock.expect(node.getId()).andReturn(43L);
        EasyMock.expect(node.getProperty(SERVICE_ID)).andReturn("serviceIdB");

        replayAll();
        ServiceReason result = serviceHeuristics.checkServiceDate(node, path);
        assertTrue(result.isValid());

        result = serviceHeuristics.checkServiceDate(node, path);
        assertEquals(ServiceReason.ReasonCode.NotOnQueryDate, result.getReasonCode());
        verifyAll();
    }

    @Test
    void shouldCheckNodeBasedOnServiceIdAndTimeOverlaps() {
        TramTime queryTime = TramTime.of(8,1);
        LocalTime elaspsedTime = LocalTime.of(9,1);
        TramTime elaspsedTramTime = TramTime.of(elaspsedTime);

        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(TestEnv.nextSaturday()), queryTime, false);
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository, config30MinsWait,
                queryTime, runningServices, endStationIds, new ServiceReasons(journeyRequest, queryTime, providesLocalNow), maxPathLength, 10);

        //runningServices.add("serviceIdA");
        EasyMock.expect(runningServices.getServiceEarliest("serviceIdA")).andReturn(TramTime.of(8,0));
        EasyMock.expect(runningServices.getServiceLatest("serviceIdA")).andReturn(TramTime.of(8,30));

        // no longer running at query time
        Node tooEarlyNode = createMock(Node.class);
        EasyMock.expect(tooEarlyNode.getId()).andReturn(42L);
        EasyMock.expect(tooEarlyNode.getProperty(SERVICE_ID)).andReturn("serviceIdA");

        // doesnt start running until after query time
        Node tooLateNode = createMock(Node.class);
        EasyMock.expect(tooLateNode.getId()).andReturn(43L);
        EasyMock.expect(tooLateNode.getProperty(SERVICE_ID)).andReturn("serviceIdA");  // CACHED
        EasyMock.expect(runningServices.getServiceEarliest("serviceIdA")).andReturn(TramTime.of(elaspsedTime.plusMinutes(MAX_WAIT+1)));
        EasyMock.expect(runningServices.getServiceLatest("serviceIdA")).andReturn(TramTime.of(elaspsedTime.plusMinutes(MAX_WAIT+30)));

        // starts before query, but still running within max wait
        Node overlapStartsBefore = createMock(Node.class);
        EasyMock.expect(overlapStartsBefore.getId()).andReturn(43L);
        EasyMock.expect(runningServices.getServiceEarliest("serviceIdA")).andReturn(TramTime.of(8,50));
        EasyMock.expect(runningServices.getServiceLatest("serviceIdA")).andReturn(TramTime.of(9,20));

        // starts after query within max wait, finishes after max wait
        Node overlapStartsAfter = createMock(Node.class);
        EasyMock.expect(overlapStartsAfter.getId()).andReturn(43L);
        EasyMock.expect(runningServices.getServiceEarliest("serviceIdA")).andReturn(TramTime.of(9,20));
        EasyMock.expect(runningServices.getServiceLatest("serviceIdA")).andReturn(TramTime.of(9,45));

        // starts before query, finishes after max wait
        Node overlapStartsBeforeFinishesAfter = createMock(Node.class);
        EasyMock.expect(overlapStartsBeforeFinishesAfter.getId()).andReturn(43L);
        EasyMock.expect(runningServices.getServiceEarliest("serviceIdA")).andReturn(TramTime.of(8,45));
        EasyMock.expect(runningServices.getServiceLatest("serviceIdA")).andReturn(TramTime.of(9,20));

        // end is after midnight case
        Node endsAfterMidnight = createMock(Node.class);
        EasyMock.expect(endsAfterMidnight.getId()).andReturn(43L);
        EasyMock.expect(runningServices.getServiceEarliest("serviceIdA")).andReturn(TramTime.of(5,23));
        EasyMock.expect(runningServices.getServiceLatest("serviceIdA")).andReturn(TramTime.of(0,1));

        replayAll();
        assertEquals(ServiceReason.DoesNotOperateOnTime(elaspsedTramTime, path),
                serviceHeuristics.checkServiceTime(path, tooEarlyNode, elaspsedTramTime));
        assertEquals(ServiceReason.DoesNotOperateOnTime(elaspsedTramTime, path),
                serviceHeuristics.checkServiceTime(path, tooLateNode, elaspsedTramTime));

        assertTrue(serviceHeuristics.checkServiceTime(path, overlapStartsBefore, elaspsedTramTime).isValid());
        assertTrue(serviceHeuristics.checkServiceTime(path, overlapStartsAfter, elaspsedTramTime).isValid());
        assertTrue(serviceHeuristics.checkServiceTime(path, overlapStartsBeforeFinishesAfter, elaspsedTramTime).isValid());
        assertTrue(serviceHeuristics.checkServiceTime(path, endsAfterMidnight, elaspsedTramTime).isValid());

        verifyAll();
    }

    @Test
    void shouldBeInterestedInCorrectHours() {
        TramTime queryTime = TramTime.of(9,1);

        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(TestEnv.nextSaturday()), queryTime, false);
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository, config30MinsWait,
                queryTime, runningServices, endStationIds, new ServiceReasons(journeyRequest, queryTime, providesLocalNow), maxPathLength, 5);

        // querytime + costSoFar + maxWait (for board) = latest time could arrive here
        // querytime + costSoFar + 0 = earlier time could arrive here

        int costSoFar = 58; // 9.59
        TramTime elapsed = queryTime.plusMinutes(costSoFar);
        Node node = createMock(Node.class);
        EasyMock.expect(node.getId()).andReturn(42L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(8);
        EasyMock.expect(node.getId()).andReturn(43L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(9);
        EasyMock.expect(node.getId()).andReturn(44L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(10);
        EasyMock.expect(node.getId()).andReturn(45L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(11);

        replayAll();
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());
        verifyAll();
    }

    @Test
    void shouldBeInterestedInCorrectHoursCrossesNextHour() {
        TramTime queryTime = TramTime.of(7,0);
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(TestEnv.nextSaturday()), queryTime, false);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository, config30MinsWait,
                queryTime, runningServices, endStationIds, new ServiceReasons(journeyRequest, queryTime, providesLocalNow), maxPathLength, 5);

        TramTime elapsed = TramTime.of(10,29);
        Node node = createMock(Node.class);

        EasyMock.expect(node.getId()).andReturn(42L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(8);
        EasyMock.expect(node.getId()).andReturn(43L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(9);
        EasyMock.expect(node.getId()).andReturn(44L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(10);
        EasyMock.expect(node.getId()).andReturn(45L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(11);

        replayAll();
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());
        verifyAll();
    }

    @Test
    void shouldBeInterestedInCorrectHoursPriorToMidnight() {
        TramTime queryTime = TramTime.of(23,10);
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(TestEnv.nextSaturday()), queryTime, false);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository, config30MinsWait,
                queryTime, runningServices, endStationIds, new ServiceReasons(journeyRequest, queryTime, providesLocalNow), maxPathLength, 5);

        int costSoFar = 15;  // 23.25
        TramTime elapsed = queryTime.plusMinutes(costSoFar);

        Node node = createMock(Node.class);
        EasyMock.expect(node.getId()).andReturn(42L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(22);
        EasyMock.expect(node.getId()).andReturn(43L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(23);
        EasyMock.expect(node.getId()).andReturn(44L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(0);
        EasyMock.expect(node.getId()).andReturn(45L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(1);


        replayAll();
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());
        verifyAll();
    }

    @Test
    void shouldBeInterestedInCorrectHoursPriorAcrossMidnight() {
        TramTime queryTime = TramTime.of(23,40);
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(TestEnv.nextSaturday()), queryTime, false);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository, config30MinsWait,
                queryTime, runningServices, endStationIds, new ServiceReasons(journeyRequest, queryTime, providesLocalNow), maxPathLength, 5);

        int costSoFar = 15;  // 23.55
        TramTime elapsed = queryTime.plusMinutes(costSoFar);

        Node node = createMock(Node.class);
        EasyMock.expect(node.getId()).andReturn(42L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(22);
        EasyMock.expect(node.getId()).andReturn(43L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(23);
        EasyMock.expect(node.getId()).andReturn(44L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(0);
        EasyMock.expect(node.getId()).andReturn(45L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(1);

        replayAll();
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed).isValid()); // before
        assertTrue(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());

        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());
        verifyAll();
    }

    @Test
    void shouldBeInterestedInCorrectHoursEarlyMorning() {
        TramTime queryTime = TramTime.of(0,5);
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(TestEnv.nextSaturday()), queryTime, false);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository, config30MinsWait,
                queryTime, runningServices, endStationIds, new ServiceReasons(journeyRequest, queryTime, providesLocalNow), maxPathLength, 5);

        int costSoFar = 15;  // 23.55
        TramTime elapsed = queryTime.plusMinutes(costSoFar);

        Node node = createMock(Node.class);
        EasyMock.expect(node.getId()).andReturn(42L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(23);
        EasyMock.expect(node.getId()).andReturn(43L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(0);
        EasyMock.expect(node.getId()).andReturn(44L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(1);

        replayAll();
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());
        verifyAll();
    }

    @Test
    void shouldBeInterestedInCorrectHoursEarlyMorningNextHour() {
        TramTime queryTime = TramTime.of(0,50);
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(TestEnv.nextSaturday()), queryTime, false);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository, config30MinsWait,
                queryTime, runningServices, endStationIds, new ServiceReasons(journeyRequest, queryTime, providesLocalNow), maxPathLength, 5);

        int costSoFar = 15;  // 23.55
        TramTime elapsed = queryTime.plusMinutes(costSoFar);

        Node node = createMock(Node.class);
        EasyMock.expect(node.getId()).andStubReturn(42L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(23);
        EasyMock.expect(node.getId()).andReturn(43L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(0);
        EasyMock.expect(node.getId()).andReturn(44L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(1);

        replayAll();
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());
        verifyAll();
    }

    @Test
    void shouldCheckTimeAtNodeCorrectly() {
        TramTime queryTime = TramTime.of(7,0);
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(TestEnv.nextSaturday()), queryTime, false);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository, config30MinsWait,
                queryTime, runningServices, endStationIds, new ServiceReasons(journeyRequest, queryTime, providesLocalNow), maxPathLength, 5);

        LocalTime nodeTime = LocalTime.of(8, 0);

        // 7.10 too early, too long to wait
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(10), nodeTime, false);
        // 7.29 too early, too long to wait
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(29), nodeTime, false);

        // 8
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(60), nodeTime, true);
        // 7.30
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(30), nodeTime, true);
        // 7.45
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(45), nodeTime, true);
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(59), nodeTime, true);

        // 8.01 - you missed it
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(61), nodeTime, false);
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(120), nodeTime, false);

    }

    @Test
    void shouldCheckTimeAtNodeCorrectlyOvermidnight() {
        TramTime queryTime = TramTime.of(23,50);
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(TestEnv.nextSaturday()), queryTime, false);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository, config30MinsWait,
                queryTime, runningServices, endStationIds, new ServiceReasons(journeyRequest, queryTime, providesLocalNow), maxPathLength, 5);

        LocalTime nodeTime = LocalTime.of(0, 5);

        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(9), nodeTime, true);
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(10), nodeTime, true);
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(11), nodeTime, true);
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(15), nodeTime, true);

        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(16), nodeTime, false);
    }

    private void checkForNodeTime(ServiceHeuristics serviceHeuristics, TramTime currentElapsed, LocalTime nodeTime, boolean expect) {
        resetAll();

        Node node = createMock(Node.class);
        EasyMock.expect(node.getId()).andStubReturn(42L); // ok IFF node time always same
        EasyMock.expect(node.getProperty(GraphStaticKeys.TIME)).andStubReturn(nodeTime);

        replayAll();
        assertEquals(expect, serviceHeuristics.checkTime(path, node, currentElapsed).isValid());
        verifyAll();
    }

    @Test
    void shouldBeInterestedInCorrectHoursOverMidnightLongerJourney() {
        TramTime queryTime = TramTime.of(23,10);
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(TestEnv.nextSaturday()), queryTime, false);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository, config30MinsWait,
                queryTime, runningServices, endStationIds, new ServiceReasons(journeyRequest, queryTime, providesLocalNow), maxPathLength, 5);

        TramTime elapsed = TramTime.of(0,1);
        Node node = createStrictMock(Node.class);

        EasyMock.expect(node.getId()).andReturn(42L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(22);
        EasyMock.expect(node.getId()).andReturn(43L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(23);
        EasyMock.expect(node.getId()).andReturn(44L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(0);
        EasyMock.expect(node.getId()).andReturn(45L);
        EasyMock.expect(node.getProperty(HOUR)).andReturn(1);

        replayAll();
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, node, elapsed).isValid());
        verifyAll();
    }

    @Test
    void shouldCheckMaximumDurationCorrectly() {
        TramTime queryTime = TramTime.of(11,20);
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(TestEnv.nextSaturday()), queryTime, false);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository,
                config30MinsWait, queryTime, runningServices, endStationIds,
                new ServiceReasons(journeyRequest, queryTime, providesLocalNow), maxPathLength, 5);

        int overallMaxLen = config30MinsWait.getMaxJourneyDuration();

        assertTrue(serviceHeuristics.journeyDurationUnderLimit(5, path).isValid());
        assertTrue(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen-1, path).isValid());
        assertTrue(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen, path).isValid());
        assertFalse(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen+1, path).isValid());

    }

    @Test
    void shouldCheckChangeLimit() {
        TramTime queryTime = TramTime.of(11,20);
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(TestEnv.nextSaturday()), queryTime, false);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository,
                config30MinsWait, queryTime, runningServices, endStationIds, new ServiceReasons(journeyRequest, queryTime, providesLocalNow),
                maxPathLength, 2);

        assertTrue(serviceHeuristics.checkNumberChanges(0, path).isValid());
        assertTrue(serviceHeuristics.checkNumberChanges(1, path).isValid());
        assertTrue(serviceHeuristics.checkNumberChanges(2, path).isValid());
        assertFalse(serviceHeuristics.checkNumberChanges(3, path).isValid());
    }

    @Test
    void shouldCheckMaximumDurationCorrectlyAcrossMidnight() {
        TramTime queryTime = TramTime.of(23,20);
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(TestEnv.nextSaturday()), queryTime, false);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository,
                config30MinsWait, queryTime, runningServices, endStationIds, new ServiceReasons(journeyRequest, queryTime, providesLocalNow),
                maxPathLength, 5);

        int overallMaxLen = config30MinsWait.getMaxJourneyDuration();

        assertTrue(serviceHeuristics.journeyDurationUnderLimit(5, path).isValid());
        assertTrue(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen-1, path).isValid());
        assertTrue(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen, path).isValid());
        assertFalse(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen+1, path).isValid());
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
