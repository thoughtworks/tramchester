package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.BusTest;
import com.tramchester.testSupport.RouteCodesForTesting;
import com.tramchester.testSupport.Stations;
import com.tramchester.repository.ReachabilityRepository;
import com.tramchester.testSupport.TestConfig;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;

import static com.tramchester.testSupport.BusStations.ALTRINCHAM_INTERCHANGE;
import static com.tramchester.testSupport.BusStations.STOCKPORT_BUSSTATION;
import static com.tramchester.testSupport.RouteCodesForTesting.ALTY_TO_STOCKPORT;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assume.assumeTrue;

@RunWith(Parameterized.class)
public class ReachabilityRepositoryTest {
    private static TramchesterConfig config;
    private ReachabilityRepository repo;
    private TransportData transportData;

    @Parameterized.Parameters
    public static Iterable<? extends Object> data() throws IOException {
        return TestConfig.getDependencies();
    }

    @Parameterized.Parameter
    public Dependencies dependencies;

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        TestConfig.closeDependencies();
    }

    @Before
    public void beforeEachTestRuns() {
        repo = dependencies.get(ReachabilityRepository.class);
        transportData = dependencies.get(TransportData.class);
        config = dependencies.getConfig();
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

    @Category({BusTest.class})
    @Test
    public void shouldHaveRoutebasedReachability() {
        assumeTrue(config.getBus());

        assertTrue(repo.stationReachable(ALTRINCHAM_INTERCHANGE+ALTY_TO_STOCKPORT, transportData.getStation(STOCKPORT_BUSSTATION)));
        assertTrue(repo.stationReachable(STOCKPORT_BUSSTATION+ALTY_TO_STOCKPORT, transportData.getStation(ALTRINCHAM_INTERCHANGE)));

        //assertFalse(repository.reachable(BusStations.SHUDEHILL_INTERCHANGE +routeCode, ALTRINCHAM_INTERCHANGE));

    }

}
