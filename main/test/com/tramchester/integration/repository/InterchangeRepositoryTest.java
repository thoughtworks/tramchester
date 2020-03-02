package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.domain.Route;
import com.tramchester.domain.Station;
import com.tramchester.testSupport.BusStations;
import com.tramchester.testSupport.BusTest;
import com.tramchester.integration.IntegrationBusTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.TransportDataFromFiles;
import com.tramchester.repository.TransportDataSource;
import com.tramchester.testSupport.RouteCodesForTesting;
import org.junit.*;
import org.junit.experimental.categories.Category;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Ignore("Experimental")
public class InterchangeRepositoryTest {
    private static Dependencies dependencies;
    private static IntegrationBusTestConfig config;
    private InterchangeRepository repository;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        config = new IntegrationBusTestConfig("int_test_bus_tramchester.db");
        dependencies.initialise(config);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Before
    public void onceBeforeEachTestRuns() {
        repository = dependencies.get(InterchangeRepository.class);
    }

    @Category({BusTest.class})
    @Test
    public void shouldFindBusInterchanges() {

        List<Station> interchanges = repository.getBusInterchanges();

        for (Station interchange : interchanges) {
            assertFalse(interchange.isTram());
        }

        assertFalse(interchanges.isEmpty());
        Set<String> interchangeIds = interchanges.stream().map(Station::getId).collect(Collectors.toSet());
//        assertTrue(interchangeIds.contains(BusStations.STOCKPORT_BUSSTATION));
        assertTrue(interchangeIds.contains(BusStations.ALTRINCHAM_INTERCHANGE));
    }

    @Category({BusTest.class})
    @Test
    public void shouldFindRoutesLinkedToDestViaInterchange() {
        Set<Route> routes = repository.findRoutesViaInterchangeFor(BusStations.STOCKPORT_BUSSTATION);

        Set<String> routeIds = routes.stream().map(Route::getId).collect(Collectors.toSet());
        assertFalse(routeIds.isEmpty());
        assertTrue(routeIds.contains(RouteCodesForTesting.ALTY_TO_STOCKPORT));
    }

}
