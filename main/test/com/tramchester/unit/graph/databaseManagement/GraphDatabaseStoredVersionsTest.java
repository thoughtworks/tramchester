package com.tramchester.unit.graph.databaseManagement;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.graph.databaseManagement.GraphDatabaseMetaInfo;
import com.tramchester.graph.databaseManagement.GraphDatabaseStoredVersions;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDateTime;
import java.util.*;

import static com.tramchester.domain.DataSourceID.naptanStopsCSV;
import static com.tramchester.domain.DataSourceID.tfgm;
import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GraphDatabaseStoredVersionsTest extends EasyMockSupport {

    private GraphDatabaseMetaInfo databaseMetaInfo;
    private GraphDatabaseStoredVersions storedVersions;
    private GraphDatabaseService databaseService;
    private Transaction transaction;
    private TramchesterConfig config;

    @Before
    public void beforeAnyTestsRun() {

        config = TestEnv.GET();
        databaseMetaInfo = createMock(GraphDatabaseMetaInfo.class);
        databaseService = createMock(GraphDatabaseService.class);
        transaction = createMock(Transaction.class);
        storedVersions = new GraphDatabaseStoredVersions(config, databaseMetaInfo);

    }

    @Test
    public void shouldOutOfDateIfNeighboursNot() {
        Set<DataSourceInfo> dataSourceInfo = new HashSet<>();

        EasyMock.expect(databaseService.beginTx()).andReturn(transaction);
        EasyMock.expect(databaseMetaInfo.isNeighboursEnabled(transaction)).andReturn(!config.getCreateNeighbours());
        transaction.close();
        EasyMock.expectLastCall();

        replayAll();
        boolean result = storedVersions.upToDate(databaseService, dataSourceInfo);
        verifyAll();

        assertFalse(result);
    }

    @Test
    public void shouldOutOfDateIfVersionMissingFromDB() {
        Set<DataSourceInfo> dataSourceInfo = new HashSet<>();

        EasyMock.expect(databaseService.beginTx()).andReturn(transaction);
        EasyMock.expect(databaseMetaInfo.isNeighboursEnabled(transaction)).andReturn(config.getCreateNeighbours());
        EasyMock.expect(databaseMetaInfo.hasVersionInfo(transaction)).andReturn(false);
        transaction.close();
        EasyMock.expectLastCall();

        replayAll();
        boolean result = storedVersions.upToDate(databaseService, dataSourceInfo);
        verifyAll();

        assertFalse(result);
    }

    @Test
    public void shouldBeUpToDateIfVersionsFromDBMatch() {
        Set<DataSourceInfo> dataSourceInfo = new HashSet<>();
        Map<String, String> versionMap = new HashMap<>();

        dataSourceInfo.add(new DataSourceInfo(tfgm, "v1.1", LocalDateTime.MIN, Collections.singleton(Tram)));
        dataSourceInfo.add(new DataSourceInfo(naptanStopsCSV, "v2.3", LocalDateTime.MIN, Collections.singleton(Bus)));

        versionMap.put("tfgm", "v1.1");
        versionMap.put("naptanStopsCSV", "v2.3");

        EasyMock.expect(databaseService.beginTx()).andReturn(transaction);
        EasyMock.expect(databaseMetaInfo.isNeighboursEnabled(transaction)).andReturn(config.getCreateNeighbours());
        EasyMock.expect(databaseMetaInfo.hasVersionInfo(transaction)).andReturn(true);
        EasyMock.expect(databaseMetaInfo.getVersions(transaction)).andReturn(versionMap);
        transaction.close();
        EasyMock.expectLastCall();

        replayAll();
        boolean result = storedVersions.upToDate(databaseService, dataSourceInfo);
        verifyAll();

        assertTrue(result);
    }

    @Test
    public void shouldOutOfDateIfVersionsNumbersFromDBMisMatch() {
        Set<DataSourceInfo> dataSourceInfo = new HashSet<>();
        Map<String, String> versionMap = new HashMap<>();

        dataSourceInfo.add(new DataSourceInfo(tfgm, "v1.2", LocalDateTime.MIN, Collections.singleton(Tram)));
        dataSourceInfo.add(new DataSourceInfo(naptanStopsCSV, "v2.3", LocalDateTime.MIN, Collections.singleton(Bus)));

        versionMap.put("tfgm", "v1.1");
        versionMap.put("naptancsv", "v2.3");

        EasyMock.expect(databaseService.beginTx()).andReturn(transaction);
        EasyMock.expect(databaseMetaInfo.isNeighboursEnabled(transaction)).andReturn(config.getCreateNeighbours());
        EasyMock.expect(databaseMetaInfo.hasVersionInfo(transaction)).andReturn(true);
        EasyMock.expect(databaseMetaInfo.getVersions(transaction)).andReturn(versionMap);
        transaction.close();
        EasyMock.expectLastCall();

        replayAll();
        boolean result = storedVersions.upToDate(databaseService, dataSourceInfo);
        verifyAll();

        assertFalse(result);
    }

    @Test
    public void shouldOutOfDateIfVersionsFromDBMisMatch() {
        Set<DataSourceInfo> dataSourceInfo = new HashSet<>();
        Map<String, String> versionMap = new HashMap<>();

        dataSourceInfo.add(new DataSourceInfo(tfgm, "v1.2", LocalDateTime.MIN, Collections.singleton(Tram)));
        dataSourceInfo.add(new DataSourceInfo(naptanStopsCSV, "v2.3", LocalDateTime.MIN, Collections.singleton(Bus)));

        versionMap.put("tfgm", "v1.1");

        EasyMock.expect(databaseService.beginTx()).andReturn(transaction);
        EasyMock.expect(databaseMetaInfo.isNeighboursEnabled(transaction)).andReturn(config.getCreateNeighbours());
        EasyMock.expect(databaseMetaInfo.hasVersionInfo(transaction)).andReturn(true);
        EasyMock.expect(databaseMetaInfo.getVersions(transaction)).andReturn(versionMap);
        transaction.close();
        EasyMock.expectLastCall();

        replayAll();
        boolean result = storedVersions.upToDate(databaseService, dataSourceInfo);
        verifyAll();

        assertFalse(result);
    }

    @Test
    public void shouldOutOfDateIfVersionsFromDBMisMatchUnexpected() {
        Set<DataSourceInfo> dataSourceInfo = new HashSet<>();
        Map<String, String> versionMap = new HashMap<>();

        dataSourceInfo.add(new DataSourceInfo(tfgm, "v1.2", LocalDateTime.MIN, Collections.singleton(Tram)));

        versionMap.put("tfgm", "v1.1");
        versionMap.put("naptancsv", "v2.3");

        EasyMock.expect(databaseService.beginTx()).andReturn(transaction);
        EasyMock.expect(databaseMetaInfo.isNeighboursEnabled(transaction)).andReturn(config.getCreateNeighbours());
        EasyMock.expect(databaseMetaInfo.hasVersionInfo(transaction)).andReturn(true);
        EasyMock.expect(databaseMetaInfo.getVersions(transaction)).andReturn(versionMap);
        transaction.close();
        EasyMock.expectLastCall();

        replayAll();
        boolean result = storedVersions.upToDate(databaseService, dataSourceInfo);
        verifyAll();

        assertFalse(result);
    }

}
