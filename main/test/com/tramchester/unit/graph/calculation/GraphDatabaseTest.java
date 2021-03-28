package com.tramchester.unit.graph.calculation;

import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.graph.GraphDatabase;
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

    @BeforeEach
    void beforeEachTest() throws IOException {
        config = new SimpleGraphConfig("GraphDatabaseTest");
        TestEnv.deleteDBIfPresent(config);
    }

    @AfterEach
    void afterEachTest() throws IOException {
        TestEnv.deleteDBIfPresent(config);
    }

    @Test
    void shouldStartDatabase() {
        ProvidesLocalNow providesNow = new ProvidesLocalNow();
        DataSourceRepository transportData = new TramTransportDataForTestFactory.TramTransportDataForTest(providesNow);
        GraphDatabase graphDatabase = new GraphDatabase(config, transportData);

        graphDatabase.start();
        assertTrue(graphDatabase.isAvailable(5000));
        graphDatabase.stop();
    }

}
