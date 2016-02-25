package com.tramchester.graph;

import com.tramchester.graph.Nodes.RouteStationNode;
import com.tramchester.graph.Nodes.StationNode;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.LinkedList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
        EasyMock.expect(node.getProperty(GraphStaticKeys.RouteStation.ROUTE_ID)).andReturn("routeId");
        EasyMock.expect(node.getProperty(GraphStaticKeys.RouteStation.STATION_NAME)).andReturn("stationName");

        replayAll();
        RouteStationNode stationNode = new RouteStationNode(node);
        assertEquals("stationId", stationNode.getId());
        assertEquals("routeName", stationNode.getRouteName());
        assertEquals("routeId", stationNode.getRouteId());
        assertEquals("stationName", stationNode.getName());
        verifyAll();
    }
}
