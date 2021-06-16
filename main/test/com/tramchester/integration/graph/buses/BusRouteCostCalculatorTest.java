package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.testSupport.testTags.BusTest;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestStation;
import com.tramchester.testSupport.reference.BusStations;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import static com.tramchester.testSupport.reference.BusStations.*;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class BusRouteCostCalculatorTest {
    public static final int SHUDEHILL_TO_ALTY = 54;
    public static final int ALTY_TO_STOCKPORT = 40;
    public static final int SHUDEHILL_TO_STOCKPORT = 41;
    private static ComponentContainer componentContainer;

    private Transaction txn;
    private RouteCostCalculator routeCost;
    private CompositeStation altrinchamInterchange;
    private CompositeStation stockportBusStation;
    private CompositeStation shudehillInterchange;
    private CompositeStationRepository stationRepository;

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
        stationRepository = componentContainer.get(CompositeStationRepository.class);
        GraphDatabase database = componentContainer.get(GraphDatabase.class);

        altrinchamInterchange = stationRepository.findByName("Altrincham Interchange");
        stockportBusStation = stationRepository.findByName("Stockport Bus Station");
        shudehillInterchange = stationRepository.findByName("Shudehill Interchange");

        routeCost = componentContainer.get(RouteCostCalculator.class);

        txn = database.beginTx();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveStations() {
        assertNotNull(altrinchamInterchange);
        assertNotNull(stockportBusStation);
        assertNotNull(shudehillInterchange);
    }

    @Test
    void shouldFindCostsCorrectlyForAltyStockportComp() {
        assertEquals(ALTY_TO_STOCKPORT, getApproxCostBetween(altrinchamInterchange, stockportBusStation));
        assertEquals(39, getApproxCostBetween(stockportBusStation, altrinchamInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForAltyStockport() {
        assertEquals(ALTY_TO_STOCKPORT, getApproxCostBetween(StopAtAltrinchamInterchange, StopAtStockportBusStation));
        assertEquals(41, getApproxCostBetween(StopAtStockportBusStation, StopAtAltrinchamInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForShudehillAltyComp() {
        assertEquals(54, getApproxCostBetween(altrinchamInterchange, shudehillInterchange));
        assertEquals(SHUDEHILL_TO_ALTY, getApproxCostBetween(shudehillInterchange, altrinchamInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForShudehillAlty() {
        assertEquals(56, getApproxCostBetween(StopAtAltrinchamInterchange, ShudehillInterchange));
        assertEquals(SHUDEHILL_TO_ALTY, getApproxCostBetween(ShudehillInterchange, StopAtAltrinchamInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForShudehillStockportComp() {
        assertEquals(SHUDEHILL_TO_STOCKPORT, getApproxCostBetween(shudehillInterchange, stockportBusStation));
        assertEquals(42, getApproxCostBetween(stockportBusStation, shudehillInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForShudehillStockport() {
        assertEquals(SHUDEHILL_TO_STOCKPORT, getApproxCostBetween(ShudehillInterchange, StopAtStockportBusStation));
        assertEquals(44, getApproxCostBetween(StopAtStockportBusStation, ShudehillInterchange));
    }

    private int getApproxCostBetween(BusStations start, BusStations end) {
        return routeCost.getApproxCostBetween(txn,
                TestStation.real(stationRepository,start), TestStation.real(stationRepository,end));
    }

    private int getApproxCostBetween(Station start, Station end) {
        return routeCost.getApproxCostBetween(txn, start, end);
    }

}
