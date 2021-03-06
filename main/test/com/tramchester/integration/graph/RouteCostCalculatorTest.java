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

import static com.tramchester.testSupport.reference.TramStations.*;
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
        assertEquals(5, getApproxCostBetween(txn, TramStations.NavigationRoad, Altrincham));
        assertEquals(6, getApproxCostBetween(txn, Altrincham, TramStations.NavigationRoad));

    }

    @Test
    void shouldComputeCostsForMediaCityAshton() {
        assertEquals(57, getApproxCostBetween(txn, MediaCityUK, Ashton));
        assertEquals(54, getApproxCostBetween(txn, Ashton, MediaCityUK));
    }

    @Test
    void shouldComputeSimpleCostBetweenStationsAltyBury() {
        assertEquals(62, getApproxCostBetween(txn, TramStations.Bury, Altrincham));
        assertEquals(62, getApproxCostBetween(txn, Altrincham, TramStations.Bury));
    }

    @Test
    void shouldComputeSimpleCostBetweenStationsMediaCityAirport() {
        assertEquals(61, getApproxCostBetween(txn, TramStations.MediaCityUK, TramStations.ManAirport));
        assertEquals(61, getApproxCostBetween(txn, TramStations.ManAirport, TramStations.MediaCityUK));
    }

    private int getApproxCostBetween(Transaction txn, TramStations start, TramStations dest) {
        return routeCostCalc.getApproxCostBetween(txn, of(start), of(dest));
    }

}
