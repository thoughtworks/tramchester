package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.RouteCodesForTesting;
import com.tramchester.integration.Stations;
import com.tramchester.repository.ReachabilityRepository;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.unsafe.impl.batchimport.stats.Stat;

import java.io.IOException;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class ReachabilityRepositoryTest {
    private static Dependencies dependencies;
    private static IntegrationTramTestConfig testConfig;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void shouldCreateReachabilityMatrix() {
        ReachabilityRepository repo = dependencies.get(ReachabilityRepository.class);

        assertFalse(repo.reachable(Stations.NavigationRoad.getId()+ RouteCodesForTesting.BURY_TO_ALTY,
                Stations.TraffordBar.getId()));
        assertTrue(repo.reachable(Stations.NavigationRoad.getId()+ RouteCodesForTesting.ALTY_TO_BURY,
                Stations.TraffordBar.getId()));
        assertFalse(repo.reachable(Stations.NavigationRoad.getId()+ RouteCodesForTesting.BURY_TO_ALTY,
                Stations.ManAirport.getId()));
        assertTrue(repo.reachable(Stations.NavigationRoad.getId()+ RouteCodesForTesting.ALTY_TO_BURY,
                Stations.ManAirport.getId()));
        assertTrue(repo.reachable(Stations.NavigationRoad.getId()+ RouteCodesForTesting.ALTY_TO_BURY,
                Stations.NavigationRoad.getId()));

        assertTrue(repo.reachable(Stations.RochdaleRail.getId() + RouteCodesForTesting.ROCH_TO_DIDS, Stations.Monsall.getId()));
        assertFalse(repo.reachable(Stations.RochdaleRail.getId() + RouteCodesForTesting.DIDS_TO_ROCH, Stations.Monsall.getId()));

        assertTrue(repo.reachable(Stations.Monsall.getId() + RouteCodesForTesting.DIDS_TO_ROCH, Stations.RochdaleRail.getId()));

        // towards victoria, so find an interchange
        assertTrue(repo.reachable(Stations.Monsall.getId() + RouteCodesForTesting.ROCH_TO_DIDS, Stations.RochdaleRail.getId()));

    }

}
