package com.tramchester.unit.graph;

import com.tramchester.domain.TramTime;
import com.tramchester.graph.*;
import com.tramchester.graph.states.NotStartedState;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;

import java.time.LocalTime;

import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO;
import static org.junit.Assert.assertEquals;

public class TramRouteEvaluatorTest extends EasyMockSupport {

    private ServiceHeuristics serviceHeuristics;
    private CachedNodeOperations nodeOperations;
    private Path path;
    private Node node;

    @Before
    public void onceBeforeEachTestRuns() {
        nodeOperations = new CachedNodeOperations();

        serviceHeuristics = createMock(ServiceHeuristics.class);
        path = createMock(Path.class);
        node = createMock(Node.class);

        EasyMock.expect(path.endNode()).andReturn(node);
        EasyMock.expect(node.getId()).andStubReturn(42L);
    }

    @Test
    public void shouldMatchDestination() {
        long destinationNodeId = 42;
        TramRouteEvaluator evaluator = new TramRouteEvaluator(serviceHeuristics, nodeOperations, destinationNodeId);

        BranchState<JourneyState> state = new TestBranchState();

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.INCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    public void shouldPruneIfTooLong() {
        TramRouteEvaluator evaluator = new TramRouteEvaluator(serviceHeuristics, nodeOperations, 88L);

        EasyMock.expect(path.length()).andReturn(401);
        BranchState<JourneyState> state = new TestBranchState();

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    public void shouldExcludeIfServiceNotRunningToday() {
        TramRouteEvaluator evaluator = new TramRouteEvaluator(serviceHeuristics, nodeOperations, 88L);

        EasyMock.expect(path.length()).andReturn(50);
        BranchState<JourneyState> state = new TestBranchState();
        EasyMock.expect(node.hasLabel(TransportGraphBuilder.Labels.SERVICE)).andReturn(true);
        EasyMock.expect(serviceHeuristics.checkServiceDate(node,path)).
                andReturn(ServiceReason.DoesNotRunOnQueryDate("not running", path));

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    public void shouldExcludeIfUnreachableNode() {
        TramRouteEvaluator evaluator = new TramRouteEvaluator(serviceHeuristics, nodeOperations, 88L);
        BranchState<JourneyState> state = new TestBranchState();

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(node.hasLabel(TransportGraphBuilder.Labels.SERVICE)).andReturn(false);
        EasyMock.expect(node.hasLabel(TransportGraphBuilder.Labels.ROUTE_STATION)).andReturn(true);
        EasyMock.expect(serviceHeuristics.canReachDestination(node,path)).
                andReturn(ServiceReason.StationNotReachable(path,"unreachable"));

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    public void shouldIncludeIfWalking() {
        TramRouteEvaluator evaluator = new TramRouteEvaluator(serviceHeuristics, nodeOperations, 88L);
        BranchState<JourneyState> state = new TestBranchState();

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(node.hasLabel(TransportGraphBuilder.Labels.SERVICE)).andReturn(false);
        EasyMock.expect(node.hasLabel(TransportGraphBuilder.Labels.ROUTE_STATION)).andReturn(false);
        Relationship relationship = createMock(Relationship.class);
        EasyMock.expect(relationship.isType(WALKS_TO)).andReturn(true);
        EasyMock.expect(path.lastRelationship()).andReturn(relationship);

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, result);
        verifyAll();
    }

    @Test
    public void shouldExcludeIfTakingTooLong() {
        TramRouteEvaluator evaluator = new TramRouteEvaluator(serviceHeuristics, nodeOperations, 88L);
        BranchState<JourneyState> state = new TestBranchState();

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(node.hasLabel(TransportGraphBuilder.Labels.SERVICE)).andReturn(false);
        EasyMock.expect(node.hasLabel(TransportGraphBuilder.Labels.ROUTE_STATION)).andReturn(false);
        Relationship relationship = createMock(Relationship.class);
        EasyMock.expect(relationship.isType(WALKS_TO)).andReturn(false);
        EasyMock.expect(path.lastRelationship()).andReturn(relationship);

        LocalTime time = LocalTime.of(8, 15);
        EasyMock.expect(serviceHeuristics.journeyTookTooLong(TramTime.of(time))).andReturn(true);

        NotStartedState traversalState = new NotStartedState(nodeOperations, 88L, "destinationStationId");
        state.setState(new JourneyState(time, traversalState));

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.INCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    public void shouldExcludeIfServiceNotRunning() {
        TramRouteEvaluator evaluator = new TramRouteEvaluator(serviceHeuristics, nodeOperations, 88L);
        BranchState<JourneyState> state = new TestBranchState();

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(node.hasLabel(TransportGraphBuilder.Labels.SERVICE)).andReturn(true);
        EasyMock.expect(node.hasLabel(TransportGraphBuilder.Labels.ROUTE_STATION)).andReturn(false);
        Relationship relationship = createMock(Relationship.class);
        EasyMock.expect(relationship.isType(WALKS_TO)).andReturn(false);
        EasyMock.expect(path.lastRelationship()).andReturn(relationship);

        LocalTime time = LocalTime.of(8, 15);
        NotStartedState traversalState = new NotStartedState(nodeOperations, 88L, "destinationStationId");

        state.setState(new JourneyState(time,traversalState));
        EasyMock.expect(serviceHeuristics.checkServiceDate(node, path)).andReturn(ServiceReason.IsValid(path,"ok"));
        EasyMock.expect(serviceHeuristics.journeyTookTooLong(TramTime.of(time))).andReturn(false);
        EasyMock.expect(serviceHeuristics.checkServiceTime(path, node, time)).
                andReturn(ServiceReason.DoesNotOperateOnTime(time, "diagnostics", path));

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    public void shouldExcludeIfServiceNotCorrectHour() {
        TramRouteEvaluator evaluator = new TramRouteEvaluator(serviceHeuristics, nodeOperations, 88L);
        BranchState<JourneyState> state = new TestBranchState();

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(node.hasLabel(TransportGraphBuilder.Labels.SERVICE)).andReturn(false);
        EasyMock.expect(node.hasLabel(TransportGraphBuilder.Labels.ROUTE_STATION)).andReturn(false);
        EasyMock.expect(node.hasLabel(TransportGraphBuilder.Labels.HOUR)).andReturn(true);
        EasyMock.expect(node.getProperty("hour")).andReturn(8);

        Relationship relationship = createMock(Relationship.class);
        EasyMock.expect(relationship.isType(WALKS_TO)).andReturn(false);
        EasyMock.expect(path.lastRelationship()).andReturn(relationship);

        NotStartedState traversalState = new NotStartedState(nodeOperations, 88L, "destinationStationId");
        LocalTime time = LocalTime.of(8, 15);

        state.setState(new JourneyState(time, traversalState));
        EasyMock.expect(serviceHeuristics.journeyTookTooLong(TramTime.of(time))).andReturn(false);
        EasyMock.expect(serviceHeuristics.interestedInHour(path, 8, time)).
                andReturn(ServiceReason.DoesNotOperateOnTime(time, "diag", path));

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    public void shouldExcludeIfServiceNotCorrectMinute() {
        TramRouteEvaluator evaluator = new TramRouteEvaluator(serviceHeuristics, nodeOperations, 88L);
        BranchState<JourneyState> state = new TestBranchState();

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(node.hasLabel(TransportGraphBuilder.Labels.SERVICE)).andReturn(false);
        EasyMock.expect(node.hasLabel(TransportGraphBuilder.Labels.ROUTE_STATION)).andReturn(false);
        EasyMock.expect(node.hasLabel(TransportGraphBuilder.Labels.HOUR)).andReturn(false);
        EasyMock.expect(node.hasLabel(TransportGraphBuilder.Labels.MINUTE)).andReturn(true);

        Relationship relationship = createMock(Relationship.class);
        EasyMock.expect(relationship.isType(WALKS_TO)).andReturn(false);
        EasyMock.expect(path.lastRelationship()).andReturn(relationship);

        NotStartedState traversalState = new NotStartedState(nodeOperations, 88L, "destinationStationId");
        LocalTime time = LocalTime.of(8, 15);
        state.setState(new JourneyState(time, traversalState));

        EasyMock.expect(serviceHeuristics.journeyTookTooLong(TramTime.of(time))).andReturn(false);
        EasyMock.expect(serviceHeuristics.checkTime(path, node, time)).
                andReturn(ServiceReason.DoesNotOperateOnTime(time, "diag", path));

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    public void shouldIncludeIfMatchesNoRules() {
        TramRouteEvaluator evaluator = new TramRouteEvaluator(serviceHeuristics, nodeOperations, 88L);
        BranchState<JourneyState> state = new TestBranchState();

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(node.hasLabel(TransportGraphBuilder.Labels.SERVICE)).andReturn(false);
        EasyMock.expect(node.hasLabel(TransportGraphBuilder.Labels.ROUTE_STATION)).andReturn(false);
        EasyMock.expect(node.hasLabel(TransportGraphBuilder.Labels.HOUR)).andReturn(false);
        EasyMock.expect(node.hasLabel(TransportGraphBuilder.Labels.MINUTE)).andReturn(false);

        Relationship relationship = createMock(Relationship.class);
        EasyMock.expect(relationship.isType(WALKS_TO)).andReturn(false);
        EasyMock.expect(path.lastRelationship()).andReturn(relationship);

        LocalTime time = LocalTime.of(8, 15);
        NotStartedState traversalState = new NotStartedState(nodeOperations, 88L, "destinationStationId");
        state.setState(new JourneyState(time, traversalState));

        EasyMock.expect(serviceHeuristics.journeyTookTooLong(TramTime.of(time))).andReturn(false);

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, result);
        verifyAll();
    }

    private class TestBranchState implements BranchState<JourneyState> {
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
