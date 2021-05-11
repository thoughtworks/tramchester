package com.tramchester.unit.graph;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.caches.NodeTypeCache;
import com.tramchester.graph.caches.PreviousSuccessfulVisits;
import com.tramchester.graph.search.*;
import com.tramchester.graph.search.stateMachine.HowIGotHere;
import com.tramchester.graph.search.stateMachine.RegistersStates;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.graph.search.stateMachine.states.NotStartedState;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.repository.TripRepository;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestStation;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO;
import static com.tramchester.testSupport.reference.TramStations.Shudehill;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TramRouteEvaluatorTest extends EasyMockSupport {

    private Set<Station> destinationStations;
    private ServiceHeuristics serviceHeuristics;
    private NodeContentsRepository nodeOperations;
    private Path path;
    private HowIGotHere howIGotHere;
    private Node node;
    private NodeTypeCache nodeTypeCache;
    private ServiceReasons reasons;
    private TramchesterConfig config;
    private SortsPositions sortsPositions;
    private PreviousSuccessfulVisits previousSuccessfulVisit;
    private LatLong latLongHint;
    private Long destinationNodeId;
    private Relationship lastRelationship;
    private TripRepository tripRepository;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        Station forTest = TestStation.forTest("destinationStationId", "area", "name", new LatLong(1, 1), TransportMode.Tram);
        destinationStations = Collections.singleton(forTest);
        forTest.addRoute(TestEnv.getTramTestRoute());

        nodeTypeCache = createMock(NodeTypeCache.class);
        previousSuccessfulVisit = createMock(PreviousSuccessfulVisits.class);
        tripRepository = createMock(TripRepository.class);

        nodeOperations = createMock(NodeContentsRepository.class);
        ProvidesLocalNow providesLocalNow = new ProvidesLocalNow();

        config = new TestConfig() {
            @Override
            protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
                return Collections.singletonList(new TFGMGTFSSourceTestConfig("data/tram",
                       GTFSTransportationType.tram, TransportMode.Tram));
            }
        };

        latLongHint = TramStations.ManAirport.getLatLong();
        destinationNodeId = 88L;

        JourneyRequest journeyRequest = new JourneyRequest(
                TramServiceDate.of(TestEnv.nextSaturday()), TramTime.of(8,15), false,
                3, config.getMaxJourneyDuration());
        reasons = new ServiceReasons(journeyRequest, TramTime.of(8,15), providesLocalNow, 3);

        serviceHeuristics = createMock(ServiceHeuristics.class);
        sortsPositions = createMock(SortsPositions.class);
        path = createMock(Path.class);
        node = createMock(Node.class);
        lastRelationship = createMock(Relationship.class);

        howIGotHere = HowIGotHere.forTest(42L, 24L);

        EasyMock.expect(node.getId()).andStubReturn(42L);
        EasyMock.expect(lastRelationship.getId()).andStubReturn(24L);

        EasyMock.expect(path.endNode()).andStubReturn(node);
        EasyMock.expect(path.lastRelationship()).andStubReturn(lastRelationship);

    }

    @NotNull
    private NotStartedState getNotStartedState() {
        Set<Long> destinationNodeIds = new HashSet<>();
        destinationNodeIds.add(destinationNodeId);

        RegistersStates registersStates = new RegistersStates();
        TraversalStateFactory traversalStateFactory = new TraversalStateFactory(registersStates, config);

        return new NotStartedState(new TraversalOps(nodeOperations, tripRepository, sortsPositions, destinationStations,
                destinationNodeIds, latLongHint), traversalStateFactory);
    }

    @NotNull
    private TramRouteEvaluator getEvaluator(long destinationNodeId) {
        Set<Long> destinationNodeIds = new HashSet<>();
        destinationNodeIds.add(destinationNodeId);
        return new TramRouteEvaluator(serviceHeuristics, destinationNodeIds, nodeTypeCache, reasons, previousSuccessfulVisit, config);
    }

    @Test
    void shouldHaveReasonsThatInclude() {
        assertEquals(Evaluation.INCLUDE_AND_PRUNE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.Arrived));
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.ServiceDateOk));
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.ServiceTimeOk));
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.NumChangesOK));
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.TimeOk));
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.HourOk));
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.Reachable));
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.ReachableNoCheck));
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.DurationOk));
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.WalkOk));
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.Continue));
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.StationOpen));

    }

    @Test
    void shouldHaveReasonsThatExclude() {
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.LongerPath));
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.SeenBusStationBefore));
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.PathTooLong));
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.TooManyChanges));
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.NotReachable));
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.NotOnQueryDate));
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.TookTooLong));
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.ServiceNotRunningAtTime));

        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.NotAtHour));
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.AlreadyDeparted));
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.NotAtQueryTime));
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.StationClosed));

    }

    @Test
    void shouldMatchDestination() {
        long destinationNodeId = 42;
        TramRouteEvaluator evaluator = getEvaluator(destinationNodeId);

        BranchState<JourneyState> state = new TestBranchState();

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        state.setState(new JourneyState(time, traversalState));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(42L, time)).andReturn(ServiceReason.ReasonCode.PreviousCacheMiss);
        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.Arrived, 42L, TramTime.of(8,15));
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.INCLUDE_AND_PRUNE, result);
        verifyAll();
    }

//    @Test
//    void shouldUseCachedResultForMultipleJourneyInclude() {
//        long destinationNodeId = 42;
//        TramRouteEvaluator evaluator = getEvaluator(destinationNodeId);
//
//        BranchState<JourneyState> state = new TestBranchState();
//
//        TramTime time = TramTime.of(8, 15);
//        NotStartedState traversalState = getNotStartedState();
//        state.setState(new JourneyState(time, traversalState));
//
//        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(42L, time)).andReturn(ServiceReason.ReasonCode.HourOk);
//
//        replayAll();
//        Evaluation result = evaluator.evaluate(path, state);
//        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, result);
//        verifyAll();
//    }

    @Test
    void shouldUseCachedResultForMultipleJourneyExclude() {
        long destinationNodeId = 42;
        TramRouteEvaluator evaluator = getEvaluator(destinationNodeId);

        BranchState<JourneyState> state = new TestBranchState();

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        state.setState(new JourneyState(time, traversalState));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(42L, time)).andReturn(ServiceReason.ReasonCode.NotAtHour);

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfPreviousVisit() {
        long destinationNodeId = 42;
        TramRouteEvaluator evaluator = getEvaluator(destinationNodeId);

        BranchState<JourneyState> state = new TestBranchState();

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        state.setState(new JourneyState(time, traversalState));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(42L, time)).andReturn(ServiceReason.ReasonCode.NotAtQueryTime);

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldPruneIfTooLong() {
        TramRouteEvaluator evaluator = getEvaluator(destinationNodeId);
        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(200);

        BranchState<JourneyState> state = new TestBranchState();
        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        state.setState(new JourneyState(time, traversalState));

        EasyMock.expect(path.length()).andReturn(201);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(42L, time)).andReturn(ServiceReason.ReasonCode.PreviousCacheMiss);

        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.PathTooLong, 42L, TramTime.of(8,15));
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfServiceNotRunningToday() {
        TramRouteEvaluator evaluator = getEvaluator(destinationNodeId);
        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumChangesOK, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.DurationOk, howIGotHere));

        EasyMock.expect(path.length()).andReturn(50);
        BranchState<JourneyState> state = new TestBranchState();
        EasyMock.expect(nodeTypeCache.isTime(node)).andReturn(false);
        EasyMock.expect(nodeTypeCache.isHour(node)).andReturn(false);
        EasyMock.expect(nodeTypeCache.isService(node)).andReturn(true);

        EasyMock.expect(serviceHeuristics.checkServiceDate(node, howIGotHere, reasons)).
                andReturn(ServiceReason.DoesNotRunOnQueryDate(howIGotHere, StringIdFor.createId("nodeServiceId")));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(42L, TramTime.of(8,15))).andReturn(ServiceReason.ReasonCode.PreviousCacheMiss);

        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.NotOnQueryDate, 42L, TramTime.of(8,15));
        EasyMock.expectLastCall();

        replayAll();
        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        state.setState(new JourneyState(time, traversalState));

        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfUnreachableNode() throws TramchesterException {
        TramRouteEvaluator evaluator = getEvaluator(destinationNodeId);
        BranchState<JourneyState> state = new TestBranchState();
        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumChangesOK, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.DurationOk, howIGotHere));

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        JourneyState journeyState = new JourneyState(time, traversalState);
        journeyState.board(TransportMode.Tram);
        state.setState(journeyState);

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(nodeTypeCache.isTime(node)).andReturn(false);
        EasyMock.expect(nodeTypeCache.isHour(node)).andReturn(false);
        EasyMock.expect(nodeTypeCache.isService(node)).andReturn(false);
        EasyMock.expect(nodeTypeCache.isRouteStation(node)).andReturn(true);

        EasyMock.expect(serviceHeuristics.canReachDestination(node, howIGotHere, reasons)).
                andReturn(ServiceReason.StationNotReachable(howIGotHere));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(42L, time)).andReturn(ServiceReason.ReasonCode.PreviousCacheMiss);

        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.NotReachable, 42L, TramTime.of(8,15));
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfStationIsClosed() throws TramchesterException {
        TramRouteEvaluator evaluator = getEvaluator(destinationNodeId);
        BranchState<JourneyState> state = new TestBranchState();
        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumChangesOK, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.DurationOk, howIGotHere));

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        JourneyState journeyState = new JourneyState(time, traversalState);
        journeyState.board(TransportMode.Tram);
        state.setState(journeyState);

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(nodeTypeCache.isTime(node)).andReturn(false);
        EasyMock.expect(nodeTypeCache.isHour(node)).andReturn(false);
        EasyMock.expect(nodeTypeCache.isService(node)).andReturn(false);
        EasyMock.expect(nodeTypeCache.isRouteStation(node)).andReturn(true);

        EasyMock.expect(serviceHeuristics.canReachDestination(node, howIGotHere, reasons)).
                andReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.Reachable, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkStationOpen(node, howIGotHere, reasons)).
                andReturn(ServiceReason.StationClosed(howIGotHere, TramStations.of(Shudehill)));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(42L, time)).andReturn(ServiceReason.ReasonCode.PreviousCacheMiss);
        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.StationClosed, 42L, TramTime.of(8,15));
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldIncludeIfNotOnTramNode() throws TramchesterException {
        TramRouteEvaluator evaluator = getEvaluator(destinationNodeId);
        BranchState<JourneyState> state = new TestBranchState();
        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumChangesOK, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.DurationOk, howIGotHere));

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        JourneyState journeyState = new JourneyState(time, traversalState);
        journeyState.board(TransportMode.Bus);
        state.setState(journeyState);

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(nodeTypeCache.isTime(node)).andReturn(false);
        EasyMock.expect(nodeTypeCache.isHour(node)).andReturn(false);
        EasyMock.expect(nodeTypeCache.isService(node)).andReturn(false);
        EasyMock.expect(nodeTypeCache.isRouteStation(node)).andReturn(true);

        EasyMock.expect(lastRelationship.isType(WALKS_TO)).andReturn(true);

        EasyMock.expect(serviceHeuristics.canReachDestination(node, howIGotHere, reasons)).
                andReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.Reachable, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkStationOpen(node, howIGotHere, reasons)).
                andReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.StationOpen, howIGotHere));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(42L, time)).andReturn(ServiceReason.ReasonCode.PreviousCacheMiss);
        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.WalkOk, 42L, TramTime.of(8,15));
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, result);
        verifyAll();
    }

    @Test
    void shouldIncludeIfWalking() {
        TramRouteEvaluator evaluator = getEvaluator(destinationNodeId);
        BranchState<JourneyState> state = new TestBranchState();
        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumChangesOK, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.DurationOk, howIGotHere));

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(nodeTypeCache.isTime(node)).andReturn(false);
        EasyMock.expect(nodeTypeCache.isHour(node)).andReturn(false);
        EasyMock.expect(nodeTypeCache.isService(node)).andReturn(false);
        EasyMock.expect(nodeTypeCache.isRouteStation(node)).andReturn(false);

        EasyMock.expect(lastRelationship.isType(WALKS_TO)).andReturn(true);

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        state.setState(new JourneyState(time, traversalState));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(42L, time)).andReturn(ServiceReason.ReasonCode.PreviousCacheMiss);
        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.WalkOk, 42L, TramTime.of(8,15));
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfTakingTooLong() {
        TramRouteEvaluator evaluator = getEvaluator(destinationNodeId);
        BranchState<JourneyState> state = new TestBranchState();

        EasyMock.expect(path.length()).andReturn(50);

        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumChangesOK, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumConnectionsOk, howIGotHere));

        TramTime time = TramTime.of(8, 15);
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(0,howIGotHere, reasons)).
                andReturn(ServiceReason.TookTooLong(time, howIGotHere));

        NotStartedState traversalState = getNotStartedState();
        state.setState(new JourneyState(time, traversalState));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(42L, time)).andReturn(ServiceReason.ReasonCode.PreviousCacheMiss);
        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.TookTooLong, 42L, TramTime.of(8,15));
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfOverLimitOnChanges() {
        TramRouteEvaluator evaluator = getEvaluator(destinationNodeId);
        BranchState<JourneyState> state = new TestBranchState();

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);

        TramTime time = TramTime.of(8, 15);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).andStubReturn(ServiceReason.TooManyChanges(howIGotHere));

        NotStartedState traversalState = getNotStartedState();
        state.setState(new JourneyState(time, traversalState));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(42L, time)).andReturn(ServiceReason.ReasonCode.PreviousCacheMiss);
        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.TooManyChanges, 42L, TramTime.of(8,15));
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfServiceNotCorrectHour() {
        TramRouteEvaluator evaluator = getEvaluator(destinationNodeId);
        BranchState<JourneyState> state = new TestBranchState();
        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumChangesOK, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumConnectionsOk, howIGotHere));

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(nodeTypeCache.isTime(node)).andReturn(false);
        EasyMock.expect(nodeTypeCache.isHour(node)).andReturn(true);

        //EasyMock.expect(lastRelationship.isType(WALKS_TO)).andReturn(false);

        NotStartedState traversalState = getNotStartedState();
        TramTime time = TramTime.of(8, 15);

        state.setState(new JourneyState(time, traversalState));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(0,howIGotHere, reasons)).
                andReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.DurationOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.interestedInHour(howIGotHere, node, time, reasons, config.getMaxInitialWait())).
                andReturn(ServiceReason.DoesNotOperateOnTime(time, howIGotHere));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(42L, time)).andReturn(ServiceReason.ReasonCode.PreviousCacheMiss);
        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.NotAtHour, 42L, TramTime.of(8,15));
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfServiceNotCorrectMinute() throws TramchesterException {
        TramRouteEvaluator evaluator = getEvaluator(destinationNodeId);
        BranchState<JourneyState> state = new TestBranchState();
        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumChangesOK, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumConnectionsOk, howIGotHere));

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(nodeTypeCache.isTime(node)).andReturn(true);

//        EasyMock.expect(lastRelationship.isType(WALKS_TO)).andReturn(false);

        NotStartedState traversalState = getNotStartedState();
        TramTime time = TramTime.of(8, 15);
        JourneyState journeyState = new JourneyState(time, traversalState);
        journeyState.board(TransportMode.Tram); // So uses non-initial wait time
        state.setState(journeyState);

        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(0 ,howIGotHere, reasons)).
                andReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.DurationOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkTime(howIGotHere, node, time, reasons, config.getMaxWait())).
                andReturn(ServiceReason.DoesNotOperateOnTime(time, howIGotHere));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(42L, time)).andReturn(ServiceReason.ReasonCode.PreviousCacheMiss);
        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.NotAtQueryTime, 42L, TramTime.of(8,15));
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldIncludeIfMatchesNoRules() {
        TramRouteEvaluator evaluator = getEvaluator(destinationNodeId);
        BranchState<JourneyState> state = new TestBranchState();
        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumChangesOK, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumConnectionsOk, howIGotHere));

        //EasyMock.expect(node.getLabels()).andReturn(Collections.singleton(GraphBuilder.Labels.TRAM_STATION));

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(nodeTypeCache.isService(node)).andReturn(false);
        EasyMock.expect(nodeTypeCache.isRouteStation(node)).andReturn(false);
        EasyMock.expect(nodeTypeCache.isHour(node)).andStubReturn(false);
        EasyMock.expect(nodeTypeCache.isTime(node)).andStubReturn(false);

        EasyMock.expect(lastRelationship.isType(WALKS_TO)).andReturn(false);

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        state.setState(new JourneyState(time, traversalState));

        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(0,howIGotHere, reasons)).
                andReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.DurationOk, howIGotHere));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(42L, time)).andReturn(ServiceReason.ReasonCode.PreviousCacheMiss);
        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.Continue, 42L, TramTime.of(8,15));
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, result);
        verifyAll();
    }

    private static class TestBranchState implements BranchState<JourneyState> {
        private JourneyState journeyState;

        @Override
        public JourneyState getState() {
            return journeyState;
        }

        @Override
        public void setState(JourneyState journeyState) {
            this.journeyState = journeyState;
        }
    }

}
