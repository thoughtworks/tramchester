package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.BusTest;
import com.tramchester.integration.IntegrationBusTestConfig;
import com.tramchester.repository.ReachabilityRepository;
import org.junit.*;
import org.junit.experimental.categories.Category;

import java.io.IOException;

import static com.tramchester.testSupport.BusStations.ALTRINCHAM_INTERCHANGE;
import static com.tramchester.testSupport.BusStations.STOCKPORT_BUSSTATION;
import static junit.framework.TestCase.assertTrue;

@Ignore("Experimental")
public class ReachabilityRepositoryBusTest {
    private static Dependencies dependencies;
    private static TestConfig testConfig;
    private ReachabilityRepository repository;

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
        repository = dependencies.get(ReachabilityRepository.class);
    }

    @Category({BusTest.class})
    @Test
    public void shouldHaveRoutebasedReachability() {
        String route_code = "GMS: 11A:I:";
        assertTrue(repository.reachable(ALTRINCHAM_INTERCHANGE+route_code, STOCKPORT_BUSSTATION));
    }

}
