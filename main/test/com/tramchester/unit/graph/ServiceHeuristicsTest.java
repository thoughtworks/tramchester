package com.tramchester.unit.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.*;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.Stations;
import com.tramchester.repository.TramReachabilityRepository;
import com.tramchester.repository.RunningServices;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static com.tramchester.graph.GraphStaticKeys.SERVICE_ID;
import static org.junit.Assert.*;

public class ServiceHeuristicsTest extends EasyMockSupport {

    private static final int MAX_WAIT = 30;
    private final List<Station> endStationIds = Arrays.asList(Stations.Deansgate);

    private TramchesterConfig config30MinsWait = new NeedMaxWaitConfig(MAX_WAIT);
    private CachedNodeOperations nodeOperations;
    private RunningServices runningServices;
    private Path path;
    private TramReachabilityRepository tramReachabilityRepository;
    private int maxPathLength = 400;
    private StationRepository stationRepository;

    @Before
    public void beforeEachTestRuns() {
        NodeIdLabelMap nodeIdLabelMap = new NodeIdLabelMap();
        nodeOperations = new CachedNodeOperations(nodeIdLabelMap);
        runningServices = createMock(RunningServices.class);
        path = createMock(Path.class);
        tramReachabilityRepository = createMock(TramReachabilityRepository.class);
        stationRepository = createMock(StationRepository.class);
    }

    @Test
    public void shouldCheckNodeBasedOnServiceId() {
        TramTime queryTime = TramTime.of(8,1);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository, config30MinsWait,
                queryTime, runningServices, endStationIds, new ServiceReasons(), maxPathLength);

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
    public void shouldCheckNodeBasedOnServiceIdAndTimeOverlaps() {
        TramTime queryTime = TramTime.of(8,1);
        LocalTime elaspsedTime = LocalTime.of(9,1);
        TramTime elaspsedTramTime = TramTime.of(elaspsedTime);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository, config30MinsWait,
                queryTime, runningServices, endStationIds, new ServiceReasons(), maxPathLength);

        //runningServices.add("serviceIdA");
        EasyMock.expect(runningServices.getServiceEarliest("serviceIdA")).andReturn(TramTime.of(8,00));
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
    public void shouldBeInterestedInCorrectHours() {
        TramTime queryTime = TramTime.of(9,1);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository, config30MinsWait,
                queryTime, runningServices, endStationIds, new ServiceReasons(), maxPathLength);

        // querytime + costSoFar + maxWait (for board) = latest time could arrive here
        // querytime + costSoFar + 0 = earlier time could arrive here

        int costSoFar = 58; // 9.59
        TramTime elapsed = queryTime.plusMinutes(costSoFar);

        assertFalse(serviceHeuristics.interestedInHour(path, 8, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, 9, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, 10, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, 11, elapsed).isValid());
    }

    @Test
    public void shouldBeInterestedInCorrectHoursCrossesNextHour() {
        TramTime queryTime = TramTime.of(7,0);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository, config30MinsWait,
                queryTime, runningServices, endStationIds, new ServiceReasons(), maxPathLength);

        TramTime elapsed = TramTime.of(10,29);
        assertFalse(serviceHeuristics.interestedInHour(path, 8, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, 9, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, 10, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, 11, elapsed).isValid());
    }

    @Test
    public void shouldBeInterestedInCorrectHoursPriorToMidnight() {
        TramTime queryTime = TramTime.of(23,10);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository, config30MinsWait,
                queryTime, runningServices, endStationIds, new ServiceReasons(), maxPathLength);

        int costSoFar = 15;  // 23.25
        TramTime elapsed = queryTime.plusMinutes(costSoFar);

        assertFalse(serviceHeuristics.interestedInHour(path, 22, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, 23, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, 0, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, 1, elapsed).isValid());
    }

    @Test
    public void shouldBeInterestedInCorrectHoursPriorAcrossMidnight() {
        TramTime queryTime = TramTime.of(23,40);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository, config30MinsWait,
                queryTime, runningServices, endStationIds, new ServiceReasons(), maxPathLength);

        int costSoFar = 15;  // 23.55
        TramTime elapsed = queryTime.plusMinutes(costSoFar);

        assertFalse(serviceHeuristics.interestedInHour(path, 22, elapsed).isValid()); // before
        assertTrue(serviceHeuristics.interestedInHour(path, 23, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, 0, elapsed).isValid());

        assertFalse(serviceHeuristics.interestedInHour(path, 1, elapsed).isValid());
    }

    @Test
    public void shouldBeInterestedInCorrectHoursEarlyMorning() {
        TramTime queryTime = TramTime.of(0,5);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository, config30MinsWait,
                queryTime, runningServices, endStationIds, new ServiceReasons(), maxPathLength);

        int costSoFar = 15;  // 23.55
        TramTime elapsed = queryTime.plusMinutes(costSoFar);

        assertFalse(serviceHeuristics.interestedInHour(path, 23, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, 0, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, 1, elapsed).isValid());
    }

    @Test
    public void shouldBeInterestedInCorrectHoursEarlyMorningNextHour() {
        TramTime queryTime = TramTime.of(0,50);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository, config30MinsWait,
                queryTime, runningServices, endStationIds, new ServiceReasons(), maxPathLength);

        int costSoFar = 15;  // 23.55
        TramTime elapsed = queryTime.plusMinutes(costSoFar);

        assertFalse(serviceHeuristics.interestedInHour(path, 23, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, 0, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, 1, elapsed).isValid());
    }

    @Test
    public void shouldCheckTimeAtNodeCorrectly() {
        TramTime queryTime = TramTime.of(7,00);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository, config30MinsWait,
                queryTime, runningServices, endStationIds, new ServiceReasons(), maxPathLength);

        LocalTime nodeTime = LocalTime.of(8, 00);

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
    public void shouldCheckTimeAtNodeCorrectlyOvermidnight() {
        TramTime queryTime = TramTime.of(23,50);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository, config30MinsWait,
                queryTime, runningServices, endStationIds, new ServiceReasons(), maxPathLength);

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
    public void shouldBeInterestedInCorrectHoursOverMidnightLongerJourney() {
        TramTime queryTime = TramTime.of(23,10);
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository, config30MinsWait,
                queryTime, runningServices, endStationIds, new ServiceReasons(), maxPathLength);

        TramTime elapsed = TramTime.of(0,1);

        assertFalse(serviceHeuristics.interestedInHour(path, 22, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, 23, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, 0, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, 1, elapsed).isValid());
    }

    @Test
    public void shouldCheckMaximumDurationCorrectly() {
        TramTime queryTime = TramTime.of(11,20);
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository,
                config30MinsWait, queryTime, runningServices, endStationIds, new ServiceReasons(), maxPathLength);

        int overallMaxLen = config30MinsWait.getMaxJourneyDuration();

        assertTrue(serviceHeuristics.journeyDurationUnderLimit(5, path).isValid());
        assertTrue(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen-1, path).isValid());
        assertTrue(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen, path).isValid());
        assertFalse(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen+1, path).isValid());

    }

    @Test
    public void shouldCheckMaximumDurationCorrectlyAcrossMidnight() {
        TramTime queryTime = TramTime.of(23,20);
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository,
                config30MinsWait, queryTime, runningServices, endStationIds, new ServiceReasons(), maxPathLength);

        int overallMaxLen = config30MinsWait.getMaxJourneyDuration();

        assertTrue(serviceHeuristics.journeyDurationUnderLimit(5, path).isValid());
        assertTrue(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen-1, path).isValid());
        assertTrue(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen, path).isValid());
        assertFalse(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen+1, path).isValid());
    }

    private ElapsedTime createMatchProvider(TramTime queryTime, TramTime journeyStart) throws TramchesterException {
        ElapsedTime provider = createMock(ElapsedTime.class);
        EasyMock.expect(provider.getElapsedTime()).andStubReturn(queryTime);
        EasyMock.expect(provider.startNotSet()).andReturn(true);
        provider.setJourneyStart(journeyStart);
        EasyMock.expectLastCall();

        return provider;
    }

    private ElapsedTime createNoMatchProvider(TramTime queryTime) throws TramchesterException {
        ElapsedTime provider = createMock(ElapsedTime.class);
        EasyMock.expect(provider.getElapsedTime()).andReturn(queryTime);
        return provider;
    }

    private class NeedMaxWaitConfig extends IntegrationTramTestConfig {
        private int maxWait;

        public NeedMaxWaitConfig(int maxWait) {
            this.maxWait = maxWait;
        }

        @Override
        public int getMaxWait() {
            return maxWait;
        }
    }
}
