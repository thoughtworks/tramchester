package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.GroupedStations;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.util.function.BiFunction;

import static com.tramchester.testSupport.reference.BusStations.*;
import static com.tramchester.testSupport.reference.BusStations.Composites.StockportTempBusStation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@BusTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class BusRouteCostCalculatorTest {
    private static ComponentContainer componentContainer;

    private Transaction txn;
    private RouteCostCalculator routeCost;
    private GroupedStations altrinchamInterchange;
    private GroupedStations stockportBusStation;
    private GroupedStations shudehillInterchange;
    private StationRepository stationRepository;

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
        StationGroupsRepository stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);

        GraphDatabase database = componentContainer.get(GraphDatabase.class);

        altrinchamInterchange = stationGroupsRepository.findByName(Composites.AltrinchamInterchange.getName());
        stockportBusStation = stationGroupsRepository.findByName(Composites.StockportTempBusStation.getName());
        shudehillInterchange = stationGroupsRepository.findByName("Shudehill Interchange");

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
        assertEquals(39, getCostBetween(average(), altrinchamInterchange, stockportBusStation));
        assertEquals(37, getCostBetween(average(), stockportBusStation, altrinchamInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForAltyStockportMax() {
        assertEquals(71, getCost(max(), StopAtAltrinchamInterchange, StockportNewbridgeLane));
        assertEquals(50, getCost(max(), StockportNewbridgeLane, StopAtAltrinchamInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForAltyStockport() {
        assertEquals(61, getCost(average(), StopAtAltrinchamInterchange, StockportNewbridgeLane));
        assertEquals(45, getCost(average(), StockportNewbridgeLane, StopAtAltrinchamInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForShudehillAltyComp() {
        assertEquals(54, getCostBetween(average(), altrinchamInterchange, shudehillInterchange));
        assertEquals(53, getCostBetween(average(), shudehillInterchange, altrinchamInterchange));
    }

    @Test
    void shouldFindMaxCostsCorrectlyForShudehillAlty() {
        assertEquals(58, getCost(max(), StopAtAltrinchamInterchange, StopAtShudehillInterchange));
        assertEquals(59, getCost(max(), StopAtShudehillInterchange, StopAtAltrinchamInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForShudehillAlty() {
        assertEquals(56, getCost(average(), StopAtAltrinchamInterchange, StopAtShudehillInterchange));
        assertEquals(54, getCost(average(), StopAtShudehillInterchange, StopAtAltrinchamInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForShudehillStockportComp() {
        assertEquals(47, getCostBetween(average(), shudehillInterchange, stockportBusStation));
        assertEquals(38, getCostBetween(average(), stockportBusStation, shudehillInterchange));
    }

    @Test
    void shouldFindMaxCostsCorrectlyForShudehillStockportComp() {
        assertEquals(51, getCostBetween(max(), shudehillInterchange, stockportBusStation));
        assertEquals(42, getCostBetween(max(), stockportBusStation, shudehillInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForShudehillStockport() {
        assertEquals(65, getCost(average(), StopAtShudehillInterchange, StockportNewbridgeLane));
        assertEquals(42, getCost(average(), StockportNewbridgeLane, StopAtShudehillInterchange));
    }

    private BiFunction<Station, Station, Integer> average() {
        return (start, finish) -> routeCost.getAverageCostBetween(txn, start, finish, date);
    }

    private BiFunction<Station, Station, Integer> max() {
        return (start, finish) -> routeCost.getMaxCostBetween(txn, start, finish, date);
    }

    private int getCost(BiFunction<Station, Station, Integer> function, BusStations start, BusStations end) {
        return getCostBetween(function, start.from(stationRepository), end.from(stationRepository));
    }

    private int getCostBetween(BiFunction<Station, Station, Integer> function, Station start, Station end) {
        return function.apply(start, end);
    }


}
