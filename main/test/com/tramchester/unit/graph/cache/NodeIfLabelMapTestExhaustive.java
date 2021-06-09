package com.tramchester.unit.graph.cache;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.caches.NodeTypeCache;
import com.tramchester.graph.caches.NodeTypeRepository;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import com.tramchester.unit.graph.calculation.SimpleGraphConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeIfLabelMapTestExhaustive {
    private ComponentContainer componentContainer;
    private SimpleGraphConfig config;

    private Transaction txn;
    private NodeTypeCache cache;
    private GraphDatabase database;

    @BeforeEach
    void beforeEachTestRuns() throws IOException {
        config = new SimpleGraphConfig("NodeIdLabelMapTest.db");
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                overrideProvider(TramTransportDataForTestFactory.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        database = componentContainer.get(GraphDatabase.class);
        cache = componentContainer.get(NodeTypeCache.class);

        txn = database.beginTx();
    }

    @AfterEach
    void afterEachTestRuns() throws IOException {
        txn.close();
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
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

    private void checkFor(NodeTypeCacheTest.ValidatesFor validatesFor, GraphLabel label) {
        Set<Node> directs = database.findNodes(txn, label).stream().collect(Collectors.toSet());
        assertFalse(directs.isEmpty(), "missing");
        for (Node direct : directs) {
            assertTrue(validatesFor.is(cache, direct));
        }
    }
}
