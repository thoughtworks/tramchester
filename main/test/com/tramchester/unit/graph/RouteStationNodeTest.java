package com.tramchester.unit.graph;

import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.Nodes.BoardPointNode;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;

import static org.junit.Assert.assertEquals;

public class RouteStationNodeTest extends EasyMockSupport {
    private Node node;

    @Before
    public void beforeEachTestRuns() {
        node = createMock(Node.class);
    }

    @Test
    public void shouldHaveExpectedProperties() {
        EasyMock.expect(node.getProperty(GraphStaticKeys.ID)).andReturn("stationId");
        EasyMock.expect(node.getProperty(GraphStaticKeys.RouteStation.ROUTE_NAME)).andReturn("routeName");
        EasyMock.expect(node.getProperty(GraphStaticKeys.ROUTE_ID)).andReturn("routeId");
        EasyMock.expect(node.getProperty(GraphStaticKeys.RouteStation.STATION_NAME)).andReturn("stationName");

        replayAll();
        BoardPointNode stationNode = new BoardPointNode(node);
        assertEquals("stationId", stationNode.getId());
        assertEquals("routeName", stationNode.getRouteName());
        assertEquals("routeId", stationNode.getRouteId());
        assertEquals("stationName", stationNode.getName());
        verifyAll();
    }
}
