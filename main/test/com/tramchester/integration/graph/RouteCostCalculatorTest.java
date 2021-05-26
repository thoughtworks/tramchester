package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RouteCostCalculatorTest {

    private static ComponentContainer componentContainer;

    private RouteCostCalculator routeCostCalc;
    private Transaction txn;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        routeCostCalc = componentContainer.get(RouteCostCalculator.class);
        GraphDatabase database = componentContainer.get(GraphDatabase.class);
        txn = database.beginTx();
    }

    @AfterEach
    void afterEachTestHasRun() {
        txn.close();
    }

    @Test
    void shouldComputeSimpleCostBetweenStationsAltyNavRoad() {
        assertEquals(5, getApproxCostBetween(txn, TramStations.NavigationRoad, TramStations.Altrincham));
        assertEquals(6, getApproxCostBetween(txn, TramStations.Altrincham, TramStations.NavigationRoad));

    }

    @Test
    void shouldComputeSimpleCostBetweenStationsAltyBury() {
        assertEquals(64, getApproxCostBetween(txn, TramStations.Bury, TramStations.Altrincham));
        assertEquals(65, getApproxCostBetween(txn, TramStations.Altrincham, TramStations.Bury));
    }

    @Test
    void shouldComputeSimpleCostBetweenStationsMediaCityAirport() {
        assertEquals(61, getApproxCostBetween(txn, TramStations.MediaCityUK, TramStations.ManAirport));
        assertEquals(61, getApproxCostBetween(txn, TramStations.ManAirport, TramStations.MediaCityUK));
    }

    private int getApproxCostBetween(Transaction txn, TramStations start, TramStations dest) {
        return routeCostCalc.getApproxCostBetween(txn, TramStations.of(start), TramStations.of(dest));
    }

}
