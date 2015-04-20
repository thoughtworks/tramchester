package com.tramchester.graph;


import com.tramchester.domain.DaysOfWeek;
import com.tramchester.graph.Relationships.*;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphalgo.CommonEvaluators;
import org.neo4j.graphdb.Relationship;

import static com.tramchester.graph.GraphStaticKeys.COST;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestTimeBasedPathExpander extends EasyMockSupport {

    public static final int MAX_WAIT_MINUTES = 30;
    private int[] times = new int[] { 600, 700, 800, 900, 1000 };
    private Relationship departs;
    private Relationship boards;
    private Relationship interchange;
    private Relationship outgoingA;
    private Relationship outgoingB;
    private boolean[] days = new boolean[] {};

    @Before
    public void beforeEachTestRuns() {
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
        outgoingA = createMock(Relationship.class);
        EasyMock.expect(outgoingA.getType()).andStubReturn(TransportRelationshipTypes.GOES_TO);
        EasyMock.expect(outgoingA.getProperty(GraphStaticKeys.COST)).andStubReturn(3);
        EasyMock.expect(outgoingA.getProperty(GraphStaticKeys.SERVICE_ID)).andStubReturn("00063");
        EasyMock.expect(outgoingA.getProperty(GraphStaticKeys.DAYS)).andStubReturn(days);
        EasyMock.expect(outgoingA.getProperty(GraphStaticKeys.TIMES)).andStubReturn(times);
        // outgoing B
        outgoingB = createMock(Relationship.class);
        EasyMock.expect(outgoingB.getType()).andStubReturn(TransportRelationshipTypes.GOES_TO);
        EasyMock.expect(outgoingB.getProperty(GraphStaticKeys.COST)).andStubReturn(3);
        EasyMock.expect(outgoingB.getProperty(GraphStaticKeys.SERVICE_ID)).andStubReturn("00042");
        EasyMock.expect(outgoingB.getProperty(GraphStaticKeys.DAYS)).andStubReturn(days);
        EasyMock.expect(outgoingB.getProperty(GraphStaticKeys.TIMES)).andStubReturn(times);
    }

    @Test
    public void shouldHandleTimesWith30MinWait() {
        TimeBasedPathExpander expander = new TimeBasedPathExpander(CommonEvaluators.doubleCostEvaluator(COST), MAX_WAIT_MINUTES);

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
        TimeBasedPathExpander expander = new TimeBasedPathExpander(CommonEvaluators.doubleCostEvaluator(COST), 15);

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
        TimeBasedPathExpander expander = new TimeBasedPathExpander(CommonEvaluators.doubleCostEvaluator(COST), MAX_WAIT_MINUTES);

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
        GoesToRelationship outA = new GoesToRelationship(this.outgoingA);
        GoesToRelationship outB = new GoesToRelationship(this.outgoingB);
        TimeBasedPathExpander expander = new TimeBasedPathExpander(CommonEvaluators.doubleCostEvaluator(COST), MAX_WAIT_MINUTES);

        assertTrue(expander.noInFlightChangeOfService(board, outA));
        assertTrue(expander.noInFlightChangeOfService(depart, outA));
        assertTrue(expander.noInFlightChangeOfService(change, outA));
        assertTrue(expander.noInFlightChangeOfService(outA, outA));
        assertFalse(expander.noInFlightChangeOfService(outB, outA));
        verifyAll();
    }

    @Test
    public void shouldCheckIfTramRunsOnADay() {
        TimeBasedPathExpander expander = new TimeBasedPathExpander(CommonEvaluators.doubleCostEvaluator(COST), MAX_WAIT_MINUTES);

        checkDay(expander, true,false,false,false,false,false,false,DaysOfWeek.Monday);
        checkDay(expander, false,true,false,false,false,false,false,DaysOfWeek.Tuesday);
        checkDay(expander, false,false,true,false,false,false,false,DaysOfWeek.Wednesday);
        checkDay(expander, false,false,false,true,false,false,false,DaysOfWeek.Thursday);
        checkDay(expander, false,false,false,false,true,false,false,DaysOfWeek.Friday);
        checkDay(expander, false,false,false,false,false,true,false,DaysOfWeek.Saturday);
        checkDay(expander, false,false,false,false,false,false,true,DaysOfWeek.Sunday);
    }

    private void checkDay(TimeBasedPathExpander expander, boolean d1, boolean d2, boolean d3, boolean d4, boolean d5, boolean d6, boolean d7, DaysOfWeek day) {

        boolean[] days = new boolean[]{d1, d2, d3, d4, d5, d6, d7};
        assertTrue(expander.operatesOnDay(days, day));
    }

}
