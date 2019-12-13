package com.tramchester.unit.graph;

import com.tramchester.domain.TramTime;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TramGoesToRelationship;
import com.tramchester.graph.Relationships.TransportRelationship;
import com.tramchester.graph.TransportGraphBuilder;
import com.tramchester.graph.TransportRelationshipTypes;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class RelationshipFactoryTest extends EasyMockSupport {

    private Relationship relationship;
    private RelationshipFactory relationshipFactory;
    private StubNode startTramNode;
    private StubNode endTramNode;

    @Before
    public void beforeEachTestRuns() {
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
    public void shouldHaveBoradingRelationship() {
        setRelationshipExpectation(TransportRelationshipTypes.BOARD, -1, 101L);

        replayAll();
        TransportRelationship transportRelationship = relationshipFactory.getRelationship(relationship);
        assertTrue(transportRelationship.isBoarding());
        assertFalse(transportRelationship.isWalk());
        assertFalse(transportRelationship.isEnterPlatform());
        assertEquals(TransportGraphBuilder.BOARDING_COST, transportRelationship.getCost());
        assertSame(startTramNode, transportRelationship.getStartNode());
        assertSame(endTramNode, transportRelationship.getEndNode());
        verifyAll();
    }

    @Test
    public void shouldHaveEnterPlatformRelationship() {
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
    public void shouldHaveLeavePlatformRelationship() {
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
    public void shouldHaveDepartRelationship() {
        setRelationshipExpectation(TransportRelationshipTypes.DEPART, -1, 104L);

        replayAll();
        TransportRelationship transportRelationship = relationshipFactory.getRelationship(relationship);
        assertTrue(transportRelationship.isDepartTram());
        assertFalse(transportRelationship.isWalk());
        assertFalse(transportRelationship.isEnterPlatform());
        assertEquals(TransportGraphBuilder.DEPARTS_COST, transportRelationship.getCost());
        assertSame(startTramNode, transportRelationship.getStartNode());
        assertSame(endTramNode, transportRelationship.getEndNode());
        verifyAll();
    }

    @Test
    public void shouldHaveInterchangeBoardRelationship() {

        setRelationshipExpectation(TransportRelationshipTypes.INTERCHANGE_BOARD, -1, 105L);

        replayAll();
        TransportRelationship transportRelationship = relationshipFactory.getRelationship(relationship);
        assertTrue(transportRelationship.isInterchange());
        assertTrue(transportRelationship.isBoarding());
        assertFalse(transportRelationship.isDepartTram());
        assertFalse(transportRelationship.isWalk());
        assertFalse(transportRelationship.isEnterPlatform());
        assertEquals(TransportGraphBuilder.INTERCHANGE_BOARD_COST, transportRelationship.getCost());
        assertSame(startTramNode, transportRelationship.getStartNode());
        assertSame(endTramNode, transportRelationship.getEndNode());
        verifyAll();
    }

    @Test
    public void shouldHaveWalkToRelationship() {
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
        if (cost>-1) {
            EasyMock.expect(relationship.getProperty(GraphStaticKeys.COST)).andReturn(cost);
        }
    }

    @Test
    public void shouldHaveInterchangeDepartsRelationship() {
        setRelationshipExpectation(TransportRelationshipTypes.INTERCHANGE_DEPART, -1, 107L);

        replayAll();
        TransportRelationship transportRelationship = relationshipFactory.getRelationship(relationship);
        assertTrue(transportRelationship.isInterchange());
        assertFalse(transportRelationship.isBoarding());
        assertTrue(transportRelationship.isDepartTram());
        assertFalse(transportRelationship.isEnterPlatform());
        assertEquals(TransportGraphBuilder.INTERCHANGE_DEPART_COST, transportRelationship.getCost());
        assertFalse(transportRelationship.isWalk());
        assertSame(startTramNode, transportRelationship.getStartNode());
        assertSame(endTramNode, transportRelationship.getEndNode());
        verifyAll();
    }

    @Test
    public void shouldHaveGoesToRelationshipWithCostAndService() {
        boolean[] days = new boolean[]{true, false, true, false};
        LocalTime[] times = new LocalTime[]{LocalTime.of(0,10), LocalTime.of(0,20),
                LocalTime.of(0,30), LocalTime.of(0,40)};
        List<TramTime> tramTimes = new ArrayList<>();
        for (int i = 0; i < times.length; i++) {
            tramTimes.add(TramTime.of(times[i]));
        }

        setRelationshipExpectation(TransportRelationshipTypes.TRAM_GOES_TO, 42, 108L);
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.SERVICE_ID)).andReturn("service99");
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.DAYS)).andReturn(days);
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.TIMES)).andReturn(times);

        replayAll();
        TramGoesToRelationship tramRelationship = (TramGoesToRelationship) relationshipFactory.getRelationship(relationship);
        assertEquals(42, tramRelationship.getCost());
        assertEquals("service99", tramRelationship.getServiceId());

        assertEquals(tramTimes, Arrays.asList(tramRelationship.getTimesServiceRuns()));
        assertSame(days, tramRelationship.getDaysServiceRuns());
        assertSame(startTramNode, tramRelationship.getStartNode());
        assertSame(endTramNode, tramRelationship.getEndNode());
        verifyAll();
    }

    @Test
    public void shouldHaveGoesToRelationshipWithCostServiceAndTrip() {
        boolean[] days = new boolean[]{true, false, true, false};
        LocalTime time = LocalTime.of(8,33);

        setRelationshipExpectation(TransportRelationshipTypes.TRAM_GOES_TO, 42, 108L);
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.SERVICE_ID)).andReturn("service99");
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.DAYS)).andReturn(days);

        EasyMock.expect(relationship.getProperty(GraphStaticKeys.DEPART_TIME)).andReturn(time);
        EasyMock.expect(relationship.hasProperty(GraphStaticKeys.TRIP_ID)).andReturn(true);
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.TRIP_ID)).andReturn("tripId");

        replayAll();
        TramGoesToRelationship tramRelationship = (TramGoesToRelationship) relationshipFactory.getRelationship(relationship);
        assertEquals(42, tramRelationship.getCost());
        assertEquals("service99", tramRelationship.getServiceId());
        assertEquals(TramTime.of(time), tramRelationship.getTimeServiceRuns());
        assertTrue(tramRelationship.hasTripId());
        assertEquals("tripId", tramRelationship.getTripId());
        assertSame(days, tramRelationship.getDaysServiceRuns());

        assertSame(startTramNode, tramRelationship.getStartNode());
        assertSame(endTramNode, tramRelationship.getEndNode());
        verifyAll();
    }

    private class StubNode extends TramNode {

        @Override
        public String getId() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

    }
}
