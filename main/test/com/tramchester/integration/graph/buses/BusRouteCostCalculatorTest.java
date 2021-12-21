package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
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
import static com.tramchester.testSupport.reference.BusStations.Composites.StockportTempBusStation;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class BusRouteCostCalculatorTest {
    public static final int SHUDEHILL_TO_ALTY = 57;
    public static final int ALTY_TO_STOCKPORT = 51;
    public static final int SHUDEHILL_TO_STOCKPORT = 49;
    private static ComponentContainer componentContainer;

    private Transaction txn;
    private RouteCostCalculator routeCost;
    private CompositeStation altrinchamInterchange;
    private CompositeStation stockportBusStation;
    private CompositeStation shudehillInterchange;
    private CompositeStationRepository stationRepository;

    private final TramServiceDate date = new TramServiceDate(TestEnv.testDay());

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

        altrinchamInterchange = stationRepository.findByName(Composites.AltrinchamInterchange.getName());
        stockportBusStation = stationRepository.findByName(StockportTempBusStation.getName());
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
        assertNotNull(StockportTempBusStation);
        assertNotNull(shudehillInterchange);
    }

    @Test
    void shouldFindCostsCorrectlyForAltyStockportComp() {
        assertEquals(43, getApproxCostBetween(altrinchamInterchange, stockportBusStation));
        assertEquals(38, getApproxCostBetween(stockportBusStation, altrinchamInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForAltyStockport() {
        assertEquals(ALTY_TO_STOCKPORT, getApproxCostBetween(StopAtAltrinchamInterchange, StockportNewbridgeLane));
        assertEquals(46, getApproxCostBetween(StockportNewbridgeLane, StopAtAltrinchamInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForShudehillAltyComp() {
        assertEquals(53, getApproxCostBetween(altrinchamInterchange, shudehillInterchange));
        assertEquals(SHUDEHILL_TO_ALTY, getApproxCostBetween(shudehillInterchange, altrinchamInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForShudehillAlty() {
        assertEquals(53, getApproxCostBetween(StopAtAltrinchamInterchange, ShudehillInterchange));
        assertEquals(59, getApproxCostBetween(ShudehillInterchange, StopAtAltrinchamInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForShudehillStockportComp() {
        assertEquals(40, getApproxCostBetween(shudehillInterchange, stockportBusStation));
        assertEquals(42, getApproxCostBetween(stockportBusStation, shudehillInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForShudehillStockport() {
        assertEquals(SHUDEHILL_TO_STOCKPORT, getApproxCostBetween(ShudehillInterchange, StockportNewbridgeLane));
        assertEquals(47, getApproxCostBetween(StockportNewbridgeLane, ShudehillInterchange));
    }

    private int getApproxCostBetween(BusStations start, BusStations end) {
        return routeCost.getAverageCostBetween(txn,
                TestStation.real(stationRepository,start), TestStation.real(stationRepository,end), date);
    }

    private int getApproxCostBetween(Station start, Station end) {
        return routeCost.getAverageCostBetween(txn, start, end, date);
    }

}
