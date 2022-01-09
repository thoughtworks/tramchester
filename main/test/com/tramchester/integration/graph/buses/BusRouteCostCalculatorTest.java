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

import java.util.function.BiFunction;

import static com.tramchester.testSupport.reference.BusStations.*;
import static com.tramchester.testSupport.reference.BusStations.Composites.StockportTempBusStation;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class BusRouteCostCalculatorTest {
    private static ComponentContainer componentContainer;

    private Transaction txn;
    private RouteCostCalculator routeCost;
    private CompositeStation altrinchamInterchange;
    private CompositeStation stockportBusStation;
    private CompositeStation shudehillInterchange;
    private CompositeStationRepository stationRepository;

    private final TramServiceDate date = new TramServiceDate(TestEnv.testDay());
    private CompositeStationRepository compositeStationRepository;

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
        compositeStationRepository = componentContainer.get(CompositeStationRepository.class);

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
        assertEquals(26, getCostBetween(average(), altrinchamInterchange, stockportBusStation));
        assertEquals(26, getCostBetween(average(), stockportBusStation, altrinchamInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForAltyStockportMax() {
        assertEquals(37, getCost(max(), StopAtAltrinchamInterchange, StockportNewbridgeLane));
        assertEquals(39, getCost(max(), StockportNewbridgeLane, StopAtAltrinchamInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForAltyStockport() {
        assertEquals(29, getCost(average(), StopAtAltrinchamInterchange, StockportNewbridgeLane));
        assertEquals(28, getCost(average(), StockportNewbridgeLane, StopAtAltrinchamInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForShudehillAltyComp() {
        assertEquals(36, getCostBetween(average(), altrinchamInterchange, shudehillInterchange));
        assertEquals(34, getCostBetween(average(), shudehillInterchange, altrinchamInterchange));
    }

    @Test
    void shouldFindMaxCostsCorrectlyForShudehillAlty() {
        assertEquals(39, getCost(max(), StopAtAltrinchamInterchange, ShudehillInterchange));
        assertEquals(44, getCost(max(), ShudehillInterchange, StopAtAltrinchamInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForShudehillAlty() {
        assertEquals(35, getCost(average(), StopAtAltrinchamInterchange, ShudehillInterchange));
        assertEquals(36, getCost(average(), ShudehillInterchange, StopAtAltrinchamInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForShudehillStockportComp() {
        assertEquals(31, getCostBetween(average(), shudehillInterchange, stockportBusStation));
        assertEquals(29, getCostBetween(average(), stockportBusStation, shudehillInterchange));
    }

    @Test
    void shouldFindMaxCostsCorrectlyForShudehillStockportComp() {
        assertEquals(39, getCostBetween(max(), shudehillInterchange, stockportBusStation));
        assertEquals(33, getCostBetween(max(), stockportBusStation, shudehillInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForShudehillStockport() {
        assertEquals(34, getCost(average(), ShudehillInterchange, StockportNewbridgeLane));
        assertEquals(28, getCost(average(), StockportNewbridgeLane, ShudehillInterchange));
    }

    private BiFunction<Station, Station, Integer> average() {
        return (start, finish) -> routeCost.getAverageCostBetween(txn, start, finish, date);
    }

    private BiFunction<Station, Station, Integer> max() {
        return (start, finish) -> routeCost.getMaxCostBetween(txn, start, finish, date);
    }

    private int getCost(BiFunction<Station, Station, Integer> function, BusStations start, BusStations end) {
        return getCostBetween(function, TestStation.real(stationRepository,start), TestStation.real(stationRepository,end));
    }

    private int getCostBetween(BiFunction<Station, Station, Integer> function, Station start, Station end) {
        return function.apply(start, end);
    }


}
