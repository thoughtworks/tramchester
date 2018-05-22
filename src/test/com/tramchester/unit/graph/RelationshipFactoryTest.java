package com.tramchester.unit.graph;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TramGoesToRelationship;
import com.tramchester.graph.Relationships.TransportRelationship;
import com.tramchester.graph.TransportRelationshipTypes;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import static org.junit.Assert.*;


public class RelationshipFactoryTest extends EasyMockSupport {

    private Relationship relationship;
    private RelationshipFactory relationshipFactory;
    private StubNode startTramNode;
    private StubNode endTramNode;

    @Before
    public void beforeEachTestRuns() throws TramchesterException {
        NodeFactory nodeFactory = createMock(NodeFactory.class);
        relationshipFactory = new RelationshipFactory(nodeFactory);

        relationship = createMock(Relationship.class);
        Node startNode = createMock(Node.class);
        Node endNode = createMock(Node.class);
        EasyMock.expect(relationship.getStartNode()).andReturn(startNode);
        EasyMock.expect(relationship.getEndNode()).andReturn(endNode);

        startTramNode = new StubNode();
        endTramNode = new StubNode();
        EasyMock.expect(nodeFactory.getNode(startNode)).andReturn(startTramNode);
        EasyMock.expect(nodeFactory.getNode(endNode)).andReturn(endTramNode);
    }

    @Test
    public void shouldHaveBoradingRelationship() throws TramchesterException {
        setRelationshipExpectation(TransportRelationshipTypes.BOARD, 45, 101L);

        replayAll();
        TransportRelationship transportRelationship = relationshipFactory.getRelationship(relationship);
        assertTrue(transportRelationship.isBoarding());
        assertFalse(transportRelationship.isWalk());
        assertFalse(transportRelationship.isEnterPlatform());
        assertEquals(45, transportRelationship.getCost());
        assertSame(startTramNode, transportRelationship.getStartNode());
        assertSame(endTramNode, transportRelationship.getEndNode());
        verifyAll();
    }

    @Test
    public void shouldHaveEnterPlatformRelationship() throws TramchesterException {
        setRelationshipExpectation(TransportRelationshipTypes.ENTER_PLATFORM, 0, 102L);

        replayAll();
        TransportRelationship transportRelationship = relationshipFactory.getRelationship(relationship);
        assertFalse(transportRelationship.isBoarding());
        assertFalse(transportRelationship.isWalk());
        assertTrue(transportRelationship.isEnterPlatform());
        assertEquals(0, transportRelationship.getCost());
        assertSame(startTramNode, transportRelationship.getStartNode());
        assertSame(endTramNode, transportRelationship.getEndNode());
        verifyAll();
    }

    @Test
    public void shouldHaveLeavePlatformRelationship() throws TramchesterException {
        setRelationshipExpectation(TransportRelationshipTypes.LEAVE_PLATFORM, 0, 103L);

        replayAll();
        TransportRelationship transportRelationship = relationshipFactory.getRelationship(relationship);
        assertFalse(transportRelationship.isBoarding());
        assertFalse(transportRelationship.isWalk());
        assertTrue(transportRelationship.isLeavePlatform());
        assertEquals(0, transportRelationship.getCost());
        assertSame(startTramNode, transportRelationship.getStartNode());
        assertSame(endTramNode, transportRelationship.getEndNode());
        verifyAll();
    }

    @Test
    public void shouldHaveDepartRelationship() throws TramchesterException {
        setRelationshipExpectation(TransportRelationshipTypes.DEPART, 43, 104L);

        replayAll();
        TransportRelationship transportRelationship = relationshipFactory.getRelationship(relationship);
        assertTrue(transportRelationship.isDepartTram());
        assertFalse(transportRelationship.isWalk());
        assertFalse(transportRelationship.isEnterPlatform());
        assertEquals(43, transportRelationship.getCost());
        assertSame(startTramNode, transportRelationship.getStartNode());
        assertSame(endTramNode, transportRelationship.getEndNode());
        verifyAll();
    }

    @Test
    public void shouldHaveInterchangeBoardRelationship() throws TramchesterException {

        setRelationshipExpectation(TransportRelationshipTypes.INTERCHANGE_BOARD, 44, 105L);

        replayAll();
        TransportRelationship transportRelationship = relationshipFactory.getRelationship(relationship);
        assertTrue(transportRelationship.isInterchange());
        assertTrue(transportRelationship.isBoarding());
        assertFalse(transportRelationship.isDepartTram());
        assertFalse(transportRelationship.isWalk());
        assertFalse(transportRelationship.isEnterPlatform());
        assertEquals(44, transportRelationship.getCost());
        assertSame(startTramNode, transportRelationship.getStartNode());
        assertSame(endTramNode, transportRelationship.getEndNode());
        verifyAll();
    }

    @Test
    public void shouldHaveWalkToRelationship() throws TramchesterException {
        setRelationshipExpectation(TransportRelationshipTypes.WALKS_TO, 6, 106L);

        replayAll();
        TransportRelationship transportRelationship = relationshipFactory.getRelationship(relationship);
        assertFalse(transportRelationship.isInterchange());
        assertFalse(transportRelationship.isBoarding());
        assertFalse(transportRelationship.isDepartTram());
        assertFalse(transportRelationship.isEnterPlatform());
        assertTrue(transportRelationship.isWalk());
        assertEquals(6, transportRelationship.getCost());
        assertSame(startTramNode, transportRelationship.getStartNode());
        assertSame(endTramNode, transportRelationship.getEndNode());
        verifyAll();

    }

    private void setRelationshipExpectation(TransportRelationshipTypes relationshipType, int cost, Long id) {
        EasyMock.expect(relationship.getId()).andReturn(id);
        EasyMock.expect(relationship.getType()).andReturn(relationshipType);
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.COST)).andReturn(cost);
    }

    @Test
    public void shouldHaveInterchangeDepartsRelationship() throws TramchesterException {
        setRelationshipExpectation(TransportRelationshipTypes.INTERCHANGE_DEPART, 55, 107L);

        replayAll();
        TransportRelationship transportRelationship = relationshipFactory.getRelationship(relationship);
        assertTrue(transportRelationship.isInterchange());
        assertFalse(transportRelationship.isBoarding());
        assertTrue(transportRelationship.isDepartTram());
        assertFalse(transportRelationship.isEnterPlatform());
        assertEquals(55, transportRelationship.getCost());
        assertFalse(transportRelationship.isWalk());
        assertSame(startTramNode, transportRelationship.getStartNode());
        assertSame(endTramNode, transportRelationship.getEndNode());
        verifyAll();
    }

    @Test
    public void shouldHaveGoesToRelationshipWithCostAndService() throws TramchesterException {
        boolean[] days = new boolean[]{true, false, true, false};
        int[] times = new int[]{10, 20, 30, 40};

        setRelationshipExpectation(TransportRelationshipTypes.TRAM_GOES_TO, 42, 108L);
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.SERVICE_ID)).andReturn("service99");
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.DAYS)).andReturn(days);
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.TIMES)).andReturn(times);
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.DESTINATION)).andReturn("dest");
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.SERVICE_START_DATE)).andReturn("20151025");
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.SERVICE_END_DATE)).andReturn("20161124");

        replayAll();
        TramGoesToRelationship tramRelationship = (TramGoesToRelationship) relationshipFactory.getRelationship(relationship);
        assertEquals(42, tramRelationship.getCost());
        assertEquals("service99", tramRelationship.getService());
        assertEquals("dest", tramRelationship.getDest());
        assertSame(times, tramRelationship.getTimesServiceRuns());
        assertSame(days, tramRelationship.getDaysServiceRuns());
        assertEquals(new LocalDate(2015, 10, 25), tramRelationship.getStartDate().getDate());
        assertEquals(new LocalDate(2016, 11, 24), tramRelationship.getEndDate().getDate());
        assertSame(startTramNode, tramRelationship.getStartNode());
        assertSame(endTramNode, tramRelationship.getEndNode());
        verifyAll();
    }

    private class StubNode implements TramNode {
        @Override
        public boolean isStation() {
            return false;
        }

        @Override
        public boolean isRouteStation() {
            return false;
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public boolean isQuery() {
            return false;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public boolean isPlatform() {
            return false;
        }
    }
}
