package com.tramchester.integration.graph.databaseManagement;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.graph.databaseManagement.GraphDatabaseServiceFactory;
import com.tramchester.integration.testSupport.IntegrationTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GraphDatabaseServiceFactoryTest {
    private static GuiceContainerDependencies componentContainer;
    private static IntegrationTestConfig config;
    private static GraphDatabaseServiceFactory factory;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        String dbName = "graphDbTest.db";

        config = new IntegrationTramTestConfig(dbName, Collections.emptyList());
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        factory = componentContainer.get(GraphDatabaseServiceFactory.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @Test
    void shouldCreateAndStartDBAndRestart() {

        String labelName = "shouldCreateAndStartDBAndRestart";
        GraphDatabaseService dbService = factory.create();

        assertTrue(Files.exists(config.getGraphDBConfig().getDbPath()));
        assertTrue(dbService.isAvailable(200));

        List<String> intial = getAllLabels(dbService);
        assertFalse(intial.contains(labelName));

        // create a node with as label so can check persistence across restart
        try (Transaction txn = dbService.beginTx()) {
            txn.createNode(Label.label(labelName));
            txn.commit();
        }

        factory.shutdownDatabase();
        assertFalse(dbService.isAvailable(200));
        assertTrue(Files.exists(config.getGraphDBConfig().getDbPath()));

        // RESTART
        GraphDatabaseService secondService = factory.create();
        assertTrue(secondService.isAvailable(200));

        List<String> afterRestart = getAllLabels(secondService);
        assertTrue(afterRestart.contains(labelName));

        factory.shutdownDatabase();
        assertFalse(dbService.isAvailable(200));
    }

    private List<String> getAllLabels(GraphDatabaseService dbService) {
        List<String> allLabels = new LinkedList<>();
        try (Transaction txn = dbService.beginTx()) {
            Iterable<Label> labels = txn.getAllLabels();
            labels.forEach(label -> allLabels.add(label.name()));
        }
        return allLabels;
    }
}
