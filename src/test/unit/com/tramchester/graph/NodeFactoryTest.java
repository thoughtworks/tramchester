package com.tramchester.graph;


import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NodeFactoryTest extends EasyMockSupport {

    private Node node;
    private NodeFactory factory;

    @Before
    public void beforeEachTestRuns() {
        node = createMock(Node.class);
        factory = new NodeFactory();
    }

    @Test
    public void shouldGetNodeOfCorrectTypeStation() {
        EasyMock.expect(node.getProperty(GraphStaticKeys.STATION_TYPE)).andReturn(GraphStaticKeys.STATION);
        EasyMock.expect(node.getProperty(GraphStaticKeys.ID)).andReturn("stationId");
        EasyMock.expect(node.getProperty(GraphStaticKeys.Station.NAME)).andReturn("stationName");

        replayAll();
        TramNode tramNode = factory.getNode(node);
        assertTrue(tramNode.isStation());
        assertFalse(tramNode.isRouteStation());
        assertFalse(tramNode.isQuery());
        verifyAll();
    }

    @Test
    public void shouldGetNodeOfCorrectTypeQuery() {
        EasyMock.expect(node.getProperty(GraphStaticKeys.STATION_TYPE)).andReturn(GraphStaticKeys.QUERY);

        replayAll();
        TramNode tramNode = factory.getNode(node);
        assertFalse(tramNode.isStation());
        assertFalse(tramNode.isRouteStation());
        assertTrue(tramNode.isQuery());
        verifyAll();
    }

    @Test
    public void shouldGetNodeOfCorrectTypeRouteStation() {
        EasyMock.expect(node.getProperty(GraphStaticKeys.STATION_TYPE)).andReturn(GraphStaticKeys.ROUTE_STATION);
        EasyMock.expect(node.getProperty(GraphStaticKeys.ID)).andReturn("stationId");
        EasyMock.expect(node.getProperty(GraphStaticKeys.RouteStation.ROUTE_NAME)).andReturn("routeName");
        EasyMock.expect(node.getProperty(GraphStaticKeys.RouteStation.ROUTE_ID)).andReturn("routeId");

        replayAll();
        TramNode tramNode = factory.getNode(node);
        assertFalse(tramNode.isStation());
        assertTrue(tramNode.isRouteStation());
        assertFalse(tramNode.isQuery());
        verifyAll();
    }
}
