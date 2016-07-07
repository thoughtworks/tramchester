package com.tramchester.graph;


import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.Relationships.GoesToRelationship;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TransportRelationship;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.BranchState;
import org.slf4j.MDC;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class TimeBasedPathExpanderTest extends EasyMockSupport {

    private Relationship departs;
    private Relationship boards;
    private Relationship goesToA;
    private Relationship goesToB;
    private NodeFactory mockNodeFactory;
    private RelationshipFactory mockRelationshipFactory;
    private ServiceHeuristics serviceHeuristics;
    private Node endNode;
    private BranchState<GraphBranchState> branchState;

    @Rule
    public TestName testName = new TestName();
    private Path path;

    @Before
    public void beforeEachTestRuns() {
        // for logging
        MDC.put("test", testName.getMethodName());

        new NodeFactory();

        departs = createMock(Relationship.class);
        EasyMock.expect(departs.getType()).andStubReturn(TransportRelationshipTypes.DEPART);

        boards = createMock(Relationship.class);
        EasyMock.expect(boards.getType()).andStubReturn(TransportRelationshipTypes.BOARD);

        goesToA = createMock(Relationship.class);
        EasyMock.expect(goesToA.getType()).andStubReturn(TransportRelationshipTypes.TRAM_GOES_TO);

        goesToB = createMock(Relationship.class);
        EasyMock.expect(goesToB.getType()).andStubReturn(TransportRelationshipTypes.TRAM_GOES_TO);

        mockNodeFactory = createMock(NodeFactory.class);
        mockRelationshipFactory = createMock(RelationshipFactory.class);
        serviceHeuristics = createMock(ServiceHeuristics.class);
        endNode = createMock(Node.class);
        path = createMock(Path.class);
        GraphBranchState state = createMock(GraphBranchState.class);
        branchState = createGraphBranchState(state);
    }

    @Test
    public void shouldExpandPathsCorrectlyForInitialPath() throws TramchesterException {

        Set<Relationship> outgoingRelationships = createRelationships(boards, goesToA, departs);

        EasyMock.expect(path.length()).andReturn(0);
        EasyMock.expect(path.endNode()).andReturn(endNode);
        EasyMock.expect(endNode.getRelationships(Direction.OUTGOING)).andReturn(outgoingRelationships);

        replayAll();
        PathExpander<GraphBranchState> pathExpander = new TimeBasedPathExpander(mockRelationshipFactory,
                mockNodeFactory, serviceHeuristics);
        Iterable<Relationship> results = pathExpander.expand(path, branchState);
        verifyAll();

        assertEquals(3, countResults(results));
    }

    @Test
    public void shouldExpandPathsCorrectlyForMatchingPath() throws TramchesterException {
        Set<Relationship> outgoingRelationships = createRelationships(boards, goesToA, departs);

        EasyMock.expect(path.length()).andReturn(1);
        EasyMock.expect(path.endNode()).andReturn(endNode);
        EasyMock.expect(endNode.getRelationships(Direction.OUTGOING)).andReturn(outgoingRelationships);
        Relationship lastRelationship = createMock(Relationship.class);
        EasyMock.expect(path.lastRelationship()).andReturn(lastRelationship);
        TransportRelationship incoming = createMock(TransportRelationship.class);
        EasyMock.expect(mockRelationshipFactory.getRelationship(lastRelationship)).andReturn(incoming);
        GoesToRelationship goesTo = createMock(GoesToRelationship.class);
        EasyMock.expect(mockRelationshipFactory.getRelationship(goesToA)).andReturn(goesTo);

        EasyMock.expect(serviceHeuristics.checkServiceHeuristics(branchState, incoming, goesTo, path)).
                andReturn(ServiceReason.IsValid);

        replayAll();
        PathExpander<GraphBranchState> pathExpander = new TimeBasedPathExpander(mockRelationshipFactory,
                mockNodeFactory, serviceHeuristics);
        Iterable<Relationship> results = pathExpander.expand(path, branchState);
        verifyAll();

        assertEquals(3, countResults(results));
    }

    @Test
    public void shouldExpandPathsCorrectlyForNonMatchingPath() throws TramchesterException {
        Set<Relationship> outgoingRelationships = createRelationships(boards, goesToA, departs);

        EasyMock.expect(path.length()).andReturn(1);
        EasyMock.expect(path.endNode()).andReturn(endNode);
        EasyMock.expect(endNode.getRelationships(Direction.OUTGOING)).andReturn(outgoingRelationships);
        Relationship lastRelationship = createMock(Relationship.class);
        EasyMock.expect(path.lastRelationship()).andReturn(lastRelationship);
        TransportRelationship incoming = createMock(TransportRelationship.class);
        EasyMock.expect(mockRelationshipFactory.getRelationship(lastRelationship)).andReturn(incoming);
        GoesToRelationship goesTo = createMock(GoesToRelationship.class);
        EasyMock.expect(mockRelationshipFactory.getRelationship(goesToA)).andReturn(goesTo);

        EasyMock.expect(serviceHeuristics.checkServiceHeuristics(branchState, incoming, goesTo, path)).
                andReturn(ServiceReason.DoesNotRunOnQueryDate);

        TramNode tramNode = createMock(TramNode.class);
        EasyMock.expect(mockNodeFactory.getNode(endNode)).andReturn(tramNode);

        replayAll();
        PathExpander<GraphBranchState> pathExpander = new TimeBasedPathExpander(mockRelationshipFactory,
                mockNodeFactory, serviceHeuristics);
        Iterable<Relationship> results = pathExpander.expand(path, branchState);
        verifyAll();

        assertEquals(2, countResults(results));
    }

    @Test
    public void shouldExpandPathsCorrectlyForMixedMatchingPath() throws TramchesterException {
        Set<Relationship> outgoingRelationships = createRelationships(boards, goesToA, goesToB, departs);

        EasyMock.expect(path.length()).andReturn(1);
        EasyMock.expect(path.endNode()).andReturn(endNode);
        EasyMock.expect(endNode.getRelationships(Direction.OUTGOING)).andReturn(outgoingRelationships);
        Relationship lastRelationship = createMock(Relationship.class);
        EasyMock.expect(path.lastRelationship()).andReturn(lastRelationship);
        TransportRelationship incoming = createMock(TransportRelationship.class);
        EasyMock.expect(mockRelationshipFactory.getRelationship(lastRelationship)).andReturn(incoming);
        GoesToRelationship goesTo1 = createMock(GoesToRelationship.class);
        EasyMock.expect(mockRelationshipFactory.getRelationship(goesToA)).andReturn(goesTo1);
        GoesToRelationship goesTo2 = createMock(GoesToRelationship.class);
        EasyMock.expect(mockRelationshipFactory.getRelationship(goesToB)).andReturn(goesTo2);

        EasyMock.expect(serviceHeuristics.checkServiceHeuristics(branchState, incoming, goesTo1, path)).
                andReturn(ServiceReason.DoesNotRunOnQueryDate);

        EasyMock.expect(serviceHeuristics.checkServiceHeuristics(branchState, incoming, goesTo2, path)).
                andReturn(ServiceReason.IsValid);

        replayAll();
        PathExpander<GraphBranchState> pathExpander = new TimeBasedPathExpander(mockRelationshipFactory,
                mockNodeFactory, serviceHeuristics);
        Iterable<Relationship> results = pathExpander.expand(path, branchState);
        verifyAll();

        assertEquals(3, countResults(results));
    }

    private Set<Relationship> createRelationships(Relationship... relats) {
        Set<Relationship> relationships = new HashSet<>();
        Collections.addAll(relationships, relats);
        return relationships;
    }

    private int countResults(Iterable<Relationship> results) {
        int count = 0;
        for(Relationship ignored : results) {
            count++;
        }
        return count;
    }

    private BranchState<GraphBranchState> createGraphBranchState(final GraphBranchState state) {
        return new BranchState<GraphBranchState>() {
                @Override
                public GraphBranchState getState() {
                    return state;
                }

                @Override
                public void setState(GraphBranchState graphBranchState) {

                }
            };
    }

}
