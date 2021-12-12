package com.tramchester.unit.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.CachedNodeOperations;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.search.*;
import com.tramchester.graph.search.stateMachine.HowIGotHere;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteInterchanges;
import com.tramchester.repository.StationRepository;
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
import java.util.EnumSet;
import java.util.Set;

import static com.tramchester.graph.graphbuild.GraphLabel.*;
import static com.tramchester.testSupport.reference.TramStations.Bury;
import static com.tramchester.testSupport.reference.TramStations.Shudehill;
import static org.junit.jupiter.api.Assertions.*;

class ServiceHeuristicsTest extends EasyMockSupport {

    private static final int MAX_WAIT = 30;
    private static final int MAX_NUM_CHANGES = 5;
    private final Set<Station> endStations = Collections.singleton(TramStations.of(TramStations.Deansgate));

    private final TramchesterConfig config30MinsWait = new NeedMaxWaitConfig(MAX_WAIT);
    private NodeContentsRepository nodeContentsCache;
    private HowIGotHere howIGotHere;
    private StationRepository stationRepository;
    private ProvidesLocalNow providesLocalNow;
    private IdFor<Service> serviceIdA;
    private IdFor<Service> serviceIdB;
    private JourneyConstraints journeyConstraints;
    private int maxJourneyDuration;
    private long maxNumberOfJourneys;
    private LowestCostsForRoutes fewestHopsForRoutes;
    private RouteInterchanges routeInterchanges;

    @BeforeEach
    void beforeEachTestRuns() {
        maxJourneyDuration = config30MinsWait.getMaxJourneyDuration();
        maxNumberOfJourneys = 1;
        providesLocalNow = new ProvidesLocalNow();
        serviceIdA = StringIdFor.createId("serviceIdA");
        serviceIdB = StringIdFor.createId("serviceIdB");

        nodeContentsCache = createMock(CachedNodeOperations.class);
        howIGotHere = createMock(HowIGotHere.class);
        stationRepository = createMock(StationRepository.class);
        fewestHopsForRoutes = createMock(LowestCostsForRoutes.class);
        routeInterchanges = createMock(RouteInterchanges.class);

        int maxPathLength = 400;
        journeyConstraints = createMock(JourneyConstraints.class);
        EasyMock.expect(journeyConstraints.getMaxPathLength()).andStubReturn(maxPathLength);
        EasyMock.expect(journeyConstraints.getEndStations()).andStubReturn(endStations);
        EasyMock.expect(journeyConstraints.getMaxJourneyDuration()).andStubReturn(maxJourneyDuration);
    }

    @NotNull
    private JourneyRequest getJourneyRequest(TramTime queryTime) {
        return new JourneyRequest(new TramServiceDate(TestEnv.nextSaturday()), queryTime, false,
                MAX_NUM_CHANGES, maxJourneyDuration, maxNumberOfJourneys);
    }

    @Test
    void shouldCheckNodeBasedOnServiceId() {
        TramTime queryTime = TramTime.of(8,1);
        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        TramTime visitTime =  queryTime.plusMinutes(35);
        EasyMock.expect(journeyConstraints.isRunning(serviceIdA, visitTime)).andReturn(true);
        EasyMock.expect(journeyConstraints.isRunning(serviceIdB, visitTime)).andReturn(false);
        EasyMock.expect(journeyConstraints.getFewestChangesCalculator()).andReturn(fewestHopsForRoutes);

        Node node = createMock(Node.class);
        EasyMock.expect(nodeContentsCache.getServiceId(node)).andReturn(serviceIdA);
        EasyMock.expect(nodeContentsCache.getServiceId(node)).andReturn(serviceIdB);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, routeInterchanges, nodeContentsCache,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        ServiceReason result = serviceHeuristics.checkServiceDate(node, howIGotHere, reasons, visitTime);
        assertTrue(result.isValid());

        result = serviceHeuristics.checkServiceDate(node, howIGotHere, reasons, visitTime);
        assertEquals(ServiceReason.ReasonCode.NotOnQueryDate, result.getReasonCode());
        verifyAll();
    }

    @Test
    void shouldCheckNodeOpenStation() {
        TramTime queryTime = TramTime.of(8,1);
        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        RouteStation routeStationA = new RouteStation(TramStations.of(Bury), TestEnv.getTramTestRoute());
        RouteStation routeStationB = new RouteStation(TramStations.of(Shudehill), TestEnv.getTramTestRoute());

        EasyMock.expect(journeyConstraints.isClosed(TramStations.of(Bury))).andReturn(false);
        EasyMock.expect(journeyConstraints.isClosed(TramStations.of(Shudehill))).andReturn(true);
        EasyMock.expect(journeyConstraints.getFewestChangesCalculator()).andReturn(fewestHopsForRoutes);

        Node node = createMock(Node.class);

        EasyMock.expect(nodeContentsCache.getRouteStationId(node)).andReturn(routeStationA.getId());
        EasyMock.expect(stationRepository.getRouteStationById(routeStationA.getId())).andReturn(routeStationA);

        EasyMock.expect(nodeContentsCache.getRouteStationId(node)).andReturn(routeStationB.getId());
        EasyMock.expect(stationRepository.getRouteStationById(routeStationB.getId())).andReturn(routeStationB);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, routeInterchanges, nodeContentsCache,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        ServiceReason result = serviceHeuristics.checkStationOpen(node, howIGotHere, reasons);
        assertTrue(result.isValid());

        result = serviceHeuristics.checkStationOpen(node, howIGotHere, reasons);
        assertEquals(ServiceReason.ReasonCode.StationClosed, result.getReasonCode());
        verifyAll();
    }

    @Test
    void shouldBeInterestedInCorrectHours() {
        TramTime queryTime = TramTime.of(9,1);

        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        EasyMock.expect(journeyConstraints.getFewestChangesCalculator()).andReturn(fewestHopsForRoutes);

        // querytime + costSoFar + maxWait (for board) = latest time could arrive here
        // querytime + costSoFar + 0 = earlier time could arrive here

        int costSoFar = 58; // 9.59
        TramTime elapsed = queryTime.plusMinutes(costSoFar);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, routeInterchanges, nodeContentsCache,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        assertFalse(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_8)).isValid());
        assertTrue(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_9)).isValid());
        assertTrue(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_10)).isValid());
        assertFalse(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_11)).isValid());
        verifyAll();
    }

    @Test
    void shouldBeInterestedInCorrectHoursCrossesNextHour() {
        TramTime queryTime = TramTime.of(7,0);
        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        EasyMock.expect(journeyConstraints.getFewestChangesCalculator()).andReturn(fewestHopsForRoutes);

        TramTime elapsed = TramTime.of(10,29);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, routeInterchanges, nodeContentsCache,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        assertFalse(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_8)).isValid());
        assertFalse(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_9)).isValid());
        assertTrue(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_10)).isValid());
        assertFalse(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_11)).isValid());
        verifyAll();
    }

    @Test
    void shouldBeInterestedInCorrectHoursPriorToMidnight() {
        TramTime queryTime = TramTime.of(23,10);
        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        EasyMock.expect(journeyConstraints.getFewestChangesCalculator()).andReturn(fewestHopsForRoutes);

        int costSoFar = 15;  // 23.25
        TramTime elapsed = queryTime.plusMinutes(costSoFar);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, routeInterchanges, nodeContentsCache,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        assertFalse(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_22)).isValid());
        assertTrue(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_23)).isValid());
        assertFalse(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_0)).isValid());
        assertFalse(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_1)).isValid());
        verifyAll();
    }

    @Test
    void shouldBeInterestedInCorrectHoursPriorAcrossMidnight() {
        TramTime queryTime = TramTime.of(23,40);
        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        EasyMock.expect(journeyConstraints.getFewestChangesCalculator()).andReturn(fewestHopsForRoutes);

        int costSoFar = 15;  // 23.55
        TramTime elapsed = queryTime.plusMinutes(costSoFar);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, routeInterchanges, nodeContentsCache,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        assertFalse(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_22)).isValid()); // before
        assertTrue(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_23)).isValid());
        assertTrue(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_0)).isValid());
        assertFalse(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_1)).isValid());
        verifyAll();
    }

    @Test
    void shouldBeInterestedInCorrectHoursEarlyMorning() {
        TramTime queryTime = TramTime.of(0,5);
        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        EasyMock.expect(journeyConstraints.getFewestChangesCalculator()).andReturn(fewestHopsForRoutes);

        int costSoFar = 15;  // = 23.55
        TramTime elapsed = queryTime.plusMinutes(costSoFar);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, routeInterchanges, nodeContentsCache,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        assertFalse(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_23)).isValid());
        assertTrue(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_0)).isValid());
        assertFalse(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_1)).isValid());
        verifyAll();
    }

    @Test
    void shouldBeInterestedInCorrectHoursEarlyMorningNextHour() {
        TramTime queryTime = TramTime.of(0,50);
        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        EasyMock.expect(journeyConstraints.getFewestChangesCalculator()).andReturn(fewestHopsForRoutes);

        int costSoFar = 15;
        TramTime elapsed = queryTime.plusMinutes(costSoFar);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, routeInterchanges, nodeContentsCache,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        assertFalse(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_23)).isValid());
        assertFalse(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_0)).isValid());
        assertTrue(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_1)).isValid());
        verifyAll();
    }

    @Test
    void shouldCheckTimeAtNodeCorrectly() {
        TramTime queryTime = TramTime.of(7,0);
        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        EasyMock.expect(journeyConstraints.getFewestChangesCalculator()).andReturn(fewestHopsForRoutes);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, routeInterchanges, nodeContentsCache,
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
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        EasyMock.expect(journeyConstraints.getFewestChangesCalculator()).andReturn(fewestHopsForRoutes);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, routeInterchanges, nodeContentsCache,
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

        EasyMock.expect(nodeContentsCache.getTime(node)).andReturn(tramTime);

        replayAll();
        assertEquals(expect, serviceHeuristics.checkTime(howIGotHere, node, currentElapsed, reasons, MAX_WAIT).isValid());
        verifyAll();
    }

    @Test
    void shouldBeInterestedInCorrectHoursOverMidnightLongerJourney() {
        TramTime queryTime = TramTime.of(23,10);
        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        EasyMock.expect(journeyConstraints.getFewestChangesCalculator()).andReturn(fewestHopsForRoutes);

        TramTime elapsed = TramTime.of(0,1);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, routeInterchanges, nodeContentsCache,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        assertFalse(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_22)).isValid());
        assertFalse(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_23)).isValid());
        assertTrue(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_0)).isValid());
        assertFalse(serviceHeuristics.interestedInHour(howIGotHere, elapsed, reasons, MAX_WAIT, EnumSet.of(HOUR_1)).isValid());
        verifyAll();
    }

    @Test
    void shouldCheckMaximumDurationCorrectly() {
        TramTime queryTime = TramTime.of(11,20);
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(TestEnv.nextSaturday()), queryTime,
                false, 3, maxJourneyDuration, maxNumberOfJourneys);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        int overallMaxLen = config30MinsWait.getMaxJourneyDuration();

        EasyMock.expect(journeyConstraints.getFewestChangesCalculator()).andReturn(fewestHopsForRoutes);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, routeInterchanges, nodeContentsCache,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        assertTrue(serviceHeuristics.journeyDurationUnderLimit(5, howIGotHere, reasons).isValid());
        assertTrue(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen-1, howIGotHere, reasons).isValid());
        assertTrue(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen, howIGotHere, reasons).isValid());
        assertFalse(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen+1, howIGotHere, reasons).isValid());

        verifyAll();
    }

    @Test
    void shouldCheckChangeLimit() {
        TramTime queryTime = TramTime.of(11,20);
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(TestEnv.nextSaturday()), queryTime,
                false, 2, 160, maxNumberOfJourneys);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        EasyMock.expect(journeyConstraints.getFewestChangesCalculator()).andReturn(fewestHopsForRoutes);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, routeInterchanges, nodeContentsCache,
                journeyConstraints, queryTime,
                2);

        assertTrue(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons).isValid());
        assertTrue(serviceHeuristics.checkNumberChanges(1, howIGotHere, reasons).isValid());
        assertTrue(serviceHeuristics.checkNumberChanges(2, howIGotHere, reasons).isValid());
        assertFalse(serviceHeuristics.checkNumberChanges(3, howIGotHere, reasons).isValid());
        verifyAll();
    }

    @Test
    void shouldCheckReachableForRouteStation() {
        TramTime queryTime = TramTime.of(11,20);
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(TestEnv.nextSaturday()), queryTime,
                false, 2, 160, maxNumberOfJourneys);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        IdFor<Station> stationId = TramStations.Altrincham.getId();
        IdFor<Route> routeId = StringIdFor.createId("currentRoute");
        Route route = TestEnv.getTramTestRoute(routeId, "routeName");
        final RouteStation routeStation = new RouteStation(TramStations.of(TramStations.Altrincham), route);

        Node node = createMock(Node.class);

        TramTime visitTime = queryTime.plusMinutes(20);

        final IdFor<RouteStation> routeStationId = RouteStation.createId(stationId, routeId);
        EasyMock.expect(nodeContentsCache.getRouteStationId(node)).andStubReturn(routeStationId);
        EasyMock.expect(stationRepository.getRouteStationById(routeStationId)).andStubReturn(routeStation);
        EasyMock.expect(journeyConstraints.getFewestChangesCalculator()).andReturn(fewestHopsForRoutes);

        // 1
        EasyMock.expect(journeyConstraints.isUnavailable(route, visitTime)).andReturn(false);
        EasyMock.expect(fewestHopsForRoutes.getFewestChanges(route)).andReturn(1);

        // 2
        EasyMock.expect(journeyConstraints.isUnavailable(route, visitTime)).andReturn(false);
        EasyMock.expect(fewestHopsForRoutes.getFewestChanges(route)).andReturn(2);

        // 3
        EasyMock.expect(journeyConstraints.isUnavailable(route, visitTime)).andReturn(true);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, routeInterchanges, nodeContentsCache,
                journeyConstraints, queryTime,
                2);

        assertTrue(serviceHeuristics.canReachDestination(node, 1, howIGotHere, reasons, visitTime).isValid());
        assertFalse(serviceHeuristics.canReachDestination(node, 1, howIGotHere, reasons, visitTime).isValid());
        assertFalse(serviceHeuristics.canReachDestination(node, 1, howIGotHere, reasons, visitTime).isValid());
        verifyAll();
    }

    @Test
    void shouldCheckMaximumDurationCorrectlyAcrossMidnight() {
        TramTime queryTime = TramTime.of(23,20);
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(TestEnv.nextSaturday()), queryTime,
                false, 3, maxJourneyDuration, maxNumberOfJourneys);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);
        EasyMock.expect(journeyConstraints.getFewestChangesCalculator()).andReturn(fewestHopsForRoutes);

        int overallMaxLen = config30MinsWait.getMaxJourneyDuration();

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, routeInterchanges, nodeContentsCache,
                journeyConstraints, queryTime,
                MAX_NUM_CHANGES);

        assertTrue(serviceHeuristics.journeyDurationUnderLimit(5, howIGotHere, reasons).isValid());
        assertTrue(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen-1, howIGotHere, reasons).isValid());
        assertTrue(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen, howIGotHere, reasons).isValid());
        assertFalse(serviceHeuristics.journeyDurationUnderLimit(overallMaxLen+1, howIGotHere, reasons).isValid());

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
