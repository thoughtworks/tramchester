package com.tramchester.graph;


import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.Relationships.*;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphalgo.CommonEvaluators;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.BranchState;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.tramchester.graph.GraphStaticKeys.COST;
import static org.junit.Assert.*;

public class TimeBasedPathExpanderTest extends EasyMockSupport {

    public static final int MAX_WAIT_MINUTES = 30;
    private int[] times = new int[] { 600, 700, 800, 900, 1000 };
    private Relationship departs;
    private Relationship boards;
    private Relationship interchangeBoards;
    private Relationship interchangeDeparts;
    private Relationship goesToA;
    private Relationship goesToB;
    private boolean[] days = new boolean[] {};
    private RelationshipFactory relationshipFactory;
    private NodeFactory nodeFactory;
    private int goesToACost;
    private int goesToBCost;
    private TramServiceDate startDate;
    private TramServiceDate endDate;
    private TramServiceDate validDate;

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
        interchangeBoards = createMock(Relationship.class);
        EasyMock.expect(interchangeBoards.getType()).andStubReturn(TransportRelationshipTypes.INTERCHANGE_BOARD);
        EasyMock.expect(interchangeBoards.getProperty(GraphStaticKeys.COST)).andStubReturn(3);
        interchangeDeparts = createMock(Relationship.class);
        EasyMock.expect(interchangeDeparts.getType()).andStubReturn(TransportRelationshipTypes.INTERCHANGE_BOARD);
        EasyMock.expect(interchangeDeparts.getProperty(GraphStaticKeys.COST)).andStubReturn(3);
        // outgoing A
        goesToA = createMock(Relationship.class);
        goesToACost = 5;
        EasyMock.expect(goesToA.getType()).andStubReturn(TransportRelationshipTypes.TRAM_GOES_TO);
        EasyMock.expect(goesToA.getProperty(GraphStaticKeys.COST)).andStubReturn(goesToACost);
        // outgoing B
        goesToB = createMock(Relationship.class);
        goesToBCost = 10;
        EasyMock.expect(goesToB.getType()).andStubReturn(TransportRelationshipTypes.TRAM_GOES_TO);
        EasyMock.expect(goesToB.getProperty(GraphStaticKeys.COST)).andStubReturn(goesToBCost);
        // date range for running services
        startDate = new TramServiceDate("20141201");
        endDate = new TramServiceDate("20151130");
        validDate = new TramServiceDate("20150214");
    }

    @Test
    public void shouldHandleTimesWith30MinWait() throws TramchesterException {
        TimeBasedPathExpander expander = new TimeBasedPathExpander(
                CommonEvaluators.doubleCostEvaluator(COST), MAX_WAIT_MINUTES, relationshipFactory, nodeFactory);

        ProvidesElapsedTime providerA = createNoMatchProvider(400);
        ProvidesElapsedTime providerB = createNoMatchProvider(550);
        ProvidesElapsedTime providerC = createMatchProvider(580, 600-TransportGraphBuilder.BOARDING_COST);
        ProvidesElapsedTime providerD = createMatchProvider(600, 600-TransportGraphBuilder.BOARDING_COST);
        ProvidesElapsedTime providerE = createNoMatchProvider(620);
        ProvidesElapsedTime providerF = createNoMatchProvider(630);
        ProvidesElapsedTime providerG = createNoMatchProvider(650);
        ProvidesElapsedTime providerH = createMatchProvider(680, 700-TransportGraphBuilder.BOARDING_COST);
        ProvidesElapsedTime providerI = createNoMatchProvider(1001);

        replayAll();
        assertFalse(expander.operatesOnTime(times, providerA));
        assertFalse(expander.operatesOnTime(times, providerB));
        assertTrue(expander.operatesOnTime(times, providerC));
        assertTrue(expander.operatesOnTime(times, providerD));
        assertFalse(expander.operatesOnTime(times, providerE));
        assertFalse(expander.operatesOnTime(times, providerF));
        assertFalse(expander.operatesOnTime(times, providerG));
        assertTrue(expander.operatesOnTime(times, providerH));
        assertFalse(expander.operatesOnTime(times, providerI));
        verifyAll();
    }

    private ProvidesElapsedTime createMatchProvider(int queryTime, int journeyStart) throws TramchesterException {
        ProvidesElapsedTime provider = createMock(ProvidesElapsedTime.class);
        EasyMock.expect(provider.getElapsedTime()).andStubReturn(queryTime);
        EasyMock.expect(provider.startNotSet()).andReturn(true);
        provider.setJourneyStart(journeyStart);
        EasyMock.expectLastCall();

        return provider;
    }

    private ProvidesElapsedTime createNoMatchProvider(int queryTime) throws TramchesterException {
        ProvidesElapsedTime provider = createMock(ProvidesElapsedTime.class);
        EasyMock.expect(provider.getElapsedTime()).andReturn(queryTime);
        return provider;
    }

    @Test
    public void shouldHandleTimesWith15MinWait() throws TramchesterException {
        TimeBasedPathExpander expander = new TimeBasedPathExpander(
                CommonEvaluators.doubleCostEvaluator(COST), 15, relationshipFactory, nodeFactory);

        ProvidesElapsedTime providerA = createNoMatchProvider(400);
        ProvidesElapsedTime providerB = createNoMatchProvider(550);
        ProvidesElapsedTime providerC = createNoMatchProvider(580);
        ProvidesElapsedTime providerD = createMatchProvider(600, 600-TransportGraphBuilder.BOARDING_COST);
        ProvidesElapsedTime providerE = createNoMatchProvider(620);
        ProvidesElapsedTime providerF = createNoMatchProvider(630);
        ProvidesElapsedTime providerG = createNoMatchProvider(650);
        ProvidesElapsedTime providerH = createNoMatchProvider(680);
        ProvidesElapsedTime providerI = createMatchProvider(590, 600-TransportGraphBuilder.BOARDING_COST);
        ProvidesElapsedTime providerJ = createNoMatchProvider(1001);

        replayAll();
        assertFalse(expander.operatesOnTime(times, providerA));
        assertFalse(expander.operatesOnTime(times, providerB));
        assertFalse(expander.operatesOnTime(times, providerC));
        assertTrue(expander.operatesOnTime(times, providerD));
        assertFalse(expander.operatesOnTime(times, providerE));
        assertFalse(expander.operatesOnTime(times, providerF));
        assertFalse(expander.operatesOnTime(times, providerG));
        assertFalse(expander.operatesOnTime(times, providerH));
        assertTrue(expander.operatesOnTime(times, providerI));
        assertFalse(expander.operatesOnTime(times, providerJ));
        verifyAll();
    }

    @Test
    public void shouldHandleTimesOneTime() throws TramchesterException {
        TimeBasedPathExpander expander = new TimeBasedPathExpander(
                CommonEvaluators.doubleCostEvaluator(COST), MAX_WAIT_MINUTES, relationshipFactory, nodeFactory);

        int[] time = new int[] { 450 };
        ProvidesElapsedTime providerA = createNoMatchProvider(400);
        ProvidesElapsedTime providerB = createMatchProvider(420, 450-TransportGraphBuilder.BOARDING_COST);
        ProvidesElapsedTime providerC = createNoMatchProvider(451);

        replayAll();
        assertFalse(expander.operatesOnTime(time, providerA));
        assertTrue(expander.operatesOnTime(time, providerB));
        assertFalse(expander.operatesOnTime(time, providerC));
        verifyAll();
    }

    @Test
    public void shouldCheckIfChangeOfServiceWithDepartAndThenBoard() {

        replayAll();

        TramRelationship board = new BoardRelationship(boards);
        TramRelationship depart = new DepartRelationship(departs);
        TramRelationship change = new InterchangeDepartsRelationship(interchangeBoards);
        TramGoesToRelationship outA = new TramGoesToRelationship("0042", 10, days, times, "id1", startDate, endDate, "destA",
                TransportMode.Tram);
        TramGoesToRelationship outB = new TramGoesToRelationship("0048", 5, days, times, "id2", startDate, endDate, "destB",
                TransportMode.Tram);
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

    private void checkDay(TimeBasedPathExpander expander, boolean d1, boolean d2, boolean d3, boolean d4,
                          boolean d5, boolean d6, boolean d7, DaysOfWeek day) {

        boolean[] days = new boolean[]{d1, d2, d3, d4, d5, d6, d7};
        assertTrue(expander.operatesOnDayOnWeekday(days, day));
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

        GraphBranchState state = new GraphBranchState(DaysOfWeek.Monday, validDate, 580);
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
                times, goesToB, validDate, true);
        assertEquals(2, results);
    }

    @Test
    public void shouldExpandPathsCorrectlyForPathWithTotalDurationOverWaitTime() {
        int maxWait = RouteCalculator.MAX_WAIT_TIME_MINS;
        int journeyStartTime = 580;
        int inboundDuration = 2 * maxWait;

        Relationship longDurationInbound = createMock(Relationship.class);
        EasyMock.expect(longDurationInbound.getType()).andStubReturn(TransportRelationshipTypes.TRAM_GOES_TO);
        EasyMock.expect(longDurationInbound.getProperty(GraphStaticKeys.COST)).andStubReturn(
                inboundDuration);

        int[] outboundTimes = new int[] { journeyStartTime+inboundDuration+1 };
        int results = countExpandedRelationships(maxWait, journeyStartTime, DaysOfWeek.Monday, "0042", "0042",
                outboundTimes, longDurationInbound, validDate, true);
        assertEquals(2, results);
    }

    @Test
    public void shouldExpandPathsCorrectlyForPathWithSimpleOutgoingTooEarly() {
        int maxWait = RouteCalculator.MAX_WAIT_TIME_MINS;

        int journeyStartTime = 100;

        int results = countExpandedRelationships(maxWait, journeyStartTime, DaysOfWeek.Monday, "0042", "0042",
                times, goesToB, validDate, true);
        assertEquals(1, results);
    }

    @Test
    public void shouldExpandPathsCorrectlyForPathWithSimpleOutgoingTooLate() {
        int maxWait = RouteCalculator.MAX_WAIT_TIME_MINS;
        int journeyStartTime = 1000;

        int results = countExpandedRelationships(maxWait, journeyStartTime, DaysOfWeek.Monday, "0042", "0042",
                times, goesToB, validDate, true);
        assertEquals(1, results);
    }

    @Test
    public void shouldExpandPathsCorrectlyForPathWithSimpleWaitTooLong() {
        int maxWait = RouteCalculator.MAX_WAIT_TIME_MINS;
        int journeyStartTime = 561;

        int results = countExpandedRelationships(maxWait, journeyStartTime, DaysOfWeek.Monday, "0042", "0042",
                times, goesToB, validDate, true);
        assertEquals(1, results);
    }

    @Test
    public void shouldExpandPathsCorrectlyForPathWithLongIncomingDuration() {
        int maxWait = RouteCalculator.MAX_WAIT_TIME_MINS;
        int journeyStartTime = 561;

        int results = countExpandedRelationships(maxWait, journeyStartTime, DaysOfWeek.Monday, "0042", "0042",
                times, goesToB, validDate, true);
        assertEquals(1, results);
    }

    @Test
    public void shouldExpandPathsCorrectlyForPathWithDifferingServiceId() {
        int maxWait = RouteCalculator.MAX_WAIT_TIME_MINS;
        int journeyStartTime = 580;

        int results = countExpandedRelationships(maxWait, journeyStartTime, DaysOfWeek.Monday, "0042", "00XX",
                times, goesToB, validDate, false);
        assertEquals(1, results);
    }

    @Test
    public void shouldExpandPathsCorrectlyWithServiceThatDoesNotRunOnGivenDate() {
        int maxWait = RouteCalculator.MAX_WAIT_TIME_MINS;
        int journeyStartTime = 580;

        TramServiceDate outOfRangeDate = new TramServiceDate("20160630");

        int results = countExpandedRelationships(maxWait, journeyStartTime, DaysOfWeek.Monday, "0042", "0042",
                times, goesToB, outOfRangeDate, false);
        assertEquals(1, results);

    }

    @Test
    public void shouldExpandPathsCorrectlyForPathWithSimpleOutgoingWrongDay() {
        int maxWait = RouteCalculator.MAX_WAIT_TIME_MINS;
        int journeyStartTime = 580;

        int results = countExpandedRelationships(maxWait, journeyStartTime, DaysOfWeek.Sunday, "0042", "0042",
                times, goesToB, validDate, false);
        assertEquals(1, results);
    }

    private int countExpandedRelationships(int maxWait, int queriedTime, DaysOfWeek day,
                                           String inboundServiceId, String outboundServiceId, int[] outgoingTimes,
                                           Relationship incomingTram, TramServiceDate queryDate, boolean pathExpands) {
        RelationshipFactory mockRelationshipFactory = createMock(RelationshipFactory.class);
        NodeFactory mockNodeFactory = createMock(NodeFactory.class);

        PathExpander<GraphBranchState> pathExpander = new TimeBasedPathExpander(RouteCalculator.COST_EVALUATOR,
                maxWait , mockRelationshipFactory, mockNodeFactory);

        Node endNode = createMock(Node.class);
        TramNode tramNode = createMock(TramNode.class);

        Path path = setNodeExpectations(mockNodeFactory, createRelationships(goesToA, departs), endNode, tramNode);
        //EasyMock.expect(tramNode.isRouteStation()).andReturn(false);
        createInboundExpectations(mockRelationshipFactory, path, inboundServiceId, incomingTram, pathExpands);
        createOutgoingExpectations(mockRelationshipFactory, goesToACost, outboundServiceId, outgoingTimes, goesToA);

        GraphBranchState state = new GraphBranchState(day, queryDate, queriedTime);
        BranchState<GraphBranchState> branchState = createGraphBranchState(state);

        replayAll();
        Iterable<Relationship> results = pathExpander.expand(path, branchState);
        verifyAll();

        return countResults(results);
    }

    private void createInboundExpectations(RelationshipFactory mockRelationshipFactory, Path path,
                                           String incomingService, Relationship incomingTram, boolean pathExpands) {
        EasyMock.expect(path.length()).andStubReturn(1);
        EasyMock.expect(path.lastRelationship()).andReturn(incomingTram);
        TramRelationship gotoRelationship = new TramGoesToRelationship(incomingService, goesToBCost, days, times,
                "id", startDate, endDate, "destFrom", TransportMode.Tram);
        EasyMock.expect(mockRelationshipFactory.getRelationship(incomingTram)).andStubReturn(gotoRelationship);
        // neo gets current cost by adding up steps so far
        if (pathExpands) {
            List<Relationship> pathSoFar = new LinkedList<>();
            pathSoFar.add(incomingTram);
            EasyMock.expect(path.relationships()).andReturn(pathSoFar);
        }
    }

    private void createOutgoingExpectations(RelationshipFactory mockRelationshipFactory, int outgoingStageCost,
                                            String service, int[] outgoingTimes, Relationship outgoingTram) {
        EasyMock.expect(mockRelationshipFactory.getRelationship(departs)).andStubReturn(departsRelationship);
        boolean[] expectedDays = new boolean[] { true, false, false, false, false, false, false };
        TramGoesToRelationship outgoingRelationship = new TramGoesToRelationship(service, outgoingStageCost,
                expectedDays, outgoingTimes, "id", startDate, endDate, "destTo",TransportMode.Tram);
        EasyMock.expect(mockRelationshipFactory.getRelationship(outgoingTram)).andStubReturn(outgoingRelationship);
    }

    private Path setNodeExpectations(NodeFactory mockNodeFactory, Set<Relationship> relationships,
                                     Node endNode, TramNode tramNode) {
        Path path = createMock(Path.class);

        EasyMock.expect(path.endNode()).andReturn(endNode);
        EasyMock.expect(mockNodeFactory.getNode(endNode)).andReturn(tramNode);
        EasyMock.expect(endNode.getRelationships(Direction.OUTGOING)).andReturn(relationships);
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

    TramRelationship departsRelationship = new TramRelationship() {
        @Override
        public boolean isTramGoesTo() {
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
        public boolean isWalk() {return false;}

        @Override
        public int getCost() {
            return 2;
        }

        @Override
        public String getId() {
            return "ID";
        }

        @Override
        public TransportMode getMode() {
            return TransportMode.Tram;
        }
    };

}
