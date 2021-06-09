package com.tramchester.unit.graph.cache;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.caches.NodeTypeRepository;
import com.tramchester.graph.caches.NodeTypeCache;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import com.tramchester.unit.graph.calculation.SimpleGraphConfig;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
class NodeTypeCacheTest {
    private static ComponentContainer componentContainer;
    private static SimpleGraphConfig config;

    private Transaction txn;
    private NodeTypeCache cache;
    private GraphDatabase database;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        config = new SimpleGraphConfig("NodeIdLabelMapTest.db");
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
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
        database = componentContainer.get(GraphDatabase.class);
        cache = componentContainer.get(NodeTypeCache.class);

        txn = database.beginTx();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveCorrectValuesInTime() {
        checkFor(NodeTypeRepository::isTime, GraphLabel.MINUTE);
    }

    @Test
    void shouldHaveCorrectValuesInHour() {
        checkFor(NodeTypeRepository::isHour, GraphLabel.HOUR);
    }

    @Test
    void shouldHaveCorrectValuesInService() {
        checkFor(NodeTypeRepository::isService, GraphLabel.SERVICE);
    }

    @Test
    void shouldHaveCorrectValuesInRouteStation() {
        checkFor(NodeTypeRepository::isRouteStation, GraphLabel.ROUTE_STATION);
    }

    private void checkFor(ValidatesFor validatesFor, GraphLabel label) {
        Set<Node> directs = database.findNodes(txn, label).stream().collect(Collectors.toSet());
        for (Node direct : directs) {
            assertTrue(validatesFor.is(cache, direct));
        }
    }

    interface ValidatesFor {
        boolean is(NodeTypeRepository cache, Node node);
    }
}
