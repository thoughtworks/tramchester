package com.tramchester.unit.graph.calculation;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.HourNodeCache;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeCachePopulationTests {
    private static ComponentContainer componentContainer;
    private static SimpleGraphConfig config;

    private Transaction txn;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        config = new SimpleGraphConfig("ServiceNodeCacheTest.db");
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder<TramTransportDataForTestFactory>().
                overrideProvider(TramTransportDataForTestFactory.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void onceAfterAllTestsRun() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        GraphDatabase database = componentContainer.get(GraphDatabase.class);
        txn = database.beginTx();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldPopulateCacheForHours() {

        List<Integer> expectedHours = Arrays.asList(8,9);
        HourNodeCache hourNodeCache = componentContainer.get(HourNodeCache.class);

        ResourceIterator<Node> hourNodes = txn.findNodes(GraphBuilder.Labels.HOUR);
        while (hourNodes.hasNext()) {
            Node hourNode = hourNodes.next();
            int found = hourNodeCache.getHourFor(hourNode.getId());
            assertTrue(expectedHours.contains(found));
        }
    }
}