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
        assertFalse(repo.stationReachable(Stations.NavigationRoad.getId()+ RouteCodesForTesting.BURY_TO_ALTY,
                Stations.TraffordBar));
        // right direction
        assertTrue(repo.stationReachable(Stations.NavigationRoad.getId()+ RouteCodesForTesting.ALTY_TO_BURY,
                Stations.TraffordBar));
        // wrong direction
        assertFalse(repo.stationReachable(Stations.NavigationRoad.getId()+ RouteCodesForTesting.BURY_TO_ALTY,
                Stations.ManAirport));
        // right direction with interchange
        assertTrue(repo.stationReachable(Stations.NavigationRoad.getId()+ RouteCodesForTesting.ALTY_TO_BURY,
                Stations.ManAirport));
        // self reachable
        assertTrue(repo.stationReachable(Stations.NavigationRoad.getId()+ RouteCodesForTesting.ALTY_TO_BURY,
                Stations.NavigationRoad));

        // right direction
        assertTrue(repo.stationReachable(Stations.RochdaleRail.getId() + RouteCodesForTesting.ROCH_TO_DIDS,
                Stations.Monsall));
        // wrong direction
        assertFalse(repo.stationReachable(Stations.RochdaleRail.getId() + RouteCodesForTesting.DIDS_TO_ROCH,
                Stations.Monsall));
        // towards victoria, so find an interchange
        assertTrue(repo.stationReachable(Stations.Monsall.getId() + RouteCodesForTesting.ROCH_TO_DIDS,
                Stations.RochdaleRail));

    }

    @Test
    public void shouldRepoIssueAltyToDeangates() {
        assertTrue(repo.stationReachable(Stations.Altrincham.getId()+ RouteCodesForTesting.ALTY_TO_BURY,
                Stations.Deansgate));
        assertTrue(repo.stationReachable(Stations.Altrincham.getId()+ RouteCodesForTesting.ALTY_TO_PICC,
                Stations.Deansgate));
        assertTrue(repo.stationReachable(Stations.StPetersSquare.getId()+ RouteCodesForTesting.ALTY_TO_BURY,
                Stations.Deansgate));
        assertTrue(repo.stationReachable(Stations.StPetersSquare.getId()+ RouteCodesForTesting.ALTY_TO_PICC,
                Stations.Deansgate));
    }

}
