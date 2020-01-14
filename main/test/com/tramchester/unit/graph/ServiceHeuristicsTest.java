package com.tramchester.unit.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.*;
import com.tramchester.graph.Relationships.*;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.ReachabilityRepository;
import com.tramchester.repository.RunningServices;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.time.LocalTime;

import static com.tramchester.graph.GraphStaticKeys.*;
import static org.junit.Assert.*;

public class ServiceHeuristicsTest extends EasyMockSupport {

    public static final int MAX_WAIT = 30;

    private TramTime am10 = TramTime.of(10,0);
    private TramTime[] tramTimes = new TramTime[] { am10,
            am10.plusMinutes(100),
            am10.plusMinutes(200),
            am10.plusMinutes(300),
            am10.plusMinutes(400) };
    private CachingCostEvaluator costEvaluator;
    private TramchesterConfig config30MinsWait = new NeedMaxWaitConfig(MAX_WAIT);
    private TramTime NOT_USED_HERE = TramTime.of(23,59);
    private CachedNodeOperations nodeOperations;
    private RunningServices runningServices;
    private Path path;
    private ReachabilityRepository reachabilityRepository;

    @Before
    public void beforeEachTestRuns() {
        costEvaluator = new CachingCostEvaluator();
        NodeIdLabelMap nodeIdLabelMap = new NodeIdLabelMap();
        nodeOperations = new CachedNodeOperations(nodeIdLabelMap);
        runningServices = createMock(RunningServices.class);
        path = createMock(Path.class);
        reachabilityRepository = createMock(ReachabilityRepository.class);
    }

    @Test
    public void shouldCheckNodeBasedOnServiceId() {
        TramTime queryTime = TramTime.of(8,1);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, reachabilityRepository, config30MinsWait,
                queryTime, runningServices, "endStationId", new ServiceReasons());

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

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, reachabilityRepository, config30MinsWait,
                queryTime, runningServices, "endStationId", new ServiceReasons());

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

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, reachabilityRepository, config30MinsWait,
                queryTime, runningServices, "endStationId", new ServiceReasons());

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

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, reachabilityRepository, config30MinsWait,
                queryTime, runningServices, "endStationId", new ServiceReasons());

        TramTime elapsed = TramTime.of(10,29);
        assertFalse(serviceHeuristics.interestedInHour(path, 8, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, 9, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, 10, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, 11, elapsed).isValid());
    }

    @Test
    public void shouldBeInterestedInCorrectHoursPriorToMidnight() {
        TramTime queryTime = TramTime.of(23,10);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, reachabilityRepository, config30MinsWait,
                queryTime, runningServices, "endStationId", new ServiceReasons());

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

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, reachabilityRepository, config30MinsWait,
                queryTime, runningServices, "endStationId", new ServiceReasons());

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

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, reachabilityRepository, config30MinsWait,
                queryTime, runningServices, "endStationId", new ServiceReasons());

        int costSoFar = 15;  // 23.55
        TramTime elapsed = queryTime.plusMinutes(costSoFar);

        assertFalse(serviceHeuristics.interestedInHour(path, 23, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, 0, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, 1, elapsed).isValid());
    }

    @Test
    public void shouldBeInterestedInCorrectHoursEarlyMorningNextHour() {
        TramTime queryTime = TramTime.of(0,50);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, reachabilityRepository, config30MinsWait,
                queryTime, runningServices, "endStationId", new ServiceReasons());

        int costSoFar = 15;  // 23.55
        TramTime elapsed = queryTime.plusMinutes(costSoFar);

        assertFalse(serviceHeuristics.interestedInHour(path, 23, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, 0, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, 1, elapsed).isValid());
    }

    @Test
    public void shouldCheckTimeAtNodeCorrectly() {
        TramTime queryTime = TramTime.of(7,00);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, reachabilityRepository, config30MinsWait,
                queryTime, runningServices, "endStationId", new ServiceReasons());

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

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, reachabilityRepository, config30MinsWait,
                queryTime, runningServices, "endStationId", new ServiceReasons());

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
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, reachabilityRepository, config30MinsWait,
                queryTime, runningServices, "endStationId", new ServiceReasons());

        TramTime elapsed = TramTime.of(0,1);

        assertFalse(serviceHeuristics.interestedInHour(path, 22, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, 23, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(path, 0, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(path, 1, elapsed).isValid());
    }

    @Test
    public void shouldHandleTimesWith30MinWait() throws TramchesterException {
        TramTime am640 = TramTime.of(6, 40); // 400

        ElapsedTime providerA = createNoMatchProvider(am640);
        ElapsedTime providerB = createNoMatchProvider(am640.plusMinutes(150));

        TramTime journeyStart = TramTime.of(10,0).minusMinutes(TransportGraphBuilder.BOARDING_COST);
        ElapsedTime providerC = createMatchProvider(am640.plusMinutes(180), journeyStart);
        ElapsedTime providerD = createMatchProvider(am640.plusMinutes(200), journeyStart);
        ElapsedTime providerE = createNoMatchProvider(am640.plusMinutes(220));
        ElapsedTime providerF = createNoMatchProvider(am640.plusMinutes(230));
        ElapsedTime providerG = createNoMatchProvider(am640.plusMinutes(250));

        ElapsedTime providerH = createMatchProvider(am640.plusMinutes(280), journeyStart.plusMinutes(100));
        ElapsedTime providerI = createNoMatchProvider(am640.plusMinutes(601));

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, reachabilityRepository, config30MinsWait,
                NOT_USED_HERE, runningServices, "endStationId", new ServiceReasons());
        assertFalse(serviceHeuristics.underMaxWait(tramTimes, providerA));
        assertFalse(serviceHeuristics.underMaxWait(tramTimes, providerB));
        assertTrue(serviceHeuristics.underMaxWait(tramTimes, providerC));
        assertTrue(serviceHeuristics.underMaxWait(tramTimes, providerD));
        assertFalse(serviceHeuristics.underMaxWait(tramTimes, providerE));
        assertFalse(serviceHeuristics.underMaxWait(tramTimes, providerF));
        assertFalse(serviceHeuristics.underMaxWait(tramTimes, providerG));
        assertTrue(serviceHeuristics.underMaxWait(tramTimes, providerH));
        assertFalse(serviceHeuristics.underMaxWait(tramTimes, providerI));
        verifyAll();
    }

    @Test
    public void shouldHandleTimesWith15MinWait() throws TramchesterException {
        TramTime am640 = TramTime.of(6, 40); // 400

        ElapsedTime providerA = createNoMatchProvider(am640);
        ElapsedTime providerB = createNoMatchProvider(am640.plusMinutes(150));
        ElapsedTime providerC = createNoMatchProvider(am640.plusMinutes(180));
        TramTime journeyStart = TramTime.of(10,0).minusMinutes(TransportGraphBuilder.BOARDING_COST);

        ElapsedTime providerD = createMatchProvider(am640.plusMinutes(200), journeyStart);
        ElapsedTime providerE = createNoMatchProvider(am640.plusMinutes(220));
        ElapsedTime providerF = createNoMatchProvider(am640.plusMinutes(230));
        ElapsedTime providerG = createNoMatchProvider(am640.plusMinutes(250));
        ElapsedTime providerH = createNoMatchProvider(am640.plusMinutes(280));
        ElapsedTime providerI = createMatchProvider(am640.plusMinutes(190), journeyStart);
        ElapsedTime providerJ = createNoMatchProvider(am640.plusMinutes(601));

        replayAll();
        TramchesterConfig configuration = new NeedMaxWaitConfig(15);
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, reachabilityRepository, configuration,
                NOT_USED_HERE, runningServices, "endStationId", new ServiceReasons());
        assertFalse(serviceHeuristics.underMaxWait(tramTimes, providerA));
        assertFalse(serviceHeuristics.underMaxWait(tramTimes, providerB));
        assertFalse(serviceHeuristics.underMaxWait(tramTimes, providerC));
        assertTrue(serviceHeuristics.underMaxWait(tramTimes, providerD));
        assertFalse(serviceHeuristics.underMaxWait(tramTimes, providerE));
        assertFalse(serviceHeuristics.underMaxWait(tramTimes, providerF));
        assertFalse(serviceHeuristics.underMaxWait(tramTimes, providerG));
        assertFalse(serviceHeuristics.underMaxWait(tramTimes, providerH));
        assertTrue(serviceHeuristics.underMaxWait(tramTimes, providerI));
        assertFalse(serviceHeuristics.underMaxWait(tramTimes, providerJ));
        verifyAll();
    }

    @Test
    public void shouldHandleTimesOneTime() throws TramchesterException {
        TramTime am640 = TramTime.of(6, 40); // 400

        TramTime[] time = new TramTime[] { TramTime.of(7,30) };
        ElapsedTime providerA = createNoMatchProvider(am640);
        TramTime journeyStart = am640.plusMinutes(50).minusMinutes(TransportGraphBuilder.BOARDING_COST);
        ElapsedTime providerB = createMatchProvider(am640.plusMinutes(20), journeyStart);
        ElapsedTime providerC = createNoMatchProvider(am640.plusMinutes(51));

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, reachabilityRepository, config30MinsWait,
                NOT_USED_HERE, runningServices, "endStationId", new ServiceReasons());
        assertFalse(serviceHeuristics.underMaxWait(time, providerA));
        assertTrue(serviceHeuristics.underMaxWait(time, providerB));
        assertFalse(serviceHeuristics.underMaxWait(time, providerC));
        verifyAll();
    }

    @Test
    public void shouldHandleTotalDurationOverWaitTime() throws TramchesterException {
        TramTime[] time = new TramTime[] { TramTime.of(7,30) };

        ElapsedTime provider = createMock(ElapsedTime.class);
        EasyMock.expect(provider.getElapsedTime()).andStubReturn(TramTime.of(7,20));
        EasyMock.expect(provider.startNotSet()).andReturn(false);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, reachabilityRepository, config30MinsWait,
                NOT_USED_HERE, runningServices, "endStationId", new ServiceReasons());
        assertTrue(serviceHeuristics.underMaxWait(time, provider));
        verifyAll();
    }

    @Test
    public void shouldCheckIfChangeOfServiceWithDepartAndThenBoard() {
//        TramServiceDate startDate = new TramServiceDate("20141201");
//        TramServiceDate endDate = new TramServiceDate("20151130");
        boolean[] days = new boolean[] {true,true,true,true,true,true,true};

        TransportRelationship board = BoardRelationship.TestOnly("boardsId", null, null);
        TransportRelationship depart = DepartRelationship.TestOnly("departsId", null, null);
        TransportRelationship change = InterchangeDepartsRelationship.TestOnly("interchangeId",null,null);

        String tripId = "";

        TramGoesToRelationship outA = TramGoesToRelationship.TestOnly("0042",10, days, tramTimes, "id1",
                null, null, tripId);

        TramGoesToRelationship outB = TramGoesToRelationship.TestOnly("0048", 5, days, tramTimes, "id2",
                null, null, tripId);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, reachabilityRepository, config30MinsWait,
                NOT_USED_HERE, runningServices, "endStationId", new ServiceReasons());
        assertTrue(serviceHeuristics.sameService(path, board, outA).isValid());
        assertTrue(serviceHeuristics.sameService(path, depart, outA).isValid());
        assertTrue(serviceHeuristics.sameService(path, change, outA).isValid());
        assertTrue(serviceHeuristics.sameService(path, outA, outA).isValid());
        assertFalse(serviceHeuristics.sameService(path, outB, outA).isValid());
        verifyAll();
    }

    @Test
    public void shouldCheckEndStationId() {
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, reachabilityRepository, config30MinsWait,
                NOT_USED_HERE, runningServices, "endStationId", new ServiceReasons());
        Relationship departA = createMock(Relationship.class);
        EasyMock.expect(departA.getProperty(STATION_ID)).andReturn("endStationId");
        Relationship departB = createMock(Relationship.class);
        EasyMock.expect(departB.getProperty(STATION_ID)).andReturn("XXX");


        replayAll();
        assertTrue(serviceHeuristics.toEndStation(departA));
        assertFalse(serviceHeuristics.toEndStation(departB));

        verifyAll();
    }

    @Test
    public void shouldCheckMaximumDurationCorrectly() {
        TramTime queryTime = TramTime.of(11,20);
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, reachabilityRepository,
                config30MinsWait, queryTime, runningServices, "endStationId", new ServiceReasons());

        int overallMaxLen = config30MinsWait.getMaxJourneyDuration();

        assertTrue(serviceHeuristics.journeyDurationUnderLimit(5, path).isValid());
        assertTrue(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen-1, path).isValid());
        assertTrue(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen, path).isValid());
        assertFalse(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen+1, path).isValid());

    }

    @Test
    public void shouldCheckMaximumDurationCorrectlyAcrossMidnight() {
        TramTime queryTime = TramTime.of(23,20);
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, reachabilityRepository,
                config30MinsWait, queryTime, runningServices, "endStationId", new ServiceReasons());

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
