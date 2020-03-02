package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.integration.IntegrationBusTestConfig;
import com.tramchester.repository.ReachabilityRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.BusTest;
import com.tramchester.testSupport.TestConfig;
import org.junit.*;
import org.junit.experimental.categories.Category;

import java.io.IOException;

import static com.tramchester.testSupport.BusStations.ALTRINCHAM_INTERCHANGE;
import static com.tramchester.testSupport.BusStations.STOCKPORT_BUSSTATION;
import static com.tramchester.testSupport.RouteCodesForTesting.ALTY_TO_STOCKPORT;
import static junit.framework.TestCase.assertTrue;

@Ignore("Experimental")
public class ReachabilityRepositoryBusTest {
    private static Dependencies dependencies;
    private static TestConfig testConfig;
    private ReachabilityRepository repository;
    private TransportData transportData;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        testConfig = new IntegrationBusTestConfig("int_test_bus_tramchester.db");
        dependencies.initialise(testConfig);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Before
    public void beforeEachTestRuns() {
        transportData = dependencies.get(TransportData.class);
        repository = dependencies.get(ReachabilityRepository.class);
    }

    @Category({BusTest.class})
    @Test
    public void shouldHaveRoutebasedReachability() {

        assertTrue(repository.stationReachable(ALTRINCHAM_INTERCHANGE+ALTY_TO_STOCKPORT, transportData.getStation(STOCKPORT_BUSSTATION)));
        assertTrue(repository.stationReachable(STOCKPORT_BUSSTATION+ALTY_TO_STOCKPORT, transportData.getStation(ALTRINCHAM_INTERCHANGE)));
       //assertFalse(repository.reachable(BusStations.SHUDEHILL_INTERCHANGE +routeCode, ALTRINCHAM_INTERCHANGE));

    }

}
