package com.tramchester.unit.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.CachedNodeOperations;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.search.*;
import com.tramchester.graph.search.stateMachine.HowIGotHere;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteInterchangeRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Node;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.TramsOnly;
import static com.tramchester.graph.graphbuild.GraphLabel.*;
import static com.tramchester.testSupport.reference.TramStations.Bury;
import static com.tramchester.testSupport.reference.TramStations.Shudehill;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

class ServiceHeuristicsTest extends EasyMockSupport {

    private static final int MAX_WAIT = 30;
    private static final int MAX_NUM_CHANGES = 5;
    private final LocationSet endStations = LocationSet.singleton(TramStations.Deansgate.fake());

    private final TramchesterConfig config30MinsWait = new NeedMaxWaitConfig(MAX_WAIT);
    private NodeContentsRepository nodeContentsCache;
    private HowIGotHere howIGotHere;
    private StationRepository stationRepository;
    private ProvidesLocalNow providesLocalNow;
    private IdFor<Service> serviceIdA;
    private IdFor<Service> serviceIdB;
    private JourneyConstraints journeyConstraints;
    private Duration maxJourneyDuration;
    private long maxNumberOfJourneys;
    private LowestCostsForDestRoutes fewestHopsForRoutes;
    private RouteInterchangeRepository routeInterchanges;

    @BeforeEach
    void beforeEachTestRuns() {
        maxJourneyDuration = Duration.ofMinutes(config30MinsWait.getMaxJourneyDuration());
        maxNumberOfJourneys = 1;
        providesLocalNow = new ProvidesLocalNow();
        serviceIdA = StringIdFor.createId("serviceIdA");
        serviceIdB = StringIdFor.createId("serviceIdB");

        nodeContentsCache = createMock(CachedNodeOperations.class);
        howIGotHere = createMock(HowIGotHere.class);
        stationRepository = createMock(StationRepository.class);
        fewestHopsForRoutes = createMock(LowestCostsForDestRoutes.class);
        routeInterchanges = createMock(RouteInterchangeRepository.class);

        int maxPathLength = 400;
        journeyConstraints = createMock(JourneyConstraints.class);
        EasyMock.expect(journeyConstraints.getMaxPathLength()).andStubReturn(maxPathLength);
        EasyMock.expect(journeyConstraints.getEndStations()).andStubReturn(endStations);
        EasyMock.expect(journeyConstraints.getMaxJourneyDuration()).andStubReturn(maxJourneyDuration);
        EasyMock.expect(howIGotHere.getEndNodeId()).andStubReturn(42L);
    }

    @NotNull
    private JourneyRequest getJourneyRequest(TramTime queryTime) {
        return new JourneyRequest(TestEnv.nextSaturday(), queryTime, false,
                MAX_NUM_CHANGES, maxJourneyDuration, maxNumberOfJourneys, getRequestedModes());
    }

    private Set<TransportMode> getRequestedModes() {
        return TramsOnly;
    }

    @Test
    void shouldCheckNodeBasedOnServiceIdAndServiceDates() {
        TramTime queryTime = TramTime.of(8,1);
        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        TramTime visitTime =  queryTime.plusMinutes(35);
        EasyMock.expect(journeyConstraints.isRunningOnDate(serviceIdA, visitTime)).andReturn(true);
        EasyMock.expect(journeyConstraints.isRunningAtTime(serviceIdA, visitTime, MAX_WAIT)).andReturn(true);
        EasyMock.expect(journeyConstraints.isRunningOnDate(serviceIdB, visitTime)).andReturn(false);
        EasyMock.expect(journeyConstraints.getFewestChangesCalculator()).andReturn(fewestHopsForRoutes);

        Node node = createMock(Node.class);
        EasyMock.expect(nodeContentsCache.getServiceId(node)).andReturn(serviceIdA);
        EasyMock.expect(nodeContentsCache.getServiceId(node)).andReturn(serviceIdB);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, routeInterchanges, nodeContentsCache,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        ServiceReason result = serviceHeuristics.checkServiceDateAndTime(node, howIGotHere, reasons, visitTime, MAX_WAIT);
        assertTrue(result.isValid());

        result = serviceHeuristics.checkServiceDateAndTime(node, howIGotHere, reasons, visitTime, MAX_WAIT);
        assertEquals(ServiceReason.ReasonCode.NotOnQueryDate, result.getReasonCode());
        verifyAll();
    }

    @Test
    void shouldCheckNodeBasedOnServiceIdAndServiceTimings() {
        TramTime queryTime = TramTime.of(8,1);
        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        TramTime visitTime =  queryTime.plusMinutes(35);
        EasyMock.expect(journeyConstraints.isRunningOnDate(serviceIdA, visitTime)).andReturn(true);
        EasyMock.expect(journeyConstraints.isRunningAtTime(serviceIdA, visitTime, MAX_WAIT)).andReturn(true);
        EasyMock.expect(journeyConstraints.isRunningOnDate(serviceIdB, visitTime)).andReturn(true);
        EasyMock.expect(journeyConstraints.isRunningAtTime(serviceIdB, visitTime, MAX_WAIT)).andReturn(false);
        EasyMock.expect(journeyConstraints.getFewestChangesCalculator()).andReturn(fewestHopsForRoutes);

        Node node = createMock(Node.class);
        EasyMock.expect(nodeContentsCache.getServiceId(node)).andReturn(serviceIdA);
        EasyMock.expect(nodeContentsCache.getServiceId(node)).andReturn(serviceIdB);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, routeInterchanges, nodeContentsCache,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        ServiceReason result = serviceHeuristics.checkServiceDateAndTime(node, howIGotHere, reasons, visitTime, MAX_WAIT);
        assertTrue(result.isValid());

        result = serviceHeuristics.checkServiceDateAndTime(node, howIGotHere, reasons, visitTime, MAX_WAIT);
        assertEquals(ServiceReason.ReasonCode.ServiceNotRunningAtTime, result.getReasonCode());
        verifyAll();

    }

    @Test
    void shouldCheckNodeOpenStation() {
        TramTime queryTime = TramTime.of(8,1);
        JourneyRequest journeyRequest = getJourneyRequest(queryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        RouteStation routeStationA = new RouteStation(Bury.fake(), TestEnv.getTramTestRoute());
        RouteStation routeStationB = new RouteStation(Shudehill.fake(), TestEnv.getTramTestRoute());

        EasyMock.expect(journeyConstraints.isClosed(Bury.fake())).andReturn(false);
        EasyMock.expect(journeyConstraints.isClosed(Shudehill.fake())).andReturn(true);
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
        TramTime baseQueryTime = TramTime.of(7,0);
        JourneyRequest journeyRequest = getJourneyRequest(baseQueryTime);
        ServiceReasons reasons = new ServiceReasons(journeyRequest, baseQueryTime, providesLocalNow);

        EasyMock.expect(journeyConstraints.getFewestChangesCalculator()).andReturn(fewestHopsForRoutes);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, routeInterchanges, nodeContentsCache,
                journeyConstraints, baseQueryTime, MAX_NUM_CHANGES);

        LocalTime nodeTime = LocalTime.of(8, 0);

        // 7.10 too early, too long to wait
        checkForNodeTime(serviceHeuristics, baseQueryTime.plusMinutes(10), nodeTime, false, reasons, false);
        // 7.29 too early, too long to wait
        checkForNodeTime(serviceHeuristics, baseQueryTime.plusMinutes(29), nodeTime, false, reasons, false);

        // 8
        checkForNodeTime(serviceHeuristics, baseQueryTime.plusMinutes(60), nodeTime, true, reasons, false);
        // 7.30
        checkForNodeTime(serviceHeuristics, baseQueryTime.plusMinutes(30), nodeTime, true, reasons, false);
        // 7.45
        checkForNodeTime(serviceHeuristics, baseQueryTime.plusMinutes(45), nodeTime, true, reasons, false);
        checkForNodeTime(serviceHeuristics, baseQueryTime.plusMinutes(59), nodeTime, true, reasons, false);

        // 8.01 - you missed it
        checkForNodeTime(serviceHeuristics, baseQueryTime.plusMinutes(61), nodeTime, false, reasons, false);
        checkForNodeTime(serviceHeuristics, baseQueryTime.plusMinutes(120), nodeTime, false, reasons, false);

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

        ////////////////
        resetAll();
        ////////////////

        EasyMock.expect(howIGotHere.getEndNodeId()).andStubReturn(42L);

        Node node = createMock(Node.class);

        TramTime tramTime = nextDay ? TramTime.nextDay(nodeTime.getHour(), nodeTime.getMinute()) : TramTime.ofHourMins(nodeTime);

        EasyMock.expect(nodeContentsCache.getTime(node)).andReturn(tramTime);

        replayAll();
        ServiceReason serviceReason = serviceHeuristics.checkTime(howIGotHere, node, currentElapsed, reasons, MAX_WAIT);
        assertEquals(expect, serviceReason.isValid(), format("currentElapsed: %s nodeTime: %s wait: %s reason: %s",
                currentElapsed, tramTime, MAX_WAIT, serviceReason));
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
        JourneyRequest journeyRequest = new JourneyRequest(TestEnv.nextSaturday(), queryTime,
                false, 3, maxJourneyDuration, maxNumberOfJourneys, getRequestedModes());
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        int overallMaxLen = config30MinsWait.getMaxJourneyDuration();

        EasyMock.expect(journeyConstraints.getFewestChangesCalculator()).andReturn(fewestHopsForRoutes);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, routeInterchanges, nodeContentsCache,
                journeyConstraints, queryTime, MAX_NUM_CHANGES);

        assertTrue(serviceHeuristics.journeyDurationUnderLimit(Duration.ofMinutes(5), howIGotHere, reasons).isValid());
        assertTrue(serviceHeuristics.journeyDurationUnderLimit(Duration.ofMinutes(overallMaxLen-1), howIGotHere, reasons).isValid());
        assertTrue(serviceHeuristics.journeyDurationUnderLimit(Duration.ofMinutes(overallMaxLen), howIGotHere, reasons).isValid());
        assertFalse(serviceHeuristics.journeyDurationUnderLimit(Duration.ofMinutes(overallMaxLen+1), howIGotHere, reasons).isValid());

        verifyAll();
    }

    @Test
    void shouldCheckChangeLimit() {
        TramTime queryTime = TramTime.of(11,20);
        JourneyRequest journeyRequest = new JourneyRequest(TestEnv.nextSaturday(), queryTime,
                false, 2, Duration.ofMinutes(160), maxNumberOfJourneys, getRequestedModes());
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
        JourneyRequest journeyRequest = new JourneyRequest(TestEnv.nextSaturday(), queryTime,
                false, 2, Duration.ofMinutes(160), maxNumberOfJourneys, getRequestedModes());
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);

        IdFor<Station> stationId = TramStations.Altrincham.getId();
        IdFor<Route> routeId = StringIdFor.createId("currentRoute");
        Route route = TestEnv.getTramTestRoute(routeId, "routeName");
        final RouteStation routeStation = new RouteStation(TramStations.Altrincham.fake(), route);

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
        JourneyRequest journeyRequest = new JourneyRequest(TestEnv.nextSaturday(), queryTime,
                false, 3, maxJourneyDuration, maxNumberOfJourneys, getRequestedModes());
        ServiceReasons reasons = new ServiceReasons(journeyRequest, queryTime, providesLocalNow);
        EasyMock.expect(journeyConstraints.getFewestChangesCalculator()).andReturn(fewestHopsForRoutes);

        int overallMaxLen = config30MinsWait.getMaxJourneyDuration();

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, routeInterchanges, nodeContentsCache,
                journeyConstraints, queryTime,
                MAX_NUM_CHANGES);

        assertTrue(serviceHeuristics.journeyDurationUnderLimit(Duration.ofMinutes(5), howIGotHere, reasons).isValid());
        assertTrue(serviceHeuristics.journeyDurationUnderLimit(Duration.ofMinutes(overallMaxLen-1), howIGotHere, reasons).isValid());
        assertTrue(serviceHeuristics.journeyDurationUnderLimit(Duration.ofMinutes(overallMaxLen), howIGotHere, reasons).isValid());
        assertFalse(serviceHeuristics.journeyDurationUnderLimit(Duration.ofMinutes(overallMaxLen+1), howIGotHere, reasons).isValid());

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
