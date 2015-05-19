package com.tramchester.graph;


import com.tramchester.domain.DaysOfWeek;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.Relationships.*;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphalgo.CommonEvaluators;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.tramchester.graph.GraphStaticKeys.COST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestTimeBasedPathExpander extends EasyMockSupport {

    public static final int MAX_WAIT_MINUTES = 30;
    private int[] times = new int[] { 600, 700, 800, 900, 1000 };
    private Relationship departs;
    private Relationship boards;
    private Relationship interchange;
    private Relationship goesToA;
    private Relationship goesToB;
    private boolean[] days = new boolean[] {};
    private RelationshipFactory relationshipFactory;
    private NodeFactory nodeFactory;

    TramRelationship departsRelationship = new TramRelationship() {
        @Override
        public boolean isGoesTo() {
            return false;
        }

        @Override
        public boolean isBoarding() {
            return false;
        }

        @Override
        public boolean isDepartTram() {
            return true;
        }

        @Override
        public boolean isInterchange() {
            return false;
        }

        @Override
        public int getCost() {
            return 2;
        }

        @Override
        public String getId() {
            return "ID";
        }
    };
    private int goesToACost;
    private int goesToBCost;

    @Before
    public void beforeEachTestRuns() {
        relationshipFactory = new RelationshipFactory();
        nodeFactory = new NodeFactory();
        // departing
        departs = createMock(Relationship.class);
        EasyMock.expect(departs.getType()).andStubReturn(TransportRelationshipTypes.DEPART);
        EasyMock.expect(departs.getProperty(GraphStaticKeys.COST)).andStubReturn(0);
        // boarding
        boards = createMock(Relationship.class);
        EasyMock.expect(boards.getType()).andStubReturn(TransportRelationshipTypes.BOARD);
        EasyMock.expect(boards.getProperty(GraphStaticKeys.COST)).andStubReturn(5);
        // interchanges
        interchange = createMock(Relationship.class);
        EasyMock.expect(interchange.getType()).andStubReturn(TransportRelationshipTypes.INTERCHANGE);
        EasyMock.expect(interchange.getProperty(GraphStaticKeys.COST)).andStubReturn(3);
        // outgoing A
        goesToA = createMock(Relationship.class);
        goesToACost = 5;
        EasyMock.expect(goesToA.getType()).andStubReturn(TransportRelationshipTypes.GOES_TO);
        EasyMock.expect(goesToA.getProperty(GraphStaticKeys.COST)).andStubReturn(goesToACost);
        // outgoing B
        goesToB = createMock(Relationship.class);
        goesToBCost = 10;
        EasyMock.expect(goesToB.getType()).andStubReturn(TransportRelationshipTypes.GOES_TO);
        EasyMock.expect(goesToB.getProperty(GraphStaticKeys.COST)).andStubReturn(goesToBCost);
    }

    @Test
    public void shouldHandleTimesWith30MinWait() {
        TimeBasedPathExpander expander = new TimeBasedPathExpander(
                CommonEvaluators.doubleCostEvaluator(COST), MAX_WAIT_MINUTES, relationshipFactory, nodeFactory);

        assertFalse(expander.operatesOnTime(times, 400));
        assertFalse(expander.operatesOnTime(times, 550));
        assertTrue(expander.operatesOnTime(times, 580));
        assertTrue(expander.operatesOnTime(times, 600));
        assertFalse(expander.operatesOnTime(times, 620));
        assertFalse(expander.operatesOnTime(times, 630));
        assertFalse(expander.operatesOnTime(times, 650));
        assertTrue(expander.operatesOnTime(times, 680));
        assertFalse(expander.operatesOnTime(times, 1001));
    }

    @Test
    public void shouldHandleTimesWith15MinWait() {
        TimeBasedPathExpander expander = new TimeBasedPathExpander(
                CommonEvaluators.doubleCostEvaluator(COST), 15, relationshipFactory, nodeFactory);

        assertFalse(expander.operatesOnTime(times, 400));
        assertFalse(expander.operatesOnTime(times, 550));
        assertFalse(expander.operatesOnTime(times, 580));
        assertTrue(expander.operatesOnTime(times, 600));
        assertFalse(expander.operatesOnTime(times, 620));
        assertFalse(expander.operatesOnTime(times, 630));
        assertFalse(expander.operatesOnTime(times, 650));
        assertFalse(expander.operatesOnTime(times, 680));
        assertTrue(expander.operatesOnTime(times, 590));
        assertFalse(expander.operatesOnTime(times, 1001));
    }

    @Test
    public void shouldHandleTimesOneTime() {
        TimeBasedPathExpander expander = new TimeBasedPathExpander(
                CommonEvaluators.doubleCostEvaluator(COST), MAX_WAIT_MINUTES, relationshipFactory, nodeFactory);

        int[] time = new int[] { 450 };
        assertFalse(expander.operatesOnTime(time, 400));
        assertTrue(expander.operatesOnTime(time, 420));
        assertFalse(expander.operatesOnTime(time, 451));
    }

    @Test
    public void shouldCheckIfChangeOfServiceWithDepartAndThenBoard() {

        replayAll();

        TramRelationship board = new BoardRelationship(boards);
        TramRelationship depart = new DepartRelationship(departs);
        TramRelationship change = new InterchangeRelationship(interchange);
        GoesToRelationship outA = new GoesToRelationship("0042", 10, days, times, "id1");
        GoesToRelationship outB = new GoesToRelationship("0048", 5, days, times, "id2");
        TimeBasedPathExpander expander = new TimeBasedPathExpander(
                CommonEvaluators.doubleCostEvaluator(COST), MAX_WAIT_MINUTES, relationshipFactory, nodeFactory);

        assertTrue(expander.noInFlightChangeOfService(board, outA));
        assertTrue(expander.noInFlightChangeOfService(depart, outA));
        assertTrue(expander.noInFlightChangeOfService(change, outA));
        assertTrue(expander.noInFlightChangeOfService(outA, outA));
        assertFalse(expander.noInFlightChangeOfService(outB, outA));
        verifyAll();
    }

    @Test
    public void shouldCheckIfTramRunsOnADay() {
        TimeBasedPathExpander expander = new TimeBasedPathExpander(
                CommonEvaluators.doubleCostEvaluator(COST), MAX_WAIT_MINUTES, relationshipFactory, nodeFactory);

        checkDay(expander, true,false,false,false,false,false,false,DaysOfWeek.Monday);
        checkDay(expander, false,true,false,false,false,false,false,DaysOfWeek.Tuesday);
        checkDay(expander, false,false,true,false,false,false,false,DaysOfWeek.Wednesday);
        checkDay(expander, false,false,false,true,false,false,false,DaysOfWeek.Thursday);
        checkDay(expander, false,false,false,false,true,false,false,DaysOfWeek.Friday);
        checkDay(expander, false,false,false,false,false,true,false,DaysOfWeek.Saturday);
        checkDay(expander, false, false, false, false, false, false, true, DaysOfWeek.Sunday);
    }

    private void checkDay(TimeBasedPathExpander expander, boolean d1, boolean d2, boolean d3, boolean d4, boolean d5, boolean d6, boolean d7, DaysOfWeek day) {

        boolean[] days = new boolean[]{d1, d2, d3, d4, d5, d6, d7};
        assertTrue(expander.operatesOnDay(days, day));
    }

    @Test
    public void shouldExpandPathsCorrectlyForInitialPath() {

        RelationshipFactory mockRelationshipFactory = createMock(RelationshipFactory.class);
        NodeFactory mockNodeFactory = createMock(NodeFactory.class);
        Set<Relationship> outgoingRelationships = createRelationships(departs, boards, goesToA);
        //
        PathExpander<GraphBranchState> pathExpander = new TimeBasedPathExpander(RouteCalculator.COST_EVALUATOR,
                RouteCalculator.MAX_WAIT_TIME_MINS, mockRelationshipFactory, mockNodeFactory);
        Node endNode = createMock(Node.class);
        TramNode tramNode = createMock(TramNode.class);
        //
        Path path = setNodeExpectations(mockNodeFactory, outgoingRelationships, endNode, tramNode);

        EasyMock.expect(path.length()).andReturn(0);

        GraphBranchState state = new GraphBranchState(580, DaysOfWeek.Monday);
        BranchState<GraphBranchState> branchState = createGraphBranchState(state);

        replayAll();
        Iterable<Relationship> results = pathExpander.expand(path, branchState);
        verifyAll();

        assertEquals(3, countResults(results));
    }

    @Test
    public void shouldExpandPathsCorrectlyForPathWithSimpleOutgoing() {
        int maxWait = RouteCalculator.MAX_WAIT_TIME_MINS;
        int journeyStartTime = 580;

        int results = countExpandedRelationships(maxWait, journeyStartTime, DaysOfWeek.Monday, "0042", "0042",
                times, goesToB);
        assertEquals(2, results);
    }

    @Test
    public void shouldExpandPathsCorrectlyForPathWithTotalDurationOverWaitTime() {
        int maxWait = RouteCalculator.MAX_WAIT_TIME_MINS;
        int journeyStartTime = 580;
        int inboundDuration = 2 * maxWait;

        Relationship longDurationInbound = createMock(Relationship.class);
        EasyMock.expect(longDurationInbound.getType()).andStubReturn(TransportRelationshipTypes.GOES_TO);
        EasyMock.expect(longDurationInbound.getProperty(GraphStaticKeys.COST)).andStubReturn(
                inboundDuration);

        int[] outboundTimes = new int[] { journeyStartTime+inboundDuration+1 };
        int results = countExpandedRelationships(maxWait, journeyStartTime, DaysOfWeek.Monday, "0042", "0042",
                outboundTimes, longDurationInbound);
        assertEquals(2, results);
    }

    @Test
    public void shouldExpandPathsCorrectlyForPathWithSimpleOutgoingTooEarly() {
        int maxWait = RouteCalculator.MAX_WAIT_TIME_MINS;

        int journeyStartTime = 100;

        int results = countExpandedRelationships(maxWait, journeyStartTime, DaysOfWeek.Monday, "0042", "0042",
                times, goesToB);
        assertEquals(1, results);
    }

    @Test
    public void shouldExpandPathsCorrectlyForPathWithSimpleOutgoingTooLate() {
        int maxWait = RouteCalculator.MAX_WAIT_TIME_MINS;
        int journeyStartTime = 1000;

        int results = countExpandedRelationships(maxWait, journeyStartTime, DaysOfWeek.Monday, "0042", "0042",
                times, goesToB);
        assertEquals(1, results);
    }

    @Test
    public void shouldExpandPathsCorrectlyForPathWithSimpleWaitTooLong() {
        int maxWait = RouteCalculator.MAX_WAIT_TIME_MINS;
        int journeyStartTime = 561;

        int results = countExpandedRelationships(maxWait, journeyStartTime, DaysOfWeek.Monday, "0042", "0042",
                times, goesToB);
        assertEquals(1, results);
    }

    @Test
    public void shouldExpandPathsCorrectlyForPathWithLongIncomingDuration() {
        int maxWait = RouteCalculator.MAX_WAIT_TIME_MINS;
        int journeyStartTime = 561;

        int results = countExpandedRelationships(maxWait, journeyStartTime, DaysOfWeek.Monday, "0042", "0042",
                times, goesToB);
        assertEquals(1, results);
    }

    @Test
    public void shouldExpandPathsCorrectlyForPathWithDifferingServiceId() {
        int maxWait = RouteCalculator.MAX_WAIT_TIME_MINS;
        int journeyStartTime = 580;

        int results = countExpandedRelationships(maxWait, journeyStartTime, DaysOfWeek.Monday, "0042", "00XX",
                times, goesToB);
        assertEquals(1, results);
    }

    @Test
    public void shouldExpandPathsCorrectlyForPathWithSimpleOutgoingWrongDay() {
        int maxWait = RouteCalculator.MAX_WAIT_TIME_MINS;
        int journeyStartTime = 580;

        int results = countExpandedRelationships(maxWait, journeyStartTime, DaysOfWeek.Sunday, "0042", "0042",
                times, goesToB);
        assertEquals(1, results);
    }

    private int countExpandedRelationships(int maxWait, int journeyStartTime, DaysOfWeek day,
                                           String inboundServiceId, String outboundServiceId, int[] outgoingTimes,
                                           Relationship incomingTram) {
        RelationshipFactory mockRelationshipFactory = createMock(RelationshipFactory.class);
        NodeFactory mockNodeFactory = createMock(NodeFactory.class);

        PathExpander<GraphBranchState> pathExpander = new TimeBasedPathExpander(RouteCalculator.COST_EVALUATOR,
                maxWait , mockRelationshipFactory, mockNodeFactory);

        Node endNode = createMock(Node.class);
        TramNode tramNode = createMock(TramNode.class);

        Path path = setNodeExpectations(mockNodeFactory, createRelationships(goesToA, departs), endNode, tramNode);
        createInboundExpectations(mockRelationshipFactory, path, inboundServiceId, incomingTram);
        createOutgoingExpectations(mockRelationshipFactory, goesToACost, outboundServiceId, outgoingTimes, goesToA);

        GraphBranchState state = new GraphBranchState(journeyStartTime, day);
        BranchState<GraphBranchState> branchState = createGraphBranchState(state);

        replayAll();
        Iterable<Relationship> results = pathExpander.expand(path, branchState);
        verifyAll();

        return countResults(results);
    }

    private void createInboundExpectations(RelationshipFactory mockRelationshipFactory, Path path,
                                           String incomingService, Relationship incomingTram) {
        EasyMock.expect(path.length()).andReturn(1);
        EasyMock.expect(path.lastRelationship()).andReturn(incomingTram);
        TramRelationship gotoRelationship = new GoesToRelationship(incomingService, goesToBCost, days, times, "id");
        EasyMock.expect(mockRelationshipFactory.getRelationship(incomingTram)).andReturn(gotoRelationship);
        // neo gets current cost by adding up steps so far
        List<Relationship> pathSoFar = new LinkedList<>();
        pathSoFar.add(incomingTram);
        EasyMock.expect(path.relationships()).andReturn(pathSoFar);
    }

    private void createOutgoingExpectations(RelationshipFactory mockRelationshipFactory, int outgoingStageCost,
                                            String service, int[] outgoingTimes, Relationship outgoingTram) {
        EasyMock.expect(mockRelationshipFactory.getRelationship(departs)).andReturn(departsRelationship);
        boolean[] expectedDays = new boolean[] { true, false, false, false, false, false, false };
        GoesToRelationship outgoingRelationship = new GoesToRelationship(service, outgoingStageCost, expectedDays, outgoingTimes, "id");
        EasyMock.expect(mockRelationshipFactory.getRelationship(outgoingTram)).andReturn(outgoingRelationship);
    }

    private Path setNodeExpectations(NodeFactory mockNodeFactory, Set<Relationship> relationships,
                                     Node endNode, TramNode tramNode) {
        Path path = createMock(Path.class);

        EasyMock.expect(path.endNode()).andReturn(endNode);
        EasyMock.expect(mockNodeFactory.getNode(endNode)).andReturn(tramNode);
        EasyMock.expect(tramNode.getRelationships()).andReturn(relationships);
        return path;
    }

    private Set<Relationship> createRelationships(Relationship... relats) {
        Set<Relationship> relationships = new HashSet<>();
        for(Relationship r : relats) {
            relationships.add(r);
        }
        return relationships;
    }

    private int countResults(Iterable<Relationship> results) {
        int count = 0;
        for(Relationship rel : results) {
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
