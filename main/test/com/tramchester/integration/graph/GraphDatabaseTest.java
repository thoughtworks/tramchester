package com.tramchester.integration.graph;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.GTFSTransportationType;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.repository.DataSourceRepository;
import com.tramchester.testSupport.TestConfig;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GraphDatabaseTest {

    private static final String SRC_1_NAME = "src1";
    private static final String SRC_2_NAME = "src2";
    private static final String VERSION_1_VALID = "version1";
    private static final String VERSION_2_VALID = "version21";
    private static final int SHUTDOWN_TIMEOUT_MILLI = 500;

    private TramchesterConfig config;
    private DataSourceRepository repository;
    private DataSourceInfo dataSourceInfo;
    private Set<DataSourceInfo.NameAndVersion> namesAndVersions;
    private List<DataSourceConfig> dataSourceConfigs;
    private Path dbFile;
    private GraphDatabase graphDatabase;

    @BeforeEach
    void beforeEachTestRuns() throws IOException {
        graphDatabase = null;
        Path dir = Paths.get("graphDbTest.db");
        if (Files.exists(dir)) {
            FileUtils.deleteDirectory(dir.toFile());
        }

        dbFile = Files.createDirectory(dir);
        namesAndVersions = new HashSet<>();
        dataSourceConfigs = new ArrayList<>();

        dataSourceInfo = new DataSourceInfo(namesAndVersions);
        repository = () -> dataSourceInfo;
        config = new TestConfig() {
            @Override
            protected List<DataSourceConfig> getDataSourceFORTESTING() {
                return dataSourceConfigs;
            }

            @Override
            public String getGraphName() {
                return dbFile.toAbsolutePath().toString();
            }
        };
    }

    @AfterEach
    void afterEachTestRuns() throws IOException {
        if (graphDatabase!=null) {
            if (graphDatabase.isAvailable(1000)) {
                graphDatabase.stop();
            }
            while (graphDatabase.isAvailable(100)) {
                // wait for stop, immediate deletion does not work
            }
        }
        FileUtils.deleteDirectory(dbFile.toFile());
    }

    @Test
    void shouldCreateIfNoDBFile() {
        createWithSingleNameAndVersion();
        assertTrue(graphDatabase.isAvailable(1000));
        assertTrue(graphDatabase.isCleanDB());
    }

    @Test
    void shouldCreateIfVersionNodeMissingFromDB() {
        createWithSingleNameAndVersion();
        assertTrue(graphDatabase.isAvailable(1000));

        try(Transaction tx = graphDatabase.beginTx()) {
            graphDatabase.createNode(tx, GraphBuilder.Labels.QUERY_NODE);
            tx.commit();
        }
        assertEquals(1, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());

        graphDatabase.stop();
        assertFalse(graphDatabase.isAvailable(SHUTDOWN_TIMEOUT_MILLI));

        graphDatabase.start();
        assertTrue(graphDatabase.isAvailable(1000));
        assertTrue(graphDatabase.isCleanDB());
        // query node gone, fresh DB
        assertEquals(0, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());
    }


    @Test
    void shouldLoadExistingIfVersionPresentWithCorrectVersionInDB() {
        createWithSingleNameAndVersion();

        assertTrue(graphDatabase.isAvailable(1000));
        try(Transaction tx = graphDatabase.beginTx()) {
            Node node = graphDatabase.createNode(tx, GraphBuilder.Labels.VERSION);
            node.setProperty(SRC_1_NAME, VERSION_1_VALID);
            // create a node in the DB to check for when we reload the same DB file
            graphDatabase.createNode(tx, GraphBuilder.Labels.QUERY_NODE);
            tx.commit();
        }
        assertEquals(1, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());

        graphDatabase.stop();
        assertFalse(graphDatabase.isAvailable(SHUTDOWN_TIMEOUT_MILLI));

        graphDatabase.start();
        assertTrue(graphDatabase.isAvailable(1000));
        assertFalse(graphDatabase.isCleanDB());
        // query node still present
        assertEquals(1, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());
    }

    @Test
    void shouldLoadExistingIfVersionPresentWithMultipleCorrectVersionsInDB() {
        createWithTwoNamesAndVersions();

        assertTrue(graphDatabase.isAvailable(1000));
        try(Transaction tx = graphDatabase.beginTx()) {
            Node node = graphDatabase.createNode(tx, GraphBuilder.Labels.VERSION);
            node.setProperty(SRC_1_NAME, VERSION_1_VALID);
            node.setProperty(SRC_2_NAME, VERSION_2_VALID);
            // create a node in the DB to check for when we reload the same DB file
            graphDatabase.createNode(tx, GraphBuilder.Labels.QUERY_NODE);
            tx.commit();
        }
        assertEquals(1, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());

        graphDatabase.stop();
        assertFalse(graphDatabase.isAvailable(SHUTDOWN_TIMEOUT_MILLI));

        graphDatabase.start();
        assertTrue(graphDatabase.isAvailable(1000));
        assertFalse(graphDatabase.isCleanDB());
        // query node still present
        AtomicInteger count = countNodeType(GraphBuilder.Labels.QUERY_NODE);
        assertEquals(1, count.intValue());
    }

    @Test
    void shoulRecreateIfExtraVersionPresentInDB() {
        createWithTwoNamesAndVersions();

        assertTrue(graphDatabase.isAvailable(1000));
        try(Transaction tx = graphDatabase.beginTx()) {
            Node node = graphDatabase.createNode(tx, GraphBuilder.Labels.VERSION);
            node.setProperty(SRC_1_NAME, VERSION_1_VALID);
            node.setProperty(SRC_2_NAME, VERSION_2_VALID);
            node.setProperty("src3", "anotherVersion");
            // create a node in the DB to check for when we reload the same DB file
            graphDatabase.createNode(tx, GraphBuilder.Labels.QUERY_NODE);
            tx.commit();
        }
        assertEquals(1, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());

        graphDatabase.stop();
        assertFalse(graphDatabase.isAvailable(SHUTDOWN_TIMEOUT_MILLI));

        graphDatabase.start();
        assertTrue(graphDatabase.isAvailable(1000));
        assertTrue(graphDatabase.isCleanDB());
        assertEquals(0, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());
    }

    @Test
    void shouldRecreateIfVersionPresentWithMismatchVersionInDB() {
        createWithSingleNameAndVersion();
        assertTrue(graphDatabase.isAvailable(1000));

        try(Transaction tx = graphDatabase.beginTx()) {
            Node node = graphDatabase.createNode(tx, GraphBuilder.Labels.VERSION);
            node.setProperty(SRC_1_NAME, "XXXXXX");
            // create a node in the DB to check for when we reload the same DB file
            graphDatabase.createNode(tx, GraphBuilder.Labels.QUERY_NODE);
            tx.commit();
        }
        assertEquals(1, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());

        graphDatabase.stop();
        assertFalse(graphDatabase.isAvailable(SHUTDOWN_TIMEOUT_MILLI));

        graphDatabase.start();
        assertTrue(graphDatabase.isAvailable(1000));
        assertTrue(graphDatabase.isCleanDB());
        // query node gone, fresh DB
        assertEquals(0, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());
    }

    @Test
    void shouldRecreateIfVersionsPresentWithOneMismatchVersionInDB() {
        createWithTwoNamesAndVersions();
        assertTrue(graphDatabase.isAvailable(1000));

        try(Transaction tx = graphDatabase.beginTx()) {
            Node node = graphDatabase.createNode(tx, GraphBuilder.Labels.VERSION);
            node.setProperty(SRC_1_NAME, VERSION_1_VALID);
            node.setProperty(SRC_2_NAME, "XXXXX");
            // create a node in the DB to check for when we reload the same DB file
            graphDatabase.createNode(tx, GraphBuilder.Labels.QUERY_NODE);
            tx.commit();
        }
        assertEquals(1, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());

        graphDatabase.stop();
        assertFalse(graphDatabase.isAvailable(SHUTDOWN_TIMEOUT_MILLI));

        graphDatabase.start();
        assertTrue(graphDatabase.isAvailable(1000));
        assertTrue(graphDatabase.isCleanDB());
        // query node gone, fresh DB
        assertEquals(0, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());
    }

    @Test
    void shouldRecreateIfVersionsPresentWithOneMissingFromDB() {
        dataSourceConfigs.add(createDataSource(SRC_1_NAME));
        dataSourceConfigs.add(createDataSource(SRC_2_NAME));
        namesAndVersions.add(new DataSourceInfo.NameAndVersion(SRC_1_NAME, VERSION_1_VALID));
        namesAndVersions.add(new DataSourceInfo.NameAndVersion(SRC_2_NAME, "version42"));

        graphDatabase = new GraphDatabase(config, repository);
        graphDatabase.start();
        assertTrue(graphDatabase.isAvailable(1000));

        try(Transaction tx = graphDatabase.beginTx()) {
            Node node = graphDatabase.createNode(tx, GraphBuilder.Labels.VERSION);
            node.setProperty(SRC_2_NAME, "version42");
            // create a node in the DB to check for when we reload the same DB file
            graphDatabase.createNode(tx, GraphBuilder.Labels.QUERY_NODE);
            tx.commit();
        }
        assertEquals(1, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());
        graphDatabase.stop();
        assertFalse(graphDatabase.isAvailable(SHUTDOWN_TIMEOUT_MILLI));

        graphDatabase.start();
        assertTrue(graphDatabase.isAvailable(1000));
        assertTrue(graphDatabase.isCleanDB());
        assertEquals(0, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());
    }

    private void createWithTwoNamesAndVersions() {
        dataSourceConfigs.add(createDataSource(SRC_1_NAME));
        namesAndVersions.add(new DataSourceInfo.NameAndVersion(SRC_1_NAME, VERSION_1_VALID));
        dataSourceConfigs.add(createDataSource(SRC_2_NAME));
        namesAndVersions.add(new DataSourceInfo.NameAndVersion(SRC_2_NAME, VERSION_2_VALID));
        graphDatabase = new GraphDatabase(config, repository);
        graphDatabase.start();
    }

    private void createWithSingleNameAndVersion() {
        dataSourceConfigs.add(createDataSource(SRC_1_NAME));
        namesAndVersions.add(new DataSourceInfo.NameAndVersion(SRC_1_NAME, VERSION_1_VALID));
        graphDatabase = new GraphDatabase(config, repository);
        graphDatabase.start();
    }

    @NotNull
    private AtomicInteger countNodeType(GraphBuilder.Labels label) {
        AtomicInteger count = new AtomicInteger();
        try(Transaction tx = graphDatabase.beginTx()) {
            ResourceIterator<Node> nodes = graphDatabase.findNodes(tx, label);
            nodes.forEachRemaining(node -> count.getAndIncrement());
        }
        return count;
    }

    private DataSourceConfig createDataSource(String name) {
        return new DataSourceConfig() {
            @Override
            public String getTramDataUrl() {
                return null;
            }

            @Override
            public String getTramDataCheckUrl() {
                return null;
            }

            @Override
            public Path getDataPath() {
                return null;
            }

            @Override
            public Path getUnzipPath() {
                return null;
            }

            @Override
            public String getZipFilename() {
                return null;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public boolean getHasFeedInfo() {
                return false;
            }

            @Override
            public Set<GTFSTransportationType> getTransportModes() {
                return null;
            }
        };
    }
}
