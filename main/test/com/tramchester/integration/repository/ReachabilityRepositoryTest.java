package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.RouteCodesForTesting;
import com.tramchester.testSupport.Stations;
import com.tramchester.repository.ReachabilityRepository;
import org.junit.*;

import java.io.IOException;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.fail;

public class ReachabilityRepositoryTest {
    private static Dependencies dependencies;
    private static IntegrationTramTestConfig testConfig;
    private ReachabilityRepository repo;

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

    @Before
    public void beforeEachTestRuns() {
        repo = dependencies.get(ReachabilityRepository.class);
    }

    @Test
    public void shouldCreateReachabilityMatrix() {

        // wrong direction
        assertFalse(repo.reachable(Stations.NavigationRoad.getId()+ RouteCodesForTesting.BURY_TO_ALTY,
                Stations.TraffordBar.getId()));
        // right direction
        assertTrue(repo.reachable(Stations.NavigationRoad.getId()+ RouteCodesForTesting.ALTY_TO_BURY,
                Stations.TraffordBar.getId()));
        // wrong direction
        assertFalse(repo.reachable(Stations.NavigationRoad.getId()+ RouteCodesForTesting.BURY_TO_ALTY,
                Stations.ManAirport.getId()));
        // right direction with interchange
        assertTrue(repo.reachable(Stations.NavigationRoad.getId()+ RouteCodesForTesting.ALTY_TO_BURY,
                Stations.ManAirport.getId()));
        // self reachable
        assertTrue(repo.reachable(Stations.NavigationRoad.getId()+ RouteCodesForTesting.ALTY_TO_BURY,
                Stations.NavigationRoad.getId()));

        // right direction
        assertTrue(repo.reachable(Stations.RochdaleRail.getId() + RouteCodesForTesting.ROCH_TO_DIDS, Stations.Monsall.getId()));
        // wrong direction
        assertFalse(repo.reachable(Stations.RochdaleRail.getId() + RouteCodesForTesting.DIDS_TO_ROCH, Stations.Monsall.getId()));
        // towards victoria, so find an interchange
        assertTrue(repo.reachable(Stations.Monsall.getId() + RouteCodesForTesting.ROCH_TO_DIDS, Stations.RochdaleRail.getId()));

    }

    @Test
    public void shouldRepoIssueAltyToDeangates() {
        assertTrue(repo.reachable(Stations.Altrincham.getId()+ RouteCodesForTesting.ALTY_TO_BURY,
                Stations.Deansgate.getId()));
        assertTrue(repo.reachable(Stations.Altrincham.getId()+ RouteCodesForTesting.ALTY_TO_PICC,
                Stations.Deansgate.getId()));
        assertTrue(repo.reachable(Stations.StPetersSquare.getId()+ RouteCodesForTesting.ALTY_TO_BURY,
                Stations.Deansgate.getId()));
        assertTrue(repo.reachable(Stations.StPetersSquare.getId()+ RouteCodesForTesting.ALTY_TO_PICC,
                Stations.Deansgate.getId()));
    }

}
