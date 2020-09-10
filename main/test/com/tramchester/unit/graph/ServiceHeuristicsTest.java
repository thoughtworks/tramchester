package com.tramchester.unit.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Service;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.CachedNodeOperations;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.NodeContentsRepository;
import com.tramchester.graph.search.*;
import com.tramchester.graph.search.states.HowIGotHere;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TramReachabilityRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Node;

import java.time.LocalTime;
import java.util.Collections;
import java.util.Set;

import static com.tramchester.graph.GraphPropertyKey.HOUR;
import static com.tramchester.graph.GraphPropertyKey.SERVICE_ID;
import static com.tramchester.testSupport.TramStations.Bury;
import static com.tramchester.testSupport.TramStations.Shudehill;
import static org.junit.jupiter.api.Assertions.*;

class ServiceHeuristicsTest extends EasyMockSupport {

    private static final int MAX_WAIT = 30;
    private static final int MAX_NUM_CHANGES = 5;
    private final Set<Station> endStations = Collections.singleton(TramStations.of(TramStations.Deansgate));

    private final TramchesterConfig config30MinsWait = new NeedMaxWaitConfig(MAX_WAIT);
    private NodeContentsRepository nodeOperations;
    private HowIGotHere path;
    private TramReachabilityRepository tramReachabilityRepository;
    private StationRepository stationRepository;
    private ProvidesLocalNow providesLocalNow;
    private IdFor<Service> serviceIdA;
    private IdFor<Service> serviceIdB;
    private JourneyConstraints journeyConstraints;
    private int maxJourneyDuration;

    @BeforeEach
    void beforeEachTestRuns() {
        maxJourneyDuration = config30MinsWait.getMaxJourneyDuration();
        providesLocalNow = new ProvidesLocalNow();
        serviceIdA = IdFor.createId("serviceIdA");
        serviceIdB = IdFor.createId("serviceIdB");

        nodeOperations = new CachedNodeOperations();
        path = createMock(HowIGotHere.class);
        tramReachabilityRepository = createMock(TramReachabilityRepository.class);
        stationRepository = createMock(StationRepository.class);

        int maxPathLength = 400;
        journeyConstraints = createMock(JourneyConstraints.class);
        EasyMock.expect(journeyConstraints.getMaxWait()).andStubReturn(MAX_WAIT);
        EasyMock.expect(journeyConstraints.getMaxPathLength()).andStubReturn(maxPathLength);
        EasyMock.expect(journeyConstraints.getIsTramOnlyDestinations()).andStubReturn(true);
        EasyMock.expect(journeyConstraints.getEndTramStations()).andStubReturn(endStations);
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
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations,
                tramReachabilityRepository,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        EasyMock.expect(journeyConstraints.isRunning(serviceIdA)).andReturn(true);
        EasyMock.expect(journeyConstraints.isRunning(serviceIdB)).andReturn(false);

        Node node = createMock(Node.class);
        EasyMock.expect(node.getId()).andReturn(42L);
        EasyMock.expect(node.getProperty(SERVICE_ID.getText())).andReturn(serviceIdA.getGraphId());
        EasyMock.expect(node.getId()).andReturn(43L);
        EasyMock.expect(node.getProperty(SERVICE_ID.getText())).andReturn(serviceIdB.getGraphId());

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
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        RouteStation routeStationA = new RouteStation(TramStations.of(Bury), TestEnv.getTestRoute());
        RouteStation routeStationB = new RouteStation(TramStations.of(Shudehill), TestEnv.getTestRoute());

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations,
                tramReachabilityRepository,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        EasyMock.expect(journeyConstraints.isClosed(TramStations.of(Bury))).andReturn(false);
        EasyMock.expect(journeyConstraints.isClosed(TramStations.of(Shudehill))).andReturn(true);

        Node node = createMock(Node.class);
        EasyMock.expect(node.getProperty("route_station_id")).andReturn("123");
        EasyMock.expect(stationRepository.getRouteStationById(IdFor.createId("123"))).andReturn(routeStationA);
        EasyMock.expect(node.getProperty("route_station_id")).andReturn("789");
        EasyMock.expect(stationRepository.getRouteStationById(IdFor.createId("789"))).andReturn(routeStationB);

        replayAll();
        ServiceReason result = serviceHeuristics.checkStationOpen(node, path, reasons);
        assertTrue(result.isValid());

        result = serviceHeuristics.checkStationOpen(node, path, reasons);
        assertEquals(ServiceReason.ReasonCode.StationClosed, result.getReasonCode());
        verifyAll();

    }

    @Test
    void shouldCheckNodeBasedOnServiceIdAndTimeOverlaps() {
        TramTime queryTime = TramTime.of(8,1);
        LocalTime elaspsedTime = LocalTime.of(9,1);
        TramTime elaspsedTramTime = TramTime.of(elaspsedTime);

        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        //runningServices.add("serviceIdA");
        EasyMock.expect(journeyConstraints.getServiceEarliest(serviceIdA)).andReturn(ServiceTime.of(8,0));
        EasyMock.expect(journeyConstraints.getServiceLatest(serviceIdA)).andReturn(ServiceTime.of(8,30));

        // no longer running at query time
        Node tooEarlyNode = createMock(Node.class);
        EasyMock.expect(tooEarlyNode.getId()).andReturn(42L);
        EasyMock.expect(tooEarlyNode.getProperty(SERVICE_ID.getText())).andReturn(serviceIdA.getGraphId());

        // doesnt start running until after query time
        Node tooLateNode = createMock(Node.class);
        EasyMock.expect(tooLateNode.getId()).andReturn(43L);
        EasyMock.expect(tooLateNode.getProperty(SERVICE_ID.getText())).andReturn(serviceIdA.getGraphId());  // CACHED
        EasyMock.expect(journeyConstraints.getServiceEarliest(serviceIdA)).andReturn(ServiceTime.of(elaspsedTime.plusMinutes(MAX_WAIT+1)));
        EasyMock.expect(journeyConstraints.getServiceLatest(serviceIdA)).andReturn(ServiceTime.of(elaspsedTime.plusMinutes(MAX_WAIT+30)));

        // starts before query, but still running within max wait
        Node overlapStartsBefore = createMock(Node.class);
        EasyMock.expect(overlapStartsBefore.getId()).andReturn(43L);
        EasyMock.expect(journeyConstraints.getServiceEarliest(serviceIdA)).andReturn(ServiceTime.of(8,50));
        EasyMock.expect(journeyConstraints.getServiceLatest(serviceIdA)).andReturn(ServiceTime.of(9,20));

        // starts after query within max wait, finishes after max wait
        Node overlapStartsAfter = createMock(Node.class);
        EasyMock.expect(overlapStartsAfter.getId()).andReturn(43L);
        EasyMock.expect(journeyConstraints.getServiceEarliest(serviceIdA)).andReturn(ServiceTime.of(9,20));
        EasyMock.expect(journeyConstraints.getServiceLatest(serviceIdA)).andReturn(ServiceTime.of(9,45));

        // starts before query, finishes after max wait
        Node overlapStartsBeforeFinishesAfter = createMock(Node.class);
        EasyMock.expect(overlapStartsBeforeFinishesAfter.getId()).andReturn(43L);
        EasyMock.expect(journeyConstraints.getServiceEarliest(serviceIdA)).andReturn(ServiceTime.of(8,45));
        EasyMock.expect(journeyConstraints.getServiceLatest(serviceIdA)).andReturn(ServiceTime.of(9,20));

        // end is after midnight case
        Node endsAfterMidnight = createMock(Node.class);
        EasyMock.expect(endsAfterMidnight.getId()).andReturn(43L);
        EasyMock.expect(journeyConstraints.getServiceEarliest(serviceIdA)).andReturn(ServiceTime.of(5,23));
        EasyMock.expect(journeyConstraints.getServiceLatest(serviceIdA)).andReturn(ServiceTime.of(0,1));

        replayAll();
        assertEquals(ServiceReason.DoesNotOperateOnTime(elaspsedTramTime, path),
                serviceHeuristics.checkServiceTime(path, tooEarlyNode, elaspsedTramTime, reasons));
        assertEquals(ServiceReason.DoesNotOperateOnTime(elaspsedTramTime, path),
                serviceHeuristics.checkServiceTime(path, tooLateNode, elaspsedTramTime, reasons));

        assertTrue(serviceHeuristics.checkServiceTime(path, overlapStartsBefore, elaspsedTramTime, reasons).isValid());
        assertTrue(serviceHeuristics.checkServiceTime(path, overlapStartsAfter, elaspsedTramTime, reasons).isValid());
        assertTrue(serviceHeuristics.checkServiceTime(path, overlapStartsBeforeFinishesAfter, elaspsedTramTime, reasons).isValid());
        assertTrue(serviceHeuristics.checkServiceTime(path, endsAfterMidnight, elaspsedTramTime, reasons).isValid());

        verifyAll();
    }

    @Test
    void shouldBeInterestedInCorrectHours() {
        TramTime queryTime = TramTime.of(9,1);

        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        // querytime + costSoFar + maxWait (for board) = latest time could arrive here
        // querytime + costSoFar + 0 = earlier time could arrive here

        int costSoFar = 58; // 9.59
        TramTime elapsed = queryTime.plusMinutes(costSoFar);
        Node node = createMock(Node.class);
        EasyMock.expect(node.getId()).andReturn(42L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(8);
        EasyMock.expect(node.getId()).andReturn(43L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(9);
        EasyMock.expect(node.getId()).andReturn(44L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(10);
        EasyMock.expect(node.getId()).andReturn(45L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(11);

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
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        TramTime elapsed = TramTime.of(10,29);
        Node node = createMock(Node.class);

        EasyMock.expect(node.getId()).andReturn(42L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(8);
        EasyMock.expect(node.getId()).andReturn(43L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(9);
        EasyMock.expect(node.getId()).andReturn(44L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(10);
        EasyMock.expect(node.getId()).andReturn(45L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(11);

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
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);


        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        int costSoFar = 15;  // 23.25
        TramTime elapsed = queryTime.plusMinutes(costSoFar);

        Node node = createMock(Node.class);
        EasyMock.expect(node.getId()).andReturn(42L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(22);
        EasyMock.expect(node.getId()).andReturn(43L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(23);
        EasyMock.expect(node.getId()).andReturn(44L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(0);
        EasyMock.expect(node.getId()).andReturn(45L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(1);


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
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        int costSoFar = 15;  // 23.55
        TramTime elapsed = queryTime.plusMinutes(costSoFar);

        Node node = createMock(Node.class);
        EasyMock.expect(node.getId()).andReturn(42L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(22);
        EasyMock.expect(node.getId()).andReturn(43L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(23);
        EasyMock.expect(node.getId()).andReturn(44L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(0);
        EasyMock.expect(node.getId()).andReturn(45L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(1);

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
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        int costSoFar = 15;  // 23.55
        TramTime elapsed = queryTime.plusMinutes(costSoFar);

        Node node = createMock(Node.class);
        EasyMock.expect(node.getId()).andReturn(42L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(23);
        EasyMock.expect(node.getId()).andReturn(43L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(0);
        EasyMock.expect(node.getId()).andReturn(44L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(1);

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
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        int costSoFar = 15;  // 23.55
        TramTime elapsed = queryTime.plusMinutes(costSoFar);

        Node node = createMock(Node.class);
        EasyMock.expect(node.getId()).andStubReturn(42L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(23);
        EasyMock.expect(node.getId()).andReturn(43L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(0);
        EasyMock.expect(node.getId()).andReturn(44L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(1);

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
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        LocalTime nodeTime = LocalTime.of(8, 0);

        // 7.10 too early, too long to wait
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(10), nodeTime, false, reasons);
        // 7.29 too early, too long to wait
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(29), nodeTime, false, reasons);

        // 8
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(60), nodeTime, true, reasons);
        // 7.30
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(30), nodeTime, true, reasons);
        // 7.45
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(45), nodeTime, true, reasons);
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(59), nodeTime, true, reasons);

        // 8.01 - you missed it
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(61), nodeTime, false, reasons);
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(120), nodeTime, false, reasons);

    }

    @Test
    void shouldCheckTimeAtNodeCorrectlyOvermidnight() {
        TramTime queryTime = TramTime.of(23,50);
        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        LocalTime nodeTime = LocalTime.of(0, 5);

        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(9), nodeTime, true, reasons);
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(10), nodeTime, true, reasons);
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(11), nodeTime, true, reasons);
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(15), nodeTime, true, reasons);

        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(16), nodeTime, false, reasons);
    }

    private void checkForNodeTime(ServiceHeuristics serviceHeuristics, TramTime currentElapsed, LocalTime nodeTime, boolean expect, ServiceReasons reasons) {
        resetAll();

        Node node = createMock(Node.class);
        EasyMock.expect(node.getId()).andStubReturn(42L); // ok IFF node time always same
        EasyMock.expect(node.getProperty(GraphPropertyKey.TIME.getText())).andStubReturn(nodeTime);
        EasyMock.expect(journeyConstraints.getMaxWait()).andStubReturn(MAX_WAIT);

        replayAll();
        assertEquals(expect, serviceHeuristics.checkTime(path, node, currentElapsed, reasons).isValid());
        verifyAll();
    }

    @Test
    void shouldBeInterestedInCorrectHoursOverMidnightLongerJourney() {
        TramTime queryTime = TramTime.of(23,10);
        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        TramTime elapsed = TramTime.of(0,1);
        Node node = createStrictMock(Node.class);

        EasyMock.expect(node.getId()).andReturn(42L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(22);
        EasyMock.expect(node.getId()).andReturn(43L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(23);
        EasyMock.expect(node.getId()).andReturn(44L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(0);
        EasyMock.expect(node.getId()).andReturn(45L);
        EasyMock.expect(node.getProperty(HOUR.getText())).andReturn(1);

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
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository,
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
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository,
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
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeOperations, tramReachabilityRepository,
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
