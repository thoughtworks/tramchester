package com.tramchester.unit.graph.databaseManagement;

import com.tramchester.domain.DataSourceInfo;
import com.tramchester.graph.databaseManagement.GraphDatabaseMetaInfo;
import com.tramchester.graph.graphbuild.GraphLabel;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDateTime;
import java.util.*;

import static com.tramchester.domain.DataSourceID.naptanStopsCSV;
import static com.tramchester.domain.DataSourceID.tfgm;
import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.*;

public class GraphDatabaseMetaInfoTest extends EasyMockSupport {

    private GraphDatabaseMetaInfo databaseMetaInfo;
    private Transaction transaction;
    private Node node;

    @BeforeEach
    public void beforeAnyTestRuns() {
        node = createMock(Node.class);
        transaction = createMock(Transaction.class);
        databaseMetaInfo = new GraphDatabaseMetaInfo();
    }

    @Test
    void shouldCheckForNeighboursNodePresent() {

        ResourceIteratorForTest nodes = new ResourceIteratorForTest(Collections.singletonList(node));
        EasyMock.expect(transaction.findNodes(GraphLabel.NEIGHBOURS_ENABLED)).andReturn(nodes);

        replayAll();
        boolean result = databaseMetaInfo.isNeighboursEnabled(transaction);
        verifyAll();

        assertTrue(result);
    }

    @Test
    void shouldCheckForNeighboursNodeMissing() {

        ResourceIteratorForTest nodes = new ResourceIteratorForTest();
        EasyMock.expect(transaction.findNodes(GraphLabel.NEIGHBOURS_ENABLED)).andReturn(nodes);

        replayAll();
        boolean result = databaseMetaInfo.isNeighboursEnabled(transaction);
        verifyAll();

        assertFalse(result);
    }

    @Test
    void shouldCheckForVersionNodeMissing() {

        ResourceIteratorForTest nodes = new ResourceIteratorForTest();
        EasyMock.expect(transaction.findNodes(GraphLabel.VERSION)).andReturn(nodes);

        replayAll();
        boolean result = databaseMetaInfo.hasVersionInfo(transaction);
        verifyAll();

        assertFalse(result);
    }

    @Test
    void shouldCheckForVersionNodePresent() {

        ResourceIteratorForTest nodes = new ResourceIteratorForTest(Collections.singletonList(node));
        EasyMock.expect(transaction.findNodes(GraphLabel.VERSION)).andReturn(nodes);

        replayAll();
        boolean result = databaseMetaInfo.hasVersionInfo(transaction);
        verifyAll();

        assertTrue(result);
    }

    @Test
    void shouldGetVersionMapFromNode() {
        Map<String, Object> versionMap = new HashMap<>();
        versionMap.put("A", "4.2");
        versionMap.put("ZZZ", "81.91");

        ResourceIteratorForTest nodes = new ResourceIteratorForTest(Collections.singletonList(node));
        EasyMock.expect(transaction.findNodes(GraphLabel.VERSION)).andReturn(nodes);
        EasyMock.expect(node.getAllProperties()).andReturn(versionMap);

        replayAll();
        Map<String, String> results = databaseMetaInfo.getVersions(transaction);
        verifyAll();

        assertEquals(2, results.size());
        assertTrue(results.containsKey("A"));
        assertEquals(results.get("A"), "4.2");
        assertTrue(results.containsKey("ZZZ"));
        assertEquals(results.get("ZZZ"), "81.91");
    }

    @Test
    void shouldSetNeighbourNode() {

        EasyMock.expect(transaction.createNode(GraphLabel.NEIGHBOURS_ENABLED)).andReturn(node);

        replayAll();
        databaseMetaInfo.setNeighboursEnabled(transaction);
        verifyAll();
    }

    @Test
    void shouldCreateVersionsNode() {
        Set<DataSourceInfo> sourceInfo = new HashSet<>();
        sourceInfo.add(new DataSourceInfo(tfgm, "4.3", LocalDateTime.MAX, Collections.singleton(Tram)));
        sourceInfo.add(new DataSourceInfo(naptanStopsCSV, "9.6", LocalDateTime.MIN, Collections.singleton(Bus)));

        EasyMock.expect(transaction.createNode(GraphLabel.VERSION)).andReturn(node);
        node.setProperty("tfgm", "4.3");
        EasyMock.expectLastCall();
        node.setProperty("naptanStopsCSV", "9.6");
        EasyMock.expectLastCall();

        replayAll();
        databaseMetaInfo.createVersionNode(transaction, sourceInfo);
        verifyAll();
    }

    private static class ResourceIteratorForTest implements ResourceIterator<Node> {
        private final LinkedList<Node> nodes;

        private ResourceIteratorForTest(final List<Node> nodes) {
            this.nodes = new LinkedList<>(nodes);
        }

        public ResourceIteratorForTest() {
            nodes = new LinkedList<>();
        }

        @Override
        public void close() {
            nodes.clear();
        }

        @Override
        public boolean hasNext() {
            return !nodes.isEmpty();
        }

        @Override
        public Node next() {
            return nodes.removeFirst();
        }
    }
}
