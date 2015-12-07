package com.tramchester.graph;

import com.tramchester.graph.Relationships.TramGoesToRelationship;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TramRelationship;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;

import static org.junit.Assert.*;


public class TestTramRelationship extends EasyMockSupport {

    private Relationship relationship;
    private RelationshipFactory relationshipFactory;

    @Before
    public void beforeEachTestRuns() {
        relationshipFactory = new RelationshipFactory();
        relationship = createMock(Relationship.class);
    }

    @Test
    public void shouldHaveBoradingRelationship() {
        EasyMock.expect(relationship.getType()).andReturn(TransportRelationshipTypes.BOARD);
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.COST)).andReturn(45);

        replayAll();
        TramRelationship tramRelationship = relationshipFactory.getRelationship(relationship);
        assertTrue(tramRelationship.isBoarding());
        assertEquals(45, tramRelationship.getCost());
        verifyAll();
    }

    @Test
    public void shouldHaveDepartRelationship() {
        EasyMock.expect(relationship.getType()).andReturn(TransportRelationshipTypes.DEPART);
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.COST)).andReturn(43);

        replayAll();
        TramRelationship tramRelationship = relationshipFactory.getRelationship(relationship);
        assertTrue(tramRelationship.isDepartTram());
        assertEquals(43, tramRelationship.getCost());
        verifyAll();
    }

    @Test
    public void shouldHaveInterchangeBoardRelationship() {
        EasyMock.expect(relationship.getType()).andReturn(TransportRelationshipTypes.INTERCHANGE_BOARD);
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.COST)).andReturn(44);

        replayAll();
        TramRelationship tramRelationship = relationshipFactory.getRelationship(relationship);
        assertTrue(tramRelationship.isInterchange());
        assertTrue(tramRelationship.isBoarding());
        assertFalse(tramRelationship.isDepartTram());
        assertEquals(44, tramRelationship.getCost());
        verifyAll();
    }

    @Test
    public void shouldHaveInterchangeDepartsRelationship() {
        EasyMock.expect(relationship.getType()).andReturn(TransportRelationshipTypes.INTERCHANGE_DEPART);
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.COST)).andReturn(55);

        replayAll();
        TramRelationship tramRelationship = relationshipFactory.getRelationship(relationship);
        assertTrue(tramRelationship.isInterchange());
        assertFalse(tramRelationship.isBoarding());
        assertTrue(tramRelationship.isDepartTram());
        assertEquals(55, tramRelationship.getCost());
        verifyAll();
    }

    @Test
    public void shouldHaveGoesToRelationshipWithCostAndService() {
        boolean[] days = new boolean[]{true, false, true, false};
        int[] times = new int[]{10, 20, 30, 40};

        EasyMock.expect(relationship.getType()).andReturn(TransportRelationshipTypes.TRAM_GOES_TO);

        EasyMock.expect(relationship.getProperty(GraphStaticKeys.COST)).andReturn(42);
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
        assertEquals(new DateTime(2015, 10, 25, 0, 0), tramRelationship.getStartDate().getDate());
        assertEquals(new DateTime(2016, 11, 24, 0, 0), tramRelationship.getEndDate().getDate());
        verifyAll();
    }
}
