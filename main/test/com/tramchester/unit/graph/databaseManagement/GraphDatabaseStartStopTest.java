package com.tramchester.unit.graph.databaseManagement;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.databaseManagement.GraphDatabaseLifecycleManager;
import com.tramchester.integration.testSupport.GraphDBTestConfig;
import com.tramchester.integration.testSupport.IntegrationTestConfig;
import com.tramchester.repository.DataSourceRepository;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphDatabaseStartStopTest extends EasyMockSupport {

    private static final DataSourceID SRC_1_NAME = DataSourceID.gbRailGTFS;
    private static final String VERSION_1_VALID = "version1";

    private Set<DataSourceInfo> namesAndVersions;
    private List<GTFSSourceConfig> dataSourceConfigs;
    private GraphDatabase graphDatabase;
    private GraphDatabaseLifecycleManager lifecycleManager;
    private GraphDatabaseService graphDatabaseService;
    private GraphDBTestConfig dbConfig;

    @BeforeEach
    void beforeEachTestRuns() throws IOException {
        String dbName = "graphDbTest.db";

        dbConfig = new GraphDBTestConfig("graphDatabaseTest", dbName);
        TramchesterConfig config = new IntegrationTestConfig(dbConfig) {
            @Override
            protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
                return dataSourceConfigs;
            }
        };

        namesAndVersions = new HashSet<>();
        dataSourceConfigs = new ArrayList<>();

        DataSourceRepository repository = new DataSourceRepository() {
            @Override
            public Set<DataSourceInfo> getDataSourceInfo() {
                return namesAndVersions;
            }

            @Override
            public LocalDateTime getNewestModTimeFor(TransportMode mode) {
                return null;
            }
        };

        lifecycleManager = createMock(GraphDatabaseLifecycleManager.class);
        graphDatabaseService = createMock(GraphDatabaseService.class);

        graphDatabase = new GraphDatabase(config, repository, lifecycleManager);

        final Path dbPath = dbConfig.getDbPath();
        Files.createDirectories(dbPath.getParent());
        Files.createFile(dbPath);
    }

    @AfterEach
    public void afterEachTestRuns() throws IOException {
        Files.deleteIfExists(dbConfig.getDbPath());
    }

    @Test
    void shouldStartLifeCycleManagerCleanExistingFile() {

        LocalDateTime lastModTime = LocalDateTime.now();
        Set<TransportMode> modes = Collections.singleton(TransportMode.Tram);

        namesAndVersions.add(new DataSourceInfo(SRC_1_NAME, VERSION_1_VALID, lastModTime, modes));

        EasyMock.expect(lifecycleManager.startDatabase(namesAndVersions, dbConfig.getDbPath(), true)).
                andReturn(graphDatabaseService);
        EasyMock.expect(lifecycleManager.isCleanDB()).andReturn(true);
        lifecycleManager.stopDatabase();
        EasyMock.expectLastCall();

        replayAll();
        graphDatabase.start();
        assertTrue(graphDatabase.isCleanDB());
        graphDatabase.stop();
        verifyAll();

    }

    @Test
    void shouldStartLifeCycleNoFile() throws IOException {
        Files.delete(dbConfig.getDbPath());

        LocalDateTime lastModTime = LocalDateTime.now();
        Set<TransportMode> modes = Collections.singleton(TransportMode.Tram);

        namesAndVersions.add(new DataSourceInfo(SRC_1_NAME, VERSION_1_VALID, lastModTime, modes));

        EasyMock.expect(lifecycleManager.startDatabase(namesAndVersions, dbConfig.getDbPath(), false)).
                andReturn(graphDatabaseService);
        EasyMock.expect(lifecycleManager.isCleanDB()).andReturn(true);
        lifecycleManager.stopDatabase();
        EasyMock.expectLastCall();

        replayAll();
        graphDatabase.start();
        assertTrue(graphDatabase.isCleanDB());
        graphDatabase.stop();
        verifyAll();

    }

    @Test
    void shouldStartLifeCycleManagerNotCleanExistingFile() {

        LocalDateTime lastModTime = LocalDateTime.now();
        Set<TransportMode> modes = Collections.singleton(TransportMode.Tram);

        namesAndVersions.add(new DataSourceInfo(SRC_1_NAME, VERSION_1_VALID, lastModTime, modes));

        EasyMock.expect(lifecycleManager.startDatabase(namesAndVersions,  dbConfig.getDbPath(), true)).
                andReturn(graphDatabaseService);
        EasyMock.expect(lifecycleManager.isCleanDB()).andReturn(false);
        lifecycleManager.stopDatabase();
        EasyMock.expectLastCall();

        replayAll();
        graphDatabase.start();
        assertFalse(graphDatabase.isCleanDB());
        graphDatabase.stop();
        verifyAll();
    }

}
