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
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.BusStations.*;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

@Disabled("WIP")
class BusRouteReachableTest {
    private static Dependencies dependencies;

    private RouteReachable reachable;
    private StationRepository stationRepository;
    private Transaction tx;
    private TransportData transportData;

    @BeforeAll
    static void onceBeforeAnyTestRuns() throws IOException {
        dependencies = new Dependencies();
        TramchesterConfig config = new IntegrationBusTestConfig();
        dependencies.initialise(config);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationRepository = dependencies.get(StationRepository.class);
        reachable = dependencies.get(RouteReachable.class);
        GraphDatabase database = dependencies.get(GraphDatabase.class);
        transportData = dependencies.get(TransportData.class);
        tx = database.beginTx();
    }

    @AfterEach
    void afterEachTestRuns() {
        tx.close();
    }

    @Category({BusTest.class})
    @Test
    void shouldFindCostsCorrectlyForBusJourneys() {
        Assertions.assertEquals(37, reachable.getApproxCostBetween(AltrinchamInterchange.getId(), StockportBusStation.getId()));
        Assertions.assertEquals(37, reachable.getApproxCostBetween(StockportBusStation.getId(), AltrinchamInterchange.getId()));
        Assertions.assertEquals(52, reachable.getApproxCostBetween(ShudehillInterchange.getId(), AltrinchamInterchange.getId()));
        Assertions.assertEquals(57, reachable.getApproxCostBetween(AltrinchamInterchange.getId(), ShudehillInterchange.getId()));
        Assertions.assertEquals(44, reachable.getApproxCostBetween(ShudehillInterchange.getId(), StockportBusStation.getId()));
        Assertions.assertEquals(37, reachable.getApproxCostBetween(StockportBusStation.getId(), ShudehillInterchange.getId()));
    }

    @Category({BusTest.class})
    @Test
    void shouldHaveRoutesBetweenBusStations() {
        Assertions.assertTrue(reachable.getRouteReachableWithInterchange(AltrinchamInterchange.getId(), StockportBusStation.getId(),
                RoutesForTesting.ALTY_TO_STOCKPORT));
        Assertions.assertTrue(reachable.getRouteReachableWithInterchange(AltrinchamInterchange.getId(), ShudehillInterchange.getId(),
                RoutesForTesting.ALTY_TO_STOCKPORT));
    }

    @Category({BusTest.class})
    @Test
    void shouldListRoutesBetweenBusStations() {
        Map<String, String> stepsSeen = reachable.getShortestRoutesBetween(AltrinchamInterchange.getId(), StockportBusStation.getId());

        Map<Station, Route> steps = stepsSeen.entrySet().stream().
                collect(Collectors.toMap(entry -> transportData.getStation(entry.getKey()), entry -> transportData.getRoute(entry.getValue())));

        Assertions.assertEquals(4, stepsSeen.size());
//        assertTrue(routesSeenAltToStockportId.containsValue(RouteCodesForTesting.ALTY_TO_STOCKPORT));
//
//        Map<String, String> routesSeenStockToShudehill = reachable.getShortestRoutesBetween(STOCKPORT_BUSSTATION, SHUDEHILL_INTERCHANGE);
//        assertEquals(8, routesSeenStockToShudehill.size());
    }

    private Station getReal(Station testStation) {
        return stationRepository.getStation(testStation.getId());
    }
}
