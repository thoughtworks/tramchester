package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.testSupport.TramStations;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RouteCostCalculatorTest {

    private static Dependencies dependencies;

    private GraphDatabase database;
    private RouteCostCalculator routeCostCalc;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        dependencies = new Dependencies();
        TramchesterConfig config = new IntegrationTramTestConfig();
        dependencies.initialise(config);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {

        routeCostCalc = dependencies.get(RouteCostCalculator.class);
        database = dependencies.get(GraphDatabase.class);
    }

    @Test
    void shouldComputeSimpleCostBetweenStationsAltyNavRoad() {

        try(Transaction txn = database.beginTx()) {
            assertEquals(5, getApproxCostBetween(txn, TramStations.NavigationRoad, TramStations.Altrincham));
            assertEquals(6, getApproxCostBetween(txn, TramStations.Altrincham, TramStations.NavigationRoad));
        }
    }

    @Test
    void shouldComputeSimpleCostBetweenStationsAltyBury() {

        try(Transaction txn = database.beginTx()) {
            assertEquals(64, getApproxCostBetween(txn, TramStations.Bury, TramStations.Altrincham));
            assertEquals(65, getApproxCostBetween(txn, TramStations.Altrincham, TramStations.Bury));
        }
    }

    @Test
    void shouldComputeSimpleCostBetweenStationsMediaCityAirport() {

        try(Transaction txn = database.beginTx()) {
            assertEquals(61, getApproxCostBetween(txn, TramStations.MediaCityUK, TramStations.ManAirport));
            assertEquals(61, getApproxCostBetween(txn, TramStations.ManAirport, TramStations.MediaCityUK));
        }
    }

    private int getApproxCostBetween(Transaction txn, TramStations start, TramStations dest) {
        return routeCostCalc.getApproxCostBetween(txn, TramStations.of(start), TramStations.of(dest));
    }

}
