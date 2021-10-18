package com.tramchester.unit.graph.calculation;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphDatabaseLifecycleManager;
import com.tramchester.graph.search.GraphDatabaseServiceFactory;
import com.tramchester.repository.DataSourceRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphDatabaseTest {

    private static SimpleGraphConfig config;
    private static ComponentContainer componentContainer;

    @BeforeEach
    void beforeEachTest() throws IOException {
        config = new SimpleGraphConfig("GraphDatabaseTest");
        TestEnv.deleteDBIfPresent(config);


        componentContainer = new ComponentsBuilder().
                overrideProvider(TramTransportDataForTestFactory.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterEach
    void afterEachTest() throws IOException {
        TestEnv.deleteDBIfPresent(config);
    }

    @Test
    void shouldStartDatabase() {
        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);

        graphDatabase.start();
        assertTrue(graphDatabase.isAvailable(5000));
        graphDatabase.stop();
    }

}
