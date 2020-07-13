package com.tramchester.unit.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.exceptions.TramchesterException;
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
import com.tramchester.graph.search.states.NotStartedState;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TramRouteEvaluatorTest extends EasyMockSupport {

    private final Set<String> destinationStationIds = Collections.singleton("destinationStationId");
    private ServiceHeuristics serviceHeuristics;
    private NodeContentsRepository nodeOperations;
    private Path path;
    private Node node;
    private NodeIdLabelMap nodeIdLabelMap;
    private ServiceReasons reasons;
    private TramchesterConfig config;
    private SortsPositions sortsPositions;
    private PreviousSuccessfulVisits previousSuccessfulVisit;
    private LatLong latLongHint;
    private Long destinationNodeId;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        nodeIdLabelMap = createMock(NodeIdLabelMap.class);
        previousSuccessfulVisit = createMock(PreviousSuccessfulVisits.class);
        nodeOperations = new CachedNodeOperations();
        ProvidesLocalNow providesLocalNow = new ProvidesLocalNow();
        config = TestEnv.GET();

        latLongHint = TestEnv.manAirportLocation;
        destinationNodeId = 88L;

        JourneyRequest journeyRequest = new JourneyRequest(
                TramServiceDate.of(TestEnv.nextSaturday()), TramTime.of(8,15), false,
                3, config.getMaxJourneyDuration());
        reasons = new ServiceReasons(journeyRequest, TramTime.of(8,15), providesLocalNow);

        serviceHeuristics = createMock(ServiceHeuristics.class);
        sortsPositions = createMock(SortsPositions.class);
        path = createMock(Path.class);
        node = createMock(Node.class);

        EasyMock.expect(path.endNode()).andStubReturn(node);
        EasyMock.expect(node.getId()).andStubReturn(42L);
    }

    @NotNull
    private NotStartedState getNotStartedState() {
        Set<Long> destinationNodeIds = new HashSet<>();
        destinationNodeIds.add(destinationNodeId);
        return new NotStartedState(sortsPositions, nodeOperations, destinationNodeIds, destinationStationIds, latLongHint, config);
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
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, TramRouteEvaluator.decideEvaluationAction(ServiceReason.ReasonCode.Valid));
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
        previousSuccessfulVisit.recordVisitIfUseful(Evaluation.INCLUDE_AND_PRUNE, node, TramTime.of(8,15));
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.INCLUDE_AND_PRUNE, result);
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
        previousSuccessfulVisit.recordVisitIfUseful(Evaluation.EXCLUDE_AND_PRUNE, node, TramTime.of(8,15));
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
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, path, reasons)).andStubReturn(ServiceReason.IsValid(path));

        EasyMock.expect(path.length()).andReturn(50);
        BranchState<JourneyState> state = new TestBranchState();
        EasyMock.expect(nodeIdLabelMap.isRouteStation(node)).andReturn(false);
        EasyMock.expect(nodeIdLabelMap.isService(node)).andStubReturn(true);

        EasyMock.expect(serviceHeuristics.checkServiceDate(node, path, reasons)).
                andReturn(ServiceReason.DoesNotRunOnQueryDate(path));

        EasyMock.expect(previousSuccessfulVisit.hasUsableResult(node, TramTime.of(8,15))).andStubReturn(false);
        previousSuccessfulVisit.recordVisitIfUseful(Evaluation.EXCLUDE_AND_PRUNE, node, TramTime.of(8,15));
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
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, path, reasons)).andStubReturn(ServiceReason.IsValid(path));

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        JourneyState journeyState = new JourneyState(time, traversalState);
        journeyState.boardTram();
        state.setState(journeyState);

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(nodeIdLabelMap.isRouteStation(node)).andReturn(true);

        EasyMock.expect(serviceHeuristics.canReachDestination(node, path, reasons)).
                andReturn(ServiceReason.StationNotReachable(path));
        EasyMock.expect(previousSuccessfulVisit.hasUsableResult(node, TramTime.of(8,15))).andStubReturn(false);
        previousSuccessfulVisit.recordVisitIfUseful(Evaluation.EXCLUDE_AND_PRUNE, node, TramTime.of(8,15));
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
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, path, reasons)).andStubReturn(ServiceReason.IsValid(path));

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        JourneyState journeyState = new JourneyState(time, traversalState);
        journeyState.boardBus();
        state.setState(journeyState);

        Relationship relationship = createMock(Relationship.class);

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(nodeIdLabelMap.isRouteStation(node)).andReturn(true);
        EasyMock.expect(nodeIdLabelMap.isService(node)).andReturn(false);
        EasyMock.expect(relationship.isType(WALKS_TO)).andReturn(true);

        EasyMock.expect(path.lastRelationship()).andReturn(relationship);

        EasyMock.expect(serviceHeuristics.canReachDestination(node, path, reasons)).
                andReturn(ServiceReason.IsValid(path));

        EasyMock.expect(previousSuccessfulVisit.hasUsableResult(node, TramTime.of(8,15))).andStubReturn(false);
        previousSuccessfulVisit.recordVisitIfUseful(Evaluation.INCLUDE_AND_CONTINUE, node, TramTime.of(8,15));
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
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, path, reasons)).andStubReturn(ServiceReason.IsValid(path));

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(nodeIdLabelMap.isService(node)).andReturn(false);
        EasyMock.expect(nodeIdLabelMap.isRouteStation(node)).andReturn(false);

        Relationship relationship = createMock(Relationship.class);
        EasyMock.expect(relationship.isType(WALKS_TO)).andReturn(true);
        EasyMock.expect(path.lastRelationship()).andReturn(relationship);

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        state.setState(new JourneyState(time, traversalState));
        EasyMock.expect(previousSuccessfulVisit.hasUsableResult(node, TramTime.of(8,15))).andStubReturn(false);
        previousSuccessfulVisit.recordVisitIfUseful(Evaluation.INCLUDE_AND_CONTINUE, node, TramTime.of(8,15));
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
        EasyMock.expect(nodeIdLabelMap.isService(node)).andReturn(false);
        EasyMock.expect(nodeIdLabelMap.isRouteStation(node)).andReturn(false);
        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, path, reasons)).andStubReturn(ServiceReason.IsValid(path));

        Relationship relationship = createMock(Relationship.class);
        EasyMock.expect(relationship.isType(WALKS_TO)).andReturn(false);
        EasyMock.expect(path.lastRelationship()).andReturn(relationship);

        TramTime time = TramTime.of(8, 15);
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(0,path, reasons)).andReturn(ServiceReason.TookTooLong(time, path));

        NotStartedState traversalState = getNotStartedState();
        state.setState(new JourneyState(time, traversalState));
        EasyMock.expect(previousSuccessfulVisit.hasUsableResult(node, TramTime.of(8,15))).andStubReturn(false);
        previousSuccessfulVisit.recordVisitIfUseful(Evaluation.EXCLUDE_AND_PRUNE, node, TramTime.of(8,15));
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
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, path, reasons)).andStubReturn(ServiceReason.TooManyChanges(path));

        NotStartedState traversalState = getNotStartedState();
        state.setState(new JourneyState(time, traversalState));
        EasyMock.expect(previousSuccessfulVisit.hasUsableResult(node, TramTime.of(8,15))).andStubReturn(false);
        previousSuccessfulVisit.recordVisitIfUseful(Evaluation.EXCLUDE_AND_PRUNE, node, TramTime.of(8,15));
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
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, path, reasons)).andStubReturn(ServiceReason.IsValid(path));

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(nodeIdLabelMap.isService(node)).andReturn(true);
        EasyMock.expect(nodeIdLabelMap.isRouteStation(node)).andReturn(false);

        Relationship relationship = createMock(Relationship.class);
        EasyMock.expect(relationship.isType(WALKS_TO)).andReturn(false);
        EasyMock.expect(path.lastRelationship()).andReturn(relationship);

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();

        state.setState(new JourneyState(time, traversalState));
        EasyMock.expect(serviceHeuristics.checkServiceDate(node, path, reasons)).andReturn(ServiceReason.IsValid(path));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(0,path, reasons)).andReturn(ServiceReason.IsValid(path));
        EasyMock.expect(serviceHeuristics.checkServiceTime(path, node, time, reasons)).
                andReturn(ServiceReason.DoesNotOperateOnTime(time, path));

        EasyMock.expect(previousSuccessfulVisit.hasUsableResult(node, TramTime.of(8,15))).andStubReturn(false);
        previousSuccessfulVisit.recordVisitIfUseful(Evaluation.EXCLUDE_AND_PRUNE, node, TramTime.of(8,15));
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
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, path, reasons)).andStubReturn(ServiceReason.IsValid(path));

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(nodeIdLabelMap.isService(node)).andReturn(false);
        EasyMock.expect(nodeIdLabelMap.isRouteStation(node)).andReturn(false);
        EasyMock.expect(nodeIdLabelMap.isHour(node)).andReturn(true);

        Relationship relationship = createMock(Relationship.class);
        EasyMock.expect(relationship.isType(WALKS_TO)).andReturn(false);
        EasyMock.expect(path.lastRelationship()).andReturn(relationship);

        NotStartedState traversalState = getNotStartedState();
        TramTime time = TramTime.of(8, 15);

        state.setState(new JourneyState(time, traversalState));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(0,path, reasons)).andReturn(ServiceReason.IsValid(path));
        EasyMock.expect(serviceHeuristics.interestedInHour(path, node, time, reasons)).
                andReturn(ServiceReason.DoesNotOperateOnTime(time, path));

        EasyMock.expect(previousSuccessfulVisit.hasUsableResult(node, TramTime.of(8,15))).andStubReturn(false);
        previousSuccessfulVisit.recordVisitIfUseful(Evaluation.EXCLUDE_AND_PRUNE, node, TramTime.of(8,15));
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
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, path, reasons)).andStubReturn(ServiceReason.IsValid(path));

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(nodeIdLabelMap.isService(node)).andReturn(false);
        EasyMock.expect(nodeIdLabelMap.isRouteStation(node)).andReturn(false);
        EasyMock.expect(nodeIdLabelMap.isHour(node)).andReturn(false);
        EasyMock.expect(nodeIdLabelMap.isTime(node)).andReturn(true);

        Relationship relationship = createMock(Relationship.class);
        EasyMock.expect(relationship.isType(WALKS_TO)).andReturn(false);
        EasyMock.expect(path.lastRelationship()).andReturn(relationship);

        NotStartedState traversalState = getNotStartedState();
        TramTime time = TramTime.of(8, 15);
        state.setState(new JourneyState(time, traversalState));

        //TramTime tramTime = TramTime.of(time);
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(0 ,path, reasons)).andReturn(ServiceReason.IsValid(path));
        EasyMock.expect(serviceHeuristics.checkTime(path, node, time, reasons)).
                andReturn(ServiceReason.DoesNotOperateOnTime(time, path));

        EasyMock.expect(previousSuccessfulVisit.hasUsableResult(node, TramTime.of(8,15))).andStubReturn(false);
        previousSuccessfulVisit.recordVisitIfUseful(Evaluation.EXCLUDE_AND_PRUNE, node, TramTime.of(8,15));
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
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, path, reasons)).andStubReturn(ServiceReason.IsValid(path));

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(nodeIdLabelMap.isService(node)).andReturn(false);
        EasyMock.expect(nodeIdLabelMap.isRouteStation(node)).andReturn(false);
        EasyMock.expect(nodeIdLabelMap.isHour(node)).andStubReturn(false);
        EasyMock.expect(nodeIdLabelMap.isTime(node)).andStubReturn(false);

        Relationship relationship = createMock(Relationship.class);
        EasyMock.expect(relationship.isType(WALKS_TO)).andReturn(false);
        EasyMock.expect(path.lastRelationship()).andReturn(relationship);

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        state.setState(new JourneyState(time, traversalState));

        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(0,path, reasons)).andReturn(ServiceReason.IsValid(path));

        EasyMock.expect(previousSuccessfulVisit.hasUsableResult(node, TramTime.of(8,15))).andStubReturn(false);
        previousSuccessfulVisit.recordVisitIfUseful(Evaluation.INCLUDE_AND_CONTINUE, node, TramTime.of(8,15));
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
