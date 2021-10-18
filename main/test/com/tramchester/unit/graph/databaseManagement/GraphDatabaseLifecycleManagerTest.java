package com.tramchester.unit.graph.databaseManagement;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.graph.databaseManagement.GraphDatabaseLifecycleManager;
import com.tramchester.graph.databaseManagement.GraphDatabaseServiceFactory;
import com.tramchester.graph.databaseManagement.GraphDatabaseStoredVersions;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.GraphDatabaseService;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;

public class GraphDatabaseLifecycleManagerTest extends EasyMockSupport {

    private GraphDatabaseLifecycleManager graphDatabaseLifecycleManager;
    private GraphDatabaseStoredVersions storedVersions;
    private GraphDatabaseServiceFactory serviceFactory;
    private GraphDatabaseService graphDatabaseService;

    private final Path dbFile = Path.of("someFilename");

    @BeforeEach
    public void onceBeforeEachTestRuns() {

        TramchesterConfig config = TestEnv.GET();

        graphDatabaseService = createMock(GraphDatabaseService.class);
        serviceFactory = createMock(GraphDatabaseServiceFactory.class);
        storedVersions = createMock(GraphDatabaseStoredVersions.class);
        graphDatabaseLifecycleManager = new GraphDatabaseLifecycleManager(config, serviceFactory, storedVersions);
    }

    @Test
    public void startImmediatelyIfExistsAndStoredVersionsOK() {
        Set<DataSourceInfo> dataSourceInfo = new HashSet<>();

        EasyMock.expect(serviceFactory.create()).andReturn(graphDatabaseService);
        EasyMock.expect(storedVersions.upToDate(graphDatabaseService, dataSourceInfo)).andReturn(true);

        replayAll();
        GraphDatabaseService result = graphDatabaseLifecycleManager.startDatabase(dataSourceInfo, dbFile, true);
        verifyAll();

        assertSame(graphDatabaseService, result);
    }

    @Test
    public void startImmediatelyIfNotExists() {
        Set<DataSourceInfo> dataSourceInfo = new HashSet<>();

        EasyMock.expect(serviceFactory.create()).andReturn(graphDatabaseService);

        replayAll();
        GraphDatabaseService result = graphDatabaseLifecycleManager.startDatabase(dataSourceInfo, dbFile, false);
        verifyAll();

        assertSame(graphDatabaseService, result);
    }

    @Test
    public void startAndThenStopIfExistsAndStoredVersionsStale() {
        Set<DataSourceInfo> dataSourceInfo = new HashSet<>();

        EasyMock.expect(serviceFactory.create()).andReturn(graphDatabaseService);
        EasyMock.expect(storedVersions.upToDate(graphDatabaseService, dataSourceInfo)).andReturn(false);
        serviceFactory.shutdownDatabase();
        EasyMock.expectLastCall();

        // wait for shutdown
        EasyMock.expect(graphDatabaseService.isAvailable(200L)).andReturn(true);
        EasyMock.expect(graphDatabaseService.isAvailable(200L)).andReturn(false);

        // restart
        EasyMock.expect(serviceFactory.create()).andReturn(graphDatabaseService);

        replayAll();
        GraphDatabaseService result = graphDatabaseLifecycleManager.startDatabase(dataSourceInfo, dbFile, true);
        verifyAll();

        assertSame(graphDatabaseService, result);
    }
}
