package com.tramchester.unit.graph;


import com.tramchester.TestConfig;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.*;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.*;

import static com.tramchester.graph.TransportRelationshipTypes.BOARD;
import static com.tramchester.graph.TransportRelationshipTypes.INTERCHANGE_BOARD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class LazyTimeBasedPathExpanderTest extends EasyMockSupport {

    private static final Logger logger = LoggerFactory.getLogger(LazyTimeBasedPathExpanderTest.class);


    private Relationship departs;
    private Relationship boards;
    private Relationship goesToA;
    private Relationship goesToB;
    private RelationshipFactory mockRelationshipFactory;
    private ServiceHeuristics serviceHeuristics;
    private Node endNode;
    private BranchState<Double> branchState;

    @Rule
    public TestName testName = new TestName();
    private Path path;

    @Before
    public void beforeEachTestRuns() {

        branchState = null;

        // for logging
        MDC.put("test", testName.getMethodName());

        departs = createMock(Relationship.class);
        EasyMock.expect(departs.isType(TransportRelationshipTypes.TRAM_GOES_TO)).andStubReturn(false);
        EasyMock.expect(departs.isType(TransportRelationshipTypes.ON_ROUTE)).andStubReturn(false);

        boards = createMock(Relationship.class);
        EasyMock.expect(boards.isType(TransportRelationshipTypes.TRAM_GOES_TO)).andStubReturn(false);
        EasyMock.expect(boards.isType(TransportRelationshipTypes.ON_ROUTE)).andStubReturn(false);

        goesToA = createMock(Relationship.class);
        EasyMock.expect(goesToA.isType(TransportRelationshipTypes.TRAM_GOES_TO)).andStubReturn(true);
        EasyMock.expect(goesToA.isType(TransportRelationshipTypes.ON_ROUTE)).andStubReturn(false);

        goesToB = createMock(Relationship.class);
        EasyMock.expect(goesToB.isType(TransportRelationshipTypes.TRAM_GOES_TO)).andStubReturn(true);
        EasyMock.expect(goesToB.isType(TransportRelationshipTypes.ON_ROUTE)).andStubReturn(false);

        mockRelationshipFactory = createMock(RelationshipFactory.class);
        serviceHeuristics = createMock(ServiceHeuristics.class);
        endNode = createMock(Node.class);
        path = createMock(Path.class);

    }

    @Test
    public void cannotRunTheseTestsWithDebuggingEnabled() {
        assertFalse(logger.isDebugEnabled());
    }

    @Test
    public void shouldExpandPathsCorrectlyForInitialPath() {

        List<Relationship> outgoingRelationships = createRelationships(boards, departs);

        EasyMock.expect(path.endNode()).andStubReturn(endNode);
        EasyMock.expect(endNode.getRelationships(Direction.OUTGOING)).andReturn(outgoingRelationships);

        Relationship lastRelationship = createMock(Relationship.class);
        EasyMock.expect(path.lastRelationship()).andReturn(lastRelationship);

        EasyMock.expect(lastRelationship.isType(BOARD)).andStubReturn(false);
        EasyMock.expect(lastRelationship.isType(INTERCHANGE_BOARD)).andStubReturn(false);

        replayAll();
        PathExpander<Double> pathExpander = new LazyTimeBasedPathExpander(mockRelationshipFactory,
                serviceHeuristics);

        Iterable<Relationship> results = pathExpander.expand(path, branchState);

        int number = countResults(results);
        verifyAll();

        assertEquals(2, number);
    }

    @Test
    public void shouldExpandPathsCorrectlyForMatchingPath() throws TramchesterException {
        List<Relationship> outgoingRelationships = createRelationships(boards, goesToA, departs);

        EasyMock.expect(path.endNode()).andStubReturn(endNode);
        EasyMock.expect(endNode.getRelationships(Direction.OUTGOING)).andReturn(outgoingRelationships);

        Relationship lastRelationship = createMock(Relationship.class);
        EasyMock.expect(path.lastRelationship()).andReturn(lastRelationship);
        EasyMock.expect(path.lastRelationship()).andReturn(lastRelationship);

        EasyMock.expect(lastRelationship.isType(BOARD)).andStubReturn(false);
        EasyMock.expect(lastRelationship.isType(INTERCHANGE_BOARD)).andStubReturn(false);

        TransportRelationship incoming = createMock(TransportRelationship.class);
        EasyMock.expect(mockRelationshipFactory.getRelationship(lastRelationship)).andReturn(incoming);

        GoesToRelationship goesTo = createMock(GoesToRelationship.class);
        EasyMock.expect(mockRelationshipFactory.getRelationship(goesToA)).andReturn(goesTo);

        EasyMock.expect(serviceHeuristics.checkServiceHeuristics(incoming, goesTo, path)).
                andReturn(ServiceReason.IsValid(path));

        replayAll();
        PathExpander<Double> pathExpander = new LazyTimeBasedPathExpander(mockRelationshipFactory,
                serviceHeuristics);
        Iterable<Relationship> results = pathExpander.expand(path, branchState);
        int actual = countResults(results);

        verifyAll();

        assertEquals(3, actual);
    }

    @Test
    public void shouldExpandPathsCorrectlyForNonMatchingPath() throws TramchesterException {
        List<Relationship> outgoingRelationships = createRelationships(boards, goesToA, departs);

        EasyMock.expect(path.endNode()).andStubReturn(endNode);
        EasyMock.expect(endNode.getRelationships(Direction.OUTGOING)).andReturn(outgoingRelationships);
        //EasyMock.expect(path.length()).andStubReturn(1);

        Relationship lastRelationship = createMock(Relationship.class);
        EasyMock.expect(path.lastRelationship()).andReturn(lastRelationship);
        EasyMock.expect(path.lastRelationship()).andReturn(lastRelationship);

        EasyMock.expect(lastRelationship.isType(BOARD)).andStubReturn(false);
        EasyMock.expect(lastRelationship.isType(INTERCHANGE_BOARD)).andStubReturn(false);

        TransportRelationship incoming = createMock(TransportRelationship.class);

        EasyMock.expect(mockRelationshipFactory.getRelationship(lastRelationship)).andReturn(incoming);
        GoesToRelationship goesTo = createMock(GoesToRelationship.class);
        EasyMock.expect(mockRelationshipFactory.getRelationship(goesToA)).andReturn(goesTo);

        EasyMock.expect(serviceHeuristics.checkServiceHeuristics(incoming, goesTo, path)).
                andReturn(ServiceReason.DoesNotRunOnQueryDate("diag", path));

        replayAll();
        PathExpander<Double> pathExpander = new LazyTimeBasedPathExpander(mockRelationshipFactory,
                serviceHeuristics);
        Iterable<Relationship> results = pathExpander.expand(path, branchState);
        int actual = countResults(results);

        verifyAll();

        assertEquals(2, actual);
    }

    @Test
    public void shouldExpandPathsCorrectlyForMixedMatchingPath() throws TramchesterException {
        List<Relationship> outgoingRelationships = createRelationships(boards, goesToA, goesToB, departs);

        EasyMock.expect(path.endNode()).andStubReturn(endNode);
        EasyMock.expect(endNode.getRelationships(Direction.OUTGOING)).andReturn(outgoingRelationships);

        EasyMock.expect(path.length()).andStubReturn(1);

        Relationship lastRelationship = createMock(Relationship.class);
        EasyMock.expect(path.lastRelationship()).andStubReturn(lastRelationship);
        EasyMock.expect(lastRelationship.isType(BOARD)).andStubReturn(false);
        EasyMock.expect(lastRelationship.isType(INTERCHANGE_BOARD)).andStubReturn(false);

        TransportRelationship incoming = createMock(TransportRelationship.class);

        EasyMock.expect(mockRelationshipFactory.getRelationship(lastRelationship)).andStubReturn(incoming);
        GoesToRelationship goesTo1 = createMock(GoesToRelationship.class);
        EasyMock.expect(mockRelationshipFactory.getRelationship(goesToA)).andReturn(goesTo1);
        GoesToRelationship goesTo2 = createMock(GoesToRelationship.class);
        EasyMock.expect(mockRelationshipFactory.getRelationship(goesToB)).andReturn(goesTo2);

        EasyMock.expect(serviceHeuristics.checkServiceHeuristics(incoming, goesTo1, path)).
                andReturn(ServiceReason.DoesNotRunOnQueryDate("diag", path));

        EasyMock.expect(serviceHeuristics.checkServiceHeuristics(incoming, goesTo2, path)).
                andReturn(ServiceReason.IsValid(path));

        replayAll();
        PathExpander<Double> pathExpander = new LazyTimeBasedPathExpander(mockRelationshipFactory,
                serviceHeuristics);
        Iterable<Relationship> results = pathExpander.expand(path, branchState);
        int actual = countResults(results);
        verifyAll();

        assertEquals(3, actual);
    }

    private List<Relationship> createRelationships(Relationship... relats) {
        List<Relationship> relationships = new ArrayList<>();
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

    private class NoEdgePerTripTestConfig extends TestConfig {
        @Override
        public java.nio.file.Path getDataFolder() {
            return null;
        }

        @Override
        public boolean getEdgePerTrip() {
            return false;
        }

    }
}
