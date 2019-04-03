package com.tramchester.unit.graph;


import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.TransportGraphBuilder;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
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
    public void shouldGetNodeOfCorrectTypeStation() throws TramchesterException {
        createStationNode(node, "stationId", "stationName");

        replayAll();
        TramNode tramNode = factory.getNode(node);
        assertTrue(tramNode.isStation());
        assertFalse(tramNode.isRouteStation());
        assertFalse(tramNode.isQuery());
        verifyAll();
    }

    @Test
    public void shouldGetNodeOfCorrectTypeQuery() throws TramchesterException {
        expectLabels(node, TransportGraphBuilder.Labels.QUERY_NODE);

        //EasyMock.expect(node.getProperty(GraphStaticKeys.STATION_TYPE)).andReturn(GraphStaticKeys.QUERY);
        EasyMock.expect(node.getProperty(GraphStaticKeys.Station.LAT)).andReturn(-2);
        EasyMock.expect(node.getProperty(GraphStaticKeys.Station.LONG)).andReturn(1);

        replayAll();
        TramNode tramNode = factory.getNode(node);
        assertFalse(tramNode.isStation());
        assertFalse(tramNode.isRouteStation());
        assertTrue(tramNode.isQuery());
        verifyAll();
    }

    @Test
    public void shouldGetNodeOfCorrectTypePlatform() throws TramchesterException {
        expectLabels(node, TransportGraphBuilder.Labels.PLATFORM);

        EasyMock.expect(node.getProperty(GraphStaticKeys.ID)).andReturn("platformID");
        EasyMock.expect(node.getProperty(GraphStaticKeys.Station.NAME)).andReturn("platformName");

        replayAll();
        TramNode tramNode = factory.getNode(node);
        assertTrue(tramNode.isPlatform());
        assertEquals("platformName",tramNode.getName());
        assertEquals("platformID",tramNode.getId());

        verifyAll();
    }

    @Test
    public void shouldGetNodeOfCorrectTypeRouteStation() throws TramchesterException {
        expectLabels(node, TransportGraphBuilder.Labels.ROUTE_STATION);

        EasyMock.expect(node.getProperty(GraphStaticKeys.ID)).andReturn("stationId");
        EasyMock.expect(node.getProperty(GraphStaticKeys.RouteStation.ROUTE_NAME)).andReturn("routeName");
        EasyMock.expect(node.getProperty(GraphStaticKeys.ROUTE_ID)).andReturn("routeId");
        EasyMock.expect(node.getProperty(GraphStaticKeys.RouteStation.STATION_NAME)).andReturn("stationName");

        replayAll();
        TramNode tramNode = factory.getNode(node);
        assertFalse(tramNode.isStation());
        assertTrue(tramNode.isRouteStation());
        assertFalse(tramNode.isQuery());
        verifyAll();
    }

    public void createStationNode(Node node, String stationId, String stationName) {

        expectLabels(node, TransportGraphBuilder.Labels.STATION);

        EasyMock.expect(node.getProperty(GraphStaticKeys.ID)).andReturn(stationId);
        EasyMock.expect(node.getProperty(GraphStaticKeys.Station.NAME)).andReturn(stationName);
    }

    private void expectLabels(Node node, TransportGraphBuilder.Labels label) {
        List<Label> stationLabels = new LinkedList<>();
        stationLabels.add(label);
        EasyMock.expect(node.getLabels()).andReturn(stationLabels);
    }
}
