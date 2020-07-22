package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteReachable;
import com.tramchester.integration.IntegrationBusTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.BusTest;
import com.tramchester.testSupport.RoutesForTesting;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.BusStations.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class BusRouteReachableTest {
    private static Dependencies dependencies;

    private RouteReachable reachable;
    private Transaction txn;
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
        reachable = dependencies.get(RouteReachable.class);
        GraphDatabase database = dependencies.get(GraphDatabase.class);
        transportData = dependencies.get(TransportData.class);
        txn = database.beginTx();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @BusTest
    @Test
    void shouldFindCostsCorrectlyForBusJourneys() {
        Assertions.assertEquals(37, reachable.getApproxCostBetween(txn, AltrinchamInterchange, StockportBusStation));
        Assertions.assertEquals(37, reachable.getApproxCostBetween(txn, StockportBusStation, AltrinchamInterchange));
        Assertions.assertEquals(63, reachable.getApproxCostBetween(txn, ShudehillInterchange, AltrinchamInterchange));
        Assertions.assertEquals(58, reachable.getApproxCostBetween(txn, AltrinchamInterchange, ShudehillInterchange));
        Assertions.assertEquals(41, reachable.getApproxCostBetween(txn, ShudehillInterchange, StockportBusStation));
        Assertions.assertEquals(40, reachable.getApproxCostBetween(txn, StockportBusStation, ShudehillInterchange));
    }

    @BusTest
    @Test
    void shouldHaveRoutesBetweenBusStations() {
        Assertions.assertTrue(reachable.getRouteReachableWithInterchange(RoutesForTesting.ALTY_TO_STOCKPORT, AltrinchamInterchange,
                StockportBusStation));
        Assertions.assertTrue(reachable.getRouteReachableWithInterchange(RoutesForTesting.ALTY_TO_STOCKPORT, AltrinchamInterchange,
                ShudehillInterchange));
    }

    @BusTest
    @Test
    void shouldListRoutesBetweenBusStations() {
        Map<String, String> stepsSeen = reachable.getShortestRoutesBetween(AltrinchamInterchange, StockportBusStation);

//        Map<Station, Route> steps = stepsSeen.entrySet().stream().
//                collect(Collectors.toMap(entry -> transportData.getStationById(entry.getKey()), entry -> transportData.getRouteById(entry.getValue())));

        Assertions.assertEquals(4, stepsSeen.size());
//        assertTrue(routesSeenAltToStockportId.containsValue(RouteCodesForTesting.ALTY_TO_STOCKPORT));
//
//        Map<String, String> routesSeenStockToShudehill = reachable.getShortestRoutesBetween(STOCKPORT_BUSSTATION, SHUDEHILL_INTERCHANGE);
//        assertEquals(8, routesSeenStockToShudehill.size());
    }

}
