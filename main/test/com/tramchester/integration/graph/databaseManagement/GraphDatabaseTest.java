package com.tramchester.integration.graph.databaseManagement;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import com.tramchester.unit.graph.calculation.SimpleGraphConfig;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphDatabaseTest {

    private static SimpleGraphConfig config;
    private static ComponentContainer componentContainer;

    @BeforeAll
    static void beforeEachTest() throws IOException {
        config = new SimpleGraphConfig("GraphDatabaseTest");
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                overrideProvider(TramTransportDataForTestFactory.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void afterEachTest() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @Test
    void shouldStartDatabase() {
        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);

        graphDatabase.start();
        assertTrue(graphDatabase.isAvailable(5000));
        graphDatabase.stop();

        // TODO EXPAND ME
    }

}
