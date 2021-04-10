package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.graphbuild.CompositeStationGraphBuilder;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.testSupport.BusTest;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class BusRouteCostCalculatorCompositeStationsTest {
    private static ComponentContainer componentContainer;

    private Transaction txn;
    private RouteCostCalculator routeCost;
    private CompositeStation altrinchamInterchange;
    private CompositeStation stockportBusStation;
    private CompositeStation shudehillInterchange;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        CompositeStationRepository stationRepository = componentContainer.get(CompositeStationRepository.class);
        GraphDatabase database = componentContainer.get(GraphDatabase.class);

        altrinchamInterchange = stationRepository.findByName("Altrincham Interchange");
        stockportBusStation = stationRepository.findByName("Stockport Bus Station");
        shudehillInterchange = stationRepository.findByName("Shudehill Interchange");

        // force creation of db nodes
        componentContainer.get(CompositeStationGraphBuilder.class);
        routeCost = componentContainer.get(RouteCostCalculator.class);

        txn = database.beginTx();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @BusTest
    @Test
    void shouldHaveStations() {
        assertNotNull(altrinchamInterchange);
        assertNotNull(stockportBusStation);
        assertNotNull(shudehillInterchange);
    }

    @BusTest
    @Test
    void shouldFindCostsCorrectlyForAltyStockport() {
        Assertions.assertEquals(37, getApproxCostBetween(altrinchamInterchange, stockportBusStation));
        Assertions.assertEquals(39, getApproxCostBetween(stockportBusStation, altrinchamInterchange));
    }

    @BusTest
    @Test
    void shouldFindCostsCorrectlyForShudehillAlty() {
        Assertions.assertEquals(55, getApproxCostBetween(altrinchamInterchange, shudehillInterchange));
        Assertions.assertEquals(55, getApproxCostBetween(shudehillInterchange, altrinchamInterchange));
    }

    @BusTest
    @Test
    void shouldFindCostsCorrectlyForShudehillStockport() {
        Assertions.assertEquals(37, getApproxCostBetween(shudehillInterchange, stockportBusStation));
        Assertions.assertEquals(38, getApproxCostBetween(stockportBusStation, shudehillInterchange));
    }

    private int getApproxCostBetween(Station start, Station end) {
        return routeCost.getApproxCostBetween(txn, start, end);
    }

}
