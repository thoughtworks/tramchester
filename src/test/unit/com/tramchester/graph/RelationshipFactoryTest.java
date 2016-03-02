package com.tramchester.graph;

import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TramGoesToRelationship;
import com.tramchester.graph.Relationships.TransportRelationship;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;

import static org.junit.Assert.*;


public class RelationshipFactoryTest extends EasyMockSupport {

    private Relationship relationship;
    private RelationshipFactory relationshipFactory;

    @Before
    public void beforeEachTestRuns() {
        relationshipFactory = new RelationshipFactory();
        relationship = createMock(Relationship.class);
    }

    @Test
    public void shouldHaveBoradingRelationship() {
        setRelationshipExpectation(TransportRelationshipTypes.BOARD, 45);

        replayAll();
        TransportRelationship transportRelationship = relationshipFactory.getRelationship(relationship);
        assertTrue(transportRelationship.isBoarding());
        assertFalse(transportRelationship.isWalk());
        assertEquals(45, transportRelationship.getCost());
        verifyAll();
    }

    @Test
    public void shouldHaveDepartRelationship() {
        setRelationshipExpectation(TransportRelationshipTypes.DEPART, 43);

        replayAll();
        TransportRelationship transportRelationship = relationshipFactory.getRelationship(relationship);
        assertTrue(transportRelationship.isDepartTram());
        assertFalse(transportRelationship.isWalk());
        assertEquals(43, transportRelationship.getCost());
        verifyAll();
    }

    @Test
    public void shouldHaveInterchangeBoardRelationship() {

        setRelationshipExpectation(TransportRelationshipTypes.INTERCHANGE_BOARD, 44);

        replayAll();
        TransportRelationship transportRelationship = relationshipFactory.getRelationship(relationship);
        assertTrue(transportRelationship.isInterchange());
        assertTrue(transportRelationship.isBoarding());
        assertFalse(transportRelationship.isDepartTram());
        assertFalse(transportRelationship.isWalk());
        assertEquals(44, transportRelationship.getCost());
        verifyAll();
    }

    @Test
    public void shouldHaveWalkToRelationship() {
        setRelationshipExpectation(TransportRelationshipTypes.WALKS_TO, 6);

        replayAll();
        TransportRelationship transportRelationship = relationshipFactory.getRelationship(relationship);
        assertFalse(transportRelationship.isInterchange());
        assertFalse(transportRelationship.isBoarding());
        assertFalse(transportRelationship.isDepartTram());
        assertTrue(transportRelationship.isWalk());
        assertEquals(6, transportRelationship.getCost());
        verifyAll();

    }

    private void setRelationshipExpectation(TransportRelationshipTypes relationshipType, int cost) {
        EasyMock.expect(relationship.getType()).andReturn(relationshipType);
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.COST)).andReturn(cost);
    }

    @Test
    public void shouldHaveInterchangeDepartsRelationship() {
        setRelationshipExpectation(TransportRelationshipTypes.INTERCHANGE_DEPART, 55);

        replayAll();
        TransportRelationship transportRelationship = relationshipFactory.getRelationship(relationship);
        assertTrue(transportRelationship.isInterchange());
        assertFalse(transportRelationship.isBoarding());
        assertTrue(transportRelationship.isDepartTram());
        assertEquals(55, transportRelationship.getCost());
        assertFalse(transportRelationship.isWalk());
        verifyAll();
    }

    @Test
    public void shouldHaveGoesToRelationshipWithCostAndService() {
        boolean[] days = new boolean[]{true, false, true, false};
        int[] times = new int[]{10, 20, 30, 40};

        setRelationshipExpectation(TransportRelationshipTypes.TRAM_GOES_TO, 42);
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.SERVICE_ID)).andReturn("service99");
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.DAYS)).andReturn(days);
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.TIMES)).andReturn(times);
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.ROUTE_STATION)).andReturn("dest");
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.SERVICE_START_DATE)).andReturn("20151025");
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.SERVICE_END_DATE)).andReturn("20161124");

        replayAll();
        TramGoesToRelationship tramRelationship = (TramGoesToRelationship) relationshipFactory.getRelationship(relationship);
        assertEquals(42, tramRelationship.getCost());
        assertEquals("service99", tramRelationship.getService());
        assertEquals("dest", tramRelationship.getDest());
        assertSame(times, tramRelationship.getTimesTramRuns());
        assertSame(days, tramRelationship.getDaysTramRuns());
        assertEquals(new LocalDate(2015, 10, 25), tramRelationship.getStartDate().getDate());
        assertEquals(new LocalDate(2016, 11, 24), tramRelationship.getEndDate().getDate());
        verifyAll();
    }
}
