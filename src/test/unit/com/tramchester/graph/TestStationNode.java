package com.tramchester.graph;

import com.tramchester.graph.Nodes.StationNode;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;

import static org.junit.Assert.assertEquals;

public class TestStationNode extends EasyMockSupport {

    private Node node;

    @Before
    public void beforeEachTestRuns() {
        node = createMock(Node.class);
    }

    @Test
    public void shouldHaveExpectedProperties() {
        EasyMock.expect(node.getProperty(GraphStaticKeys.ID)).andReturn("stationId");
        EasyMock.expect(node.getProperty(GraphStaticKeys.Station.NAME)).andReturn("stationName");

        replayAll();
        StationNode stationNode = new StationNode(node);
        assertEquals("stationId", stationNode.getId());
        assertEquals("stationName", stationNode.getName());
        verifyAll();
    }

}
