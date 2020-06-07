package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.TramReachabilityRepository;
import com.tramchester.testSupport.RoutesForTesting;
import com.tramchester.testSupport.Stations;
import org.junit.jupiter.api.*;

import java.io.IOException;

class TramReachabilityRepositoryTest {
    private TramReachabilityRepository repository;
    private static Dependencies dependencies;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        repository = dependencies.get(TramReachabilityRepository.class);
    }

    @Test
    void shouldCreateReachabilityMatrix() {

        // wrong direction
        Assertions.assertFalse(repository.stationReachable(Stations.NavigationRoad, RoutesForTesting.PICC_TO_ALTY, Stations.TraffordBar));
        // right direction
        Assertions.assertTrue(repository.stationReachable(Stations.NavigationRoad, RoutesForTesting.ALTY_TO_PICC, Stations.TraffordBar));
        // wrong direction
        Assertions.assertFalse(repository.stationReachable(Stations.NavigationRoad, RoutesForTesting.PICC_TO_ALTY, Stations.ManAirport));
        // right direction with interchange
        Assertions.assertTrue(repository.stationReachable(Stations.NavigationRoad, RoutesForTesting.ALTY_TO_PICC, Stations.ManAirport));
        // self reachable
        Assertions.assertTrue(repository.stationReachable(Stations.NavigationRoad, RoutesForTesting.ALTY_TO_PICC, Stations.NavigationRoad));

        // right direction
        Assertions.assertTrue(repository.stationReachable(Stations.RochdaleRail, RoutesForTesting.ROCH_TO_DIDS, Stations.Monsall));
        // wrong direction
        Assertions.assertFalse(repository.stationReachable(Stations.RochdaleRail, RoutesForTesting.DIDS_TO_ROCH, Stations.Monsall));
        // towards victoria, so find an interchange
        Assertions.assertTrue(repository.stationReachable(Stations.Monsall, RoutesForTesting.ROCH_TO_DIDS, Stations.RochdaleRail));
    }

    // TODO Lockdown
    @Test
    @Disabled("Not during lockdown")
    void shouldRepoIssueAltyToDeangates() {
        Assertions.assertTrue(repository.stationReachable(Stations.Altrincham, RoutesForTesting.ALTY_TO_BURY, Stations.Deansgate));
        Assertions.assertTrue(repository.stationReachable(Stations.Altrincham, RoutesForTesting.ALTY_TO_PICC, Stations.Deansgate));
        Assertions.assertTrue(repository.stationReachable(Stations.StPetersSquare, RoutesForTesting.ALTY_TO_BURY, Stations.Deansgate));
        Assertions.assertTrue(repository.stationReachable(Stations.StPetersSquare, RoutesForTesting.ALTY_TO_PICC, Stations.Deansgate));
    }


}
