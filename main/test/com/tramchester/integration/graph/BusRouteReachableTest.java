package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteReachable;
import com.tramchester.integration.IntegrationBusTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.BusTest;
import com.tramchester.testSupport.RoutesForTesting;
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
        assertEquals(37, reachable.getApproxCostBetween(AltrinchamInterchange.getId(), StockportBusStation.getId()));
        assertEquals(37, reachable.getApproxCostBetween(StockportBusStation.getId(), AltrinchamInterchange.getId()));
        assertEquals(52, reachable.getApproxCostBetween(ShudehillInterchange.getId(), AltrinchamInterchange.getId()));
        assertEquals(57, reachable.getApproxCostBetween(AltrinchamInterchange.getId(), ShudehillInterchange.getId()));
        assertEquals(44, reachable.getApproxCostBetween(ShudehillInterchange.getId(), StockportBusStation.getId()));
        assertEquals(37, reachable.getApproxCostBetween(StockportBusStation.getId(), ShudehillInterchange.getId()));
    }

    @Category({BusTest.class})
    @Test
    public void shouldHaveRoutesBetweenBusStations() {
        assertTrue(reachable.getRouteReachableWithInterchange(AltrinchamInterchange.getId(), StockportBusStation.getId(),
                RoutesForTesting.ALTY_TO_STOCKPORT));
        assertTrue(reachable.getRouteReachableWithInterchange(AltrinchamInterchange.getId(), ShudehillInterchange.getId(),
                RoutesForTesting.ALTY_TO_STOCKPORT));
    }

    @Category({BusTest.class})
    @Test
    public void shouldListRoutesBetweenBusStations() {
        Map<String, String> stepsSeen = reachable.getShortestRoutesBetween(AltrinchamInterchange.getId(), StockportBusStation.getId());

        Map<Station, Route> steps = stepsSeen.entrySet().stream().
                collect(Collectors.toMap(entry -> transportData.getStation(entry.getKey()), entry -> transportData.getRoute(entry.getValue())));

        assertEquals(4, stepsSeen.size());
//        assertTrue(routesSeenAltToStockportId.containsValue(RouteCodesForTesting.ALTY_TO_STOCKPORT));
//
//        Map<String, String> routesSeenStockToShudehill = reachable.getShortestRoutesBetween(STOCKPORT_BUSSTATION, SHUDEHILL_INTERCHANGE);
//        assertEquals(8, routesSeenStockToShudehill.size());
    }

    private Station getReal(Station testStation) {
        return stationRepository.getStation(testStation.getId());
    }
}
