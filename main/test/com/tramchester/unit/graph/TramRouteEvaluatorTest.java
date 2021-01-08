package com.tramchester.unit.graph;

import com.tramchester.CacheMetrics;
import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.CachedNodeOperations;
import com.tramchester.graph.NodeContentsRepository;
import com.tramchester.graph.NodeIdLabelMap;
import com.tramchester.graph.PreviousSuccessfulVisits;
import com.tramchester.graph.search.*;
import com.tramchester.graph.search.states.HowIGotHere;
import com.tramchester.graph.search.states.NotStartedState;
import com.tramchester.integration.testSupport.TFGMTestDataSourceConfig;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestLiveDataConfig;
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
import org.opengis.referencing.operation.TransformException;

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
    private NodeIdLabelMap nodeIdLabelMap;
    private ServiceReasons reasons;
    private TramchesterConfig config;
    private SortsPositions sortsPositions;
    private PreviousSuccessfulVisits previousSuccessfulVisit;
    private LatLong latLongHint;
    private Long destinationNodeId;
    private Relationship lastRelationship;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        Station forTest = TestStation.forTest("destinationStationId", "area", "name", new LatLong(1, 1), TransportMode.Tram);
        destinationStations = Collections.singleton(forTest);
        forTest.addRoute(TestEnv.getTestRoute());

        nodeIdLabelMap = createMock(NodeIdLabelMap.class);
        previousSuccessfulVisit = createMock(PreviousSuccessfulVisits.class);
        nodeOperations = new CachedNodeOperations(new CacheMetrics(TestEnv.NoopRegisterMetrics()));
        ProvidesLocalNow providesLocalNow = new ProvidesLocalNow();

        config = new TestConfig() {
            @Override
            protected List<DataSourceConfig> getDataSourceFORTESTING() {
                return Collections.singletonList(new TFGMTestDataSourceConfig("data/tram",
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
        return new NotStartedState(sortsPositions, nodeOperations, destinationNodeIds, destinationStations, latLongHint, config);
    }

    @NotNull
    private TramRouteEvaluator getEvaluator(long destinationNodeId) {
        Set<Long> destinationNodeIds = new HashSet<>();
        destinationNodeIds.add(destinationNodeId);
        return new TramRouteEvaluator(serviceHeuristics, destinationNodeIds, nodeIdLabelMap, reasons, previousSuccessfulVisit, config);
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

        EasyMock.expect(previousSuccessfulVisit.hasUsableResult(node, TramTime.of(8,15))).andStubReturn(false);
        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.Arrived, node, TramTime.of(8,15));
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.INCLUDE_AND_PRUNE, result);
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

        EasyMock.expect(previousSuccessfulVisit.hasUsableResult(node, TramTime.of(8,15))).andStubReturn(true);

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

        EasyMock.expect(previousSuccessfulVisit.hasUsableResult(node, TramTime.of(8,15))).andStubReturn(false);
        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.PathTooLong, node, TramTime.of(8,15));
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
        EasyMock.expect(serviceHeuristics.checkNumberConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.DurationOk, howIGotHere));

        EasyMock.expect(path.length()).andReturn(50);
        BranchState<JourneyState> state = new TestBranchState();
        EasyMock.expect(nodeIdLabelMap.isRouteStation(node)).andReturn(false);
        EasyMock.expect(nodeIdLabelMap.isService(node)).andStubReturn(true);

        EasyMock.expect(serviceHeuristics.checkServiceDate(node, howIGotHere, reasons)).
                andReturn(ServiceReason.DoesNotRunOnQueryDate(howIGotHere, IdFor.createId("nodeServiceId")));

        EasyMock.expect(previousSuccessfulVisit.hasUsableResult(node, TramTime.of(8,15))).andStubReturn(false);
        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.NotOnQueryDate, node, TramTime.of(8,15));
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
        EasyMock.expect(serviceHeuristics.checkNumberConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.DurationOk, howIGotHere));

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        JourneyState journeyState = new JourneyState(time, traversalState);
        journeyState.board(TransportMode.Tram);
        state.setState(journeyState);

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(nodeIdLabelMap.isRouteStation(node)).andReturn(true);

        EasyMock.expect(serviceHeuristics.canReachDestination(node, howIGotHere, reasons)).
                andReturn(ServiceReason.StationNotReachable(howIGotHere));
        EasyMock.expect(previousSuccessfulVisit.hasUsableResult(node, TramTime.of(8,15))).andStubReturn(false);
        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.NotReachable, node, TramTime.of(8,15));
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
        EasyMock.expect(serviceHeuristics.checkNumberConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.DurationOk, howIGotHere));

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        JourneyState journeyState = new JourneyState(time, traversalState);
        journeyState.board(TransportMode.Tram);
        state.setState(journeyState);

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(nodeIdLabelMap.isRouteStation(node)).andReturn(true);

        EasyMock.expect(serviceHeuristics.canReachDestination(node, howIGotHere, reasons)).
                andReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.Reachable, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkStationOpen(node, howIGotHere, reasons)).
                andReturn(ServiceReason.StationClosed(howIGotHere, TramStations.of(Shudehill)));
        EasyMock.expect(previousSuccessfulVisit.hasUsableResult(node, TramTime.of(8,15))).andStubReturn(false);
        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.StationClosed, node, TramTime.of(8,15));
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
        EasyMock.expect(serviceHeuristics.checkNumberConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.DurationOk, howIGotHere));

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        JourneyState journeyState = new JourneyState(time, traversalState);
        journeyState.board(TransportMode.Bus);
        state.setState(journeyState);

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(nodeIdLabelMap.isRouteStation(node)).andReturn(true);
        EasyMock.expect(nodeIdLabelMap.isService(node)).andReturn(false);

        EasyMock.expect(lastRelationship.isType(WALKS_TO)).andReturn(true);

        EasyMock.expect(serviceHeuristics.canReachDestination(node, howIGotHere, reasons)).
                andReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.Reachable, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkStationOpen(node, howIGotHere, reasons)).
                andReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.StationOpen, howIGotHere));

        EasyMock.expect(previousSuccessfulVisit.hasUsableResult(node, TramTime.of(8,15))).andStubReturn(false);
        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.WalkOk, node, TramTime.of(8,15));
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
        EasyMock.expect(serviceHeuristics.checkNumberConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.DurationOk, howIGotHere));

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(nodeIdLabelMap.isService(node)).andReturn(false);
        EasyMock.expect(nodeIdLabelMap.isRouteStation(node)).andReturn(false);

        EasyMock.expect(lastRelationship.isType(WALKS_TO)).andReturn(true);

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        state.setState(new JourneyState(time, traversalState));
        EasyMock.expect(previousSuccessfulVisit.hasUsableResult(node, TramTime.of(8,15))).andStubReturn(false);
        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.WalkOk, node, TramTime.of(8,15));
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
        EasyMock.expect(serviceHeuristics.checkNumberConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumConnectionsOk, howIGotHere));

        TramTime time = TramTime.of(8, 15);
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(0,howIGotHere, reasons)).
                andReturn(ServiceReason.TookTooLong(time, howIGotHere));

        NotStartedState traversalState = getNotStartedState();
        state.setState(new JourneyState(time, traversalState));
        EasyMock.expect(previousSuccessfulVisit.hasUsableResult(node, TramTime.of(8,15))).andStubReturn(false);
        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.TookTooLong, node, TramTime.of(8,15));
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
        EasyMock.expect(previousSuccessfulVisit.hasUsableResult(node, TramTime.of(8,15))).andStubReturn(false);
        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.TooManyChanges, node, TramTime.of(8,15));
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfServiceNotRunning() {
        TramRouteEvaluator evaluator = getEvaluator(destinationNodeId);
        BranchState<JourneyState> state = new TestBranchState();
        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumChangesOK, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumConnectionsOk, howIGotHere));

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(nodeIdLabelMap.isService(node)).andReturn(true);
        EasyMock.expect(nodeIdLabelMap.isRouteStation(node)).andReturn(false);

        EasyMock.expect(lastRelationship.isType(WALKS_TO)).andReturn(false);

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();

        state.setState(new JourneyState(time, traversalState));
        EasyMock.expect(serviceHeuristics.checkServiceDate(node, howIGotHere, reasons)).
                andReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.ServiceDateOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(0,howIGotHere, reasons)).
                andReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.DurationOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkServiceTime(howIGotHere, node, time, reasons)).
                andReturn(ServiceReason.DoesNotOperateOnTime(time, howIGotHere));

        EasyMock.expect(previousSuccessfulVisit.hasUsableResult(node, TramTime.of(8,15))).andStubReturn(false);
        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.ServiceNotRunningAtTime, node, TramTime.of(8,15));
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
        EasyMock.expect(serviceHeuristics.checkNumberConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumConnectionsOk, howIGotHere));

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(nodeIdLabelMap.isService(node)).andReturn(false);
        EasyMock.expect(nodeIdLabelMap.isRouteStation(node)).andReturn(false);
        EasyMock.expect(nodeIdLabelMap.isHour(node)).andReturn(true);

        EasyMock.expect(lastRelationship.isType(WALKS_TO)).andReturn(false);

        NotStartedState traversalState = getNotStartedState();
        TramTime time = TramTime.of(8, 15);

        state.setState(new JourneyState(time, traversalState));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(0,howIGotHere, reasons)).
                andReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.DurationOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.interestedInHour(howIGotHere, node, time, reasons)).
                andReturn(ServiceReason.DoesNotOperateOnTime(time, howIGotHere));

        EasyMock.expect(previousSuccessfulVisit.hasUsableResult(node, TramTime.of(8,15))).andStubReturn(false);
        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.NotAtHour, node, TramTime.of(8,15));
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfServiceNotCorrectMinute() {
        TramRouteEvaluator evaluator = getEvaluator(destinationNodeId);
        BranchState<JourneyState> state = new TestBranchState();
        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumChangesOK, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumConnectionsOk, howIGotHere));

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(nodeIdLabelMap.isService(node)).andReturn(false);
        EasyMock.expect(nodeIdLabelMap.isRouteStation(node)).andReturn(false);
        EasyMock.expect(nodeIdLabelMap.isHour(node)).andReturn(false);
        EasyMock.expect(nodeIdLabelMap.isTime(node)).andReturn(true);

        EasyMock.expect(lastRelationship.isType(WALKS_TO)).andReturn(false);

        NotStartedState traversalState = getNotStartedState();
        TramTime time = TramTime.of(8, 15);
        state.setState(new JourneyState(time, traversalState));

        //TramTime tramTime = TramTime.of(time);
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(0 ,howIGotHere, reasons)).
                andReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.DurationOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkTime(howIGotHere, node, time, reasons)).
                andReturn(ServiceReason.DoesNotOperateOnTime(time, howIGotHere));

        EasyMock.expect(previousSuccessfulVisit.hasUsableResult(node, TramTime.of(8,15))).andStubReturn(false);
        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.NotAtQueryTime, node, TramTime.of(8,15));
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
        EasyMock.expect(serviceHeuristics.checkNumberConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.NumConnectionsOk, howIGotHere));

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(nodeIdLabelMap.isService(node)).andReturn(false);
        EasyMock.expect(nodeIdLabelMap.isRouteStation(node)).andReturn(false);
        EasyMock.expect(nodeIdLabelMap.isHour(node)).andStubReturn(false);
        EasyMock.expect(nodeIdLabelMap.isTime(node)).andStubReturn(false);

        EasyMock.expect(lastRelationship.isType(WALKS_TO)).andReturn(false);

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        state.setState(new JourneyState(time, traversalState));

        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(0,howIGotHere, reasons)).
                andReturn(ServiceReason.IsValid(ServiceReason.ReasonCode.DurationOk, howIGotHere));

        EasyMock.expect(previousSuccessfulVisit.hasUsableResult(node, TramTime.of(8,15))).andStubReturn(false);
        previousSuccessfulVisit.recordVisitIfUseful(ServiceReason.ReasonCode.Continue, node, TramTime.of(8,15));
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
