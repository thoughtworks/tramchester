package com.tramchester.integration.graph;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.integration.testSupport.IntegrationTestConfig;
import com.tramchester.repository.DataSourceRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GraphDatabaseTest {

    private static final DataSourceID SRC_1_NAME = new DataSourceID("src1");
    private static final DataSourceID SRC_2_NAME = new DataSourceID("src2");
    private static final String VERSION_1_VALID = "version1";
    private static final String VERSION_2_VALID = "version21";
    private static final int SHUTDOWN_TIMEOUT_MILLI = 200;
    private static final int START_TIMEOUT_MILLI = 100;

    private TramchesterConfig config;
    private DataSourceRepository repository;
    private Set<DataSourceInfo> namesAndVersions;
    private List<DataSourceConfig> dataSourceConfigs;
    private Path dbFile;
    private GraphDatabase graphDatabase;
    private String dbName;

    @BeforeEach
    void beforeEachTestRuns() throws IOException {
        dbName = "graphDbTest.db";

        config = new IntegrationTestConfig("graphDatabaseTest", dbName) {
            @Override
            protected List<DataSourceConfig> getDataSourceFORTESTING() {
                return dataSourceConfigs;
            }
        };

        graphDatabase = null;
        Path dir = Paths.get(config.getGraphDBConfig().getGraphName());
        if (Files.exists(dir)) {
            FileUtils.deleteDirectory(dir.toFile());
        }

        dbFile = Files.createDirectories(dir);
        namesAndVersions = new HashSet<>();
        dataSourceConfigs = new ArrayList<>();

        repository = new DataSourceRepository() {
            @Override
            public Set<DataSourceInfo> getDataSourceInfo() {
                return namesAndVersions;
            }

            @Override
            public LocalDateTime getNewestModTimeFor(TransportMode mode) {
                return null;
            }
        };

//
//        config = new TestConfig() {
//            @Override
//            protected List<DataSourceConfig> getDataSourceFORTESTING() {
//                return dataSourceConfigs;
//            }
//
//            @Override
//            public String getGraphName() {
//                return dbFile.toAbsolutePath().toString();
//            }
//
//            @Override
//            public String getNeo4jPagecacheMemory() {
//                return "100m";
//            }
//        };
    }

    @AfterEach
    void afterEachTestRuns() throws IOException {
        if (graphDatabase!=null) {
            if (isAvailable(200)) {
                graphDatabase.stop();
            }
            while (isAvailable(START_TIMEOUT_MILLI)) {
                // wait for stop, immediate deletion does not work
            }
        }
        FileUtils.deleteDirectory(dbFile.toFile());
    }

    private boolean isAvailable(int timeoutMilli) {
        return graphDatabase.isAvailable(timeoutMilli);
    }

    @Test
    void shouldCreateIfNoDBFile() {
        createWithSingleNameAndVersion();
        assertTrue(isAvailable(START_TIMEOUT_MILLI));
        assertTrue(graphDatabase.isCleanDB());
    }

    @Test
    void shouldCreateIfVersionNodeMissingFromDB() {
        createWithSingleNameAndVersion();
        assertTrue(isAvailable(START_TIMEOUT_MILLI));

        try(Transaction tx = graphDatabase.beginTx()) {
            graphDatabase.createNode(tx, GraphBuilder.Labels.QUERY_NODE);
            tx.commit();
        }
        assertEquals(1, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());

        graphDatabase.stop();
        assertFalse(isAvailable(SHUTDOWN_TIMEOUT_MILLI));

        graphDatabase.start();
        assertTrue(isAvailable(1000));
        assertTrue(graphDatabase.isCleanDB());
        // query node gone, fresh DB
        assertEquals(0, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());
    }


    @Test
    void shouldLoadExistingIfVersionPresentWithCorrectVersionInDB() {
        createWithSingleNameAndVersion();

        assertTrue(isAvailable(START_TIMEOUT_MILLI));
        try(Transaction tx = graphDatabase.beginTx()) {
            Node node = graphDatabase.createNode(tx, GraphBuilder.Labels.VERSION);
            node.setProperty(SRC_1_NAME.getName(), VERSION_1_VALID);
            // create a node in the DB to check for when we reload the same DB file
            graphDatabase.createNode(tx, GraphBuilder.Labels.QUERY_NODE);
            tx.commit();
        }
        assertEquals(1, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());

        graphDatabase.stop();
        assertFalse(isAvailable(SHUTDOWN_TIMEOUT_MILLI));

        graphDatabase.start();
        assertTrue(isAvailable(START_TIMEOUT_MILLI));
        assertFalse(graphDatabase.isCleanDB());
        // query node still present
        assertEquals(1, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());
    }

    @Test
    void shouldLoadExistingIfVersionPresentWithMultipleCorrectVersionsInDB() {
        createWithTwoNamesAndVersions();

        assertTrue(isAvailable(START_TIMEOUT_MILLI));
        try(Transaction tx = graphDatabase.beginTx()) {
            Node node = graphDatabase.createNode(tx, GraphBuilder.Labels.VERSION);
            node.setProperty(SRC_1_NAME.getName(), VERSION_1_VALID);
            node.setProperty(SRC_2_NAME.getName(), VERSION_2_VALID);
            // create a node in the DB to check for when we reload the same DB file
            graphDatabase.createNode(tx, GraphBuilder.Labels.QUERY_NODE);
            tx.commit();
        }
        assertEquals(1, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());

        graphDatabase.stop();
        assertFalse(isAvailable(SHUTDOWN_TIMEOUT_MILLI));

        graphDatabase.start();
        assertTrue(isAvailable(START_TIMEOUT_MILLI));
        assertFalse(graphDatabase.isCleanDB());
        // query node still present
        AtomicInteger count = countNodeType(GraphBuilder.Labels.QUERY_NODE);
        assertEquals(1, count.intValue());
    }

    @Test
    void shoulRecreateIfExtraVersionPresentInDB() {
        createWithTwoNamesAndVersions();

        assertTrue(isAvailable(START_TIMEOUT_MILLI));
        try(Transaction tx = graphDatabase.beginTx()) {
            Node node = graphDatabase.createNode(tx, GraphBuilder.Labels.VERSION);
            node.setProperty(SRC_1_NAME.getName(), VERSION_1_VALID);
            node.setProperty(SRC_2_NAME.getName(), VERSION_2_VALID);
            node.setProperty("src3", "anotherVersion");
            // create a node in the DB to check for when we reload the same DB file
            graphDatabase.createNode(tx, GraphBuilder.Labels.QUERY_NODE);
            tx.commit();
        }
        assertEquals(1, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());

        graphDatabase.stop();
        assertFalse(isAvailable(SHUTDOWN_TIMEOUT_MILLI));

        graphDatabase.start();
        assertTrue(isAvailable(START_TIMEOUT_MILLI));
        assertTrue(graphDatabase.isCleanDB());
        assertEquals(0, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());
    }

    @Test
    void shouldRecreateIfVersionPresentWithMismatchVersionInDB() {
        createWithSingleNameAndVersion();
        assertTrue(isAvailable(START_TIMEOUT_MILLI));

        try(Transaction tx = graphDatabase.beginTx()) {
            Node node = graphDatabase.createNode(tx, GraphBuilder.Labels.VERSION);
            node.setProperty(SRC_1_NAME.getName(), "XXXXXX");
            // create a node in the DB to check for when we reload the same DB file
            graphDatabase.createNode(tx, GraphBuilder.Labels.QUERY_NODE);
            tx.commit();
        }
        assertEquals(1, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());

        graphDatabase.stop();
        assertFalse(isAvailable(SHUTDOWN_TIMEOUT_MILLI));

        graphDatabase.start();
        assertTrue(isAvailable(START_TIMEOUT_MILLI));
        assertTrue(graphDatabase.isCleanDB());
        // query node gone, fresh DB
        assertEquals(0, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());
    }

    @Test
    void shouldRecreateIfVersionsPresentWithOneMismatchVersionInDB() {
        createWithTwoNamesAndVersions();
        assertTrue(isAvailable(1000));

        try(Transaction tx = graphDatabase.beginTx()) {
            Node node = graphDatabase.createNode(tx, GraphBuilder.Labels.VERSION);
            node.setProperty(SRC_1_NAME.getName(), VERSION_1_VALID);
            node.setProperty(SRC_2_NAME.getName(), "XXXXX");
            // create a node in the DB to check for when we reload the same DB file
            graphDatabase.createNode(tx, GraphBuilder.Labels.QUERY_NODE);
            tx.commit();
        }
        assertEquals(1, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());

        graphDatabase.stop();
        assertFalse(isAvailable(SHUTDOWN_TIMEOUT_MILLI));

        graphDatabase.start();
        assertTrue(isAvailable(1000));
        assertTrue(graphDatabase.isCleanDB());
        // query node gone, fresh DB
        assertEquals(0, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());
    }

    @Test
    void shouldRecreateIfVersionsPresentWithOneMissingFromDB() {
        LocalDateTime lastModTime = LocalDateTime.now();
        Set<TransportMode> modes = Collections.singleton(TransportMode.Tram);

        dataSourceConfigs.add(createDataSource(SRC_1_NAME.getName()));
        dataSourceConfigs.add(createDataSource(SRC_2_NAME.getName()));
        namesAndVersions.add(new DataSourceInfo(SRC_1_NAME, VERSION_1_VALID, lastModTime, modes));
        namesAndVersions.add(new DataSourceInfo(SRC_2_NAME, "version42", lastModTime, modes));

        graphDatabase = new GraphDatabase(config, repository);
        graphDatabase.start();
        assertTrue(isAvailable(START_TIMEOUT_MILLI));

        try(Transaction tx = graphDatabase.beginTx()) {
            Node node = graphDatabase.createNode(tx, GraphBuilder.Labels.VERSION);
            node.setProperty(SRC_2_NAME.getName(), "version42");
            // create a node in the DB to check for when we reload the same DB file
            graphDatabase.createNode(tx, GraphBuilder.Labels.QUERY_NODE);
            tx.commit();
        }
        assertEquals(1, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());
        graphDatabase.stop();
        assertFalse(isAvailable(SHUTDOWN_TIMEOUT_MILLI));

        graphDatabase.start();
        assertTrue(isAvailable(START_TIMEOUT_MILLI));
        assertTrue(graphDatabase.isCleanDB());
        assertEquals(0, countNodeType(GraphBuilder.Labels.QUERY_NODE).intValue());
    }

    private void createWithTwoNamesAndVersions() {
        LocalDateTime lastModTime = LocalDateTime.now();
        Set<TransportMode> modes = Collections.singleton(TransportMode.Bus);

        dataSourceConfigs.add(createDataSource(SRC_1_NAME.getName()));
        namesAndVersions.add(new DataSourceInfo(SRC_1_NAME, VERSION_1_VALID, lastModTime, modes));
        dataSourceConfigs.add(createDataSource(SRC_2_NAME.getName()));
        namesAndVersions.add(new DataSourceInfo(SRC_2_NAME, VERSION_2_VALID, lastModTime, modes));
        graphDatabase = new GraphDatabase(config, repository);
        graphDatabase.start();
    }

    private void createWithSingleNameAndVersion() {
        LocalDateTime lastModTime = LocalDateTime.now();
        Set<TransportMode> modes = Collections.singleton(TransportMode.Tram);

        dataSourceConfigs.add(createDataSource(SRC_1_NAME.getName()));
        namesAndVersions.add(new DataSourceInfo(SRC_1_NAME, VERSION_1_VALID, lastModTime, modes));
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

            @Override
            public Set<TransportMode> getTransportModesWithPlatforms() {
                return null;
            }

            @Override
            public Set<LocalDate> getNoServices() {
                return Collections.emptySet();
            }
        };
    }
}
