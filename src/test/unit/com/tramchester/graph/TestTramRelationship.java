package com.tramchester.graph;

import com.tramchester.graph.Relationships.GoesToRelationship;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TramRelationship;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;


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
    public void shouldHaveInterchangeRelationship() {
        EasyMock.expect(relationship.getType()).andReturn(TransportRelationshipTypes.INTERCHANGE);
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.COST)).andReturn(44);

        replayAll();
        TramRelationship tramRelationship = relationshipFactory.getRelationship(relationship);
        assertTrue(tramRelationship.isInterchange());
        assertEquals(44, tramRelationship.getCost());
        verifyAll();
    }

    @Test
    public void shouldHaveGoesToRealtionshipWithCostAndService() {
        boolean[] days = new boolean[]{true,false,true,false};
        int[] times = new int[]{10,20,30,40};

        EasyMock.expect(relationship.getType()).andReturn(TransportRelationshipTypes.GOES_TO);

        EasyMock.expect(relationship.getProperty(GraphStaticKeys.COST)).andReturn(42);
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.SERVICE_ID)).andReturn("service99");
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.DAYS)).andReturn(days);
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.TIMES)).andReturn(times);

        replayAll();
        GoesToRelationship tramRelationship = (GoesToRelationship) relationshipFactory.getRelationship(relationship);
        assertEquals(42, tramRelationship.getCost());
        assertEquals("service99", tramRelationship.getService());
        assertSame(times, tramRelationship.getTimesTramRuns());
        assertSame(days, tramRelationship.getDaysTramRuns());
        verifyAll();
    }
}
