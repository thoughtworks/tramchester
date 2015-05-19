package com.tramchester.graph;

import com.tramchester.graph.Nodes.RouteStationNode;
import com.tramchester.graph.Nodes.StationNode;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;

import static org.junit.Assert.assertEquals;

public class TestRouteStationNode extends EasyMockSupport {
    private Node node;

    @Before
    public void beforeEachTestRuns() {
        node = createMock(Node.class);
    }

    @Test
    public void shouldHaveExpectedProperties() {
        EasyMock.expect(node.getProperty(GraphStaticKeys.ID)).andReturn("stationId");
        EasyMock.expect(node.getProperty(GraphStaticKeys.RouteStation.ROUTE_NAME)).andReturn("routeName");
        EasyMock.expect(node.getProperty(GraphStaticKeys.RouteStation.ROUTE_ID)).andReturn("routeId");


        replayAll();
        RouteStationNode stationNode = new RouteStationNode(node);
        assertEquals("stationId", stationNode.getId());
        assertEquals("routeName", stationNode.getRouteName());
        assertEquals("routeId", stationNode.getRouteId());

        verifyAll();
    }
}
