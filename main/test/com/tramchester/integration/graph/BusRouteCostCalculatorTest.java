package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.integration.IntegrationBusTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.BusStations;
import com.tramchester.testSupport.BusTest;
import com.tramchester.testSupport.TestStation;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;

import static com.tramchester.testSupport.BusStations.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class BusRouteCostCalculatorTest {
    private static Dependencies dependencies;

    private Transaction txn;
    private RouteCostCalculator routeCost;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
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
        routeCost = dependencies.get(RouteCostCalculator.class);
        stationRepository = dependencies.get(StationRepository.class);
        GraphDatabase database = dependencies.get(GraphDatabase.class);
        txn = database.beginTx();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @BusTest
    @Test
    void shouldFindCostsCorrectlyForBusJourneys() {
        Assertions.assertEquals(41, getApproxCostBetween(AltrinchamInterchange, StockportBusStation));
        Assertions.assertEquals(44, getApproxCostBetween(StockportBusStation, AltrinchamInterchange));

        Assertions.assertEquals(61, getApproxCostBetween(ShudehillInterchange, AltrinchamInterchange));
        Assertions.assertEquals(59, getApproxCostBetween(AltrinchamInterchange, ShudehillInterchange));

        Assertions.assertEquals(48, getApproxCostBetween(ShudehillInterchange, StockportBusStation));
        Assertions.assertEquals(41, getApproxCostBetween(StockportBusStation, ShudehillInterchange));
    }

    private int getApproxCostBetween(BusStations start, BusStations end) {
        return routeCost.getApproxCostBetween(txn,
                TestStation.real(stationRepository,start), TestStation.real(stationRepository,end));
    }

}
