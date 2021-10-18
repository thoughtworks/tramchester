package com.tramchester.unit.graph;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphDatabaseLifecycleManager;
import com.tramchester.integration.testSupport.GraphDBTestConfig;
import com.tramchester.integration.testSupport.IntegrationTestConfig;
import com.tramchester.repository.DataSourceRepository;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.GraphDatabaseService;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphDatabaseLifecycleTest extends EasyMockSupport {

    private static final DataSourceID SRC_1_NAME = DataSourceID.gbRail;
    private static final String VERSION_1_VALID = "version1";

    private Set<DataSourceInfo> namesAndVersions;
    private List<GTFSSourceConfig> dataSourceConfigs;
    private GraphDatabase graphDatabase;
    private GraphDatabaseLifecycleManager lifecycleManager;
    private GraphDatabaseService graphDatabaseService;

    @BeforeEach
    void beforeEachTestRuns() {
        String dbName = "graphDbTest.db";

        GraphDBTestConfig dbConfig = new GraphDBTestConfig("graphDatabaseTest", dbName);
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
    }

    @Test
    void shouldStartLifeCycleManagerClean() {

        LocalDateTime lastModTime = LocalDateTime.now();
        Set<TransportMode> modes = Collections.singleton(TransportMode.Tram);

        namesAndVersions.add(new DataSourceInfo(SRC_1_NAME, VERSION_1_VALID, lastModTime, modes));

        EasyMock.expect(lifecycleManager.startDatabase(namesAndVersions)).andReturn(graphDatabaseService);
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
    void shouldStartLifeCycleManagerNotClean() {

        LocalDateTime lastModTime = LocalDateTime.now();
        Set<TransportMode> modes = Collections.singleton(TransportMode.Tram);

        namesAndVersions.add(new DataSourceInfo(SRC_1_NAME, VERSION_1_VALID, lastModTime, modes));

        EasyMock.expect(lifecycleManager.startDatabase(namesAndVersions)).andReturn(graphDatabaseService);
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
