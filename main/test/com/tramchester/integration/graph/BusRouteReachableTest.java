package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteReachable;
import com.tramchester.integration.IntegrationBusTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.BusTest;
import com.tramchester.testSupport.RouteCodesForTesting;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.BusStations.*;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

@Ignore("WIP")
public class BusRouteReachableTest {
    private static Dependencies dependencies;
    private static TramchesterConfig config;

    private RouteReachable reachable;
    private StationRepository stationRepository;
    private GraphDatabase database;
    private Transaction tx;
    private TransportData transportData;

    @BeforeClass
    public static void onceBeforeAnyTestRuns() throws IOException {
        dependencies = new Dependencies();
        config = new IntegrationBusTestConfig();
        dependencies.initialise(config);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Before
    public void beforeEachTestRuns() {
        stationRepository = dependencies.get(StationRepository.class);
        reachable = dependencies.get(RouteReachable.class);
        database = dependencies.get(GraphDatabase.class);
        transportData = dependencies.get(TransportData.class);
        tx = database.beginTx();
    }

    @After
    public void afterEachTestRuns() {
        tx.close();
    }

    @Category({BusTest.class})
    @Test
    public void shouldFindCostsCorrectlyForBusJourneys() {
        assertEquals(56, reachable.getApproxCostBetween(ALTRINCHAM_INTERCHANGE, STOCKPORT_BUSSTATION));
        assertEquals(51, reachable.getApproxCostBetween(STOCKPORT_BUSSTATION, ALTRINCHAM_INTERCHANGE));
        assertEquals(122, reachable.getApproxCostBetween(SHUDEHILL_INTERCHANGE, ALTRINCHAM_INTERCHANGE));
        assertEquals(99, reachable.getApproxCostBetween(ALTRINCHAM_INTERCHANGE, SHUDEHILL_INTERCHANGE));
        assertEquals(80, reachable.getApproxCostBetween(SHUDEHILL_INTERCHANGE, STOCKPORT_BUSSTATION));
        assertEquals(76, reachable.getApproxCostBetween(STOCKPORT_BUSSTATION, SHUDEHILL_INTERCHANGE));
    }

    @Category({BusTest.class})
    @Test
    public void shouldHaveRoutesBetweenBusStations() {
        assertTrue(reachable.getRouteReachableWithInterchange(ALTRINCHAM_INTERCHANGE, STOCKPORT_BUSSTATION, RouteCodesForTesting.ALTY_TO_STOCKPORT));
        assertTrue(reachable.getRouteReachableWithInterchange(ALTRINCHAM_INTERCHANGE, SHUDEHILL_INTERCHANGE, RouteCodesForTesting.ALTY_TO_STOCKPORT));
    }

    @Category({BusTest.class})
    @Test
    public void shouldListRoutesBetweenBusStations() {
        Map<String, String> stepsSeen = reachable.getShortestRoutesBetween(ALTRINCHAM_INTERCHANGE, STOCKPORT_BUSSTATION);

        Map<Station, Route> steps = stepsSeen.entrySet().stream().
                collect(Collectors.toMap(entry -> transportData.getStation(entry.getKey()), entry -> transportData.getRoute(entry.getValue())));

        assertEquals(3, stepsSeen.size());
//        assertTrue(routesSeenAltToStockportId.containsValue(RouteCodesForTesting.ALTY_TO_STOCKPORT));
//
//        Map<String, String> routesSeenStockToShudehill = reachable.getShortestRoutesBetween(STOCKPORT_BUSSTATION, SHUDEHILL_INTERCHANGE);
//        assertEquals(8, routesSeenStockToShudehill.size());
    }

    private Station getReal(Station testStation) {
        return stationRepository.getStation(testStation.getId());
    }
}
