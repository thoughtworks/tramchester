package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.integration.IntegrationBusTestConfig;
import com.tramchester.testSupport.BusTest;
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
        Assertions.assertEquals(40, routeCost.getApproxCostBetween(txn, AltrinchamInterchange, StockportBusStation));
        Assertions.assertEquals(40, routeCost.getApproxCostBetween(txn, StockportBusStation, AltrinchamInterchange));

        Assertions.assertEquals(55, routeCost.getApproxCostBetween(txn, ShudehillInterchange, AltrinchamInterchange));
        Assertions.assertEquals(54, routeCost.getApproxCostBetween(txn, AltrinchamInterchange, ShudehillInterchange));

        Assertions.assertEquals(41, routeCost.getApproxCostBetween(txn, ShudehillInterchange, StockportBusStation));
        Assertions.assertEquals(42, routeCost.getApproxCostBetween(txn, StockportBusStation, ShudehillInterchange));
    }

}
