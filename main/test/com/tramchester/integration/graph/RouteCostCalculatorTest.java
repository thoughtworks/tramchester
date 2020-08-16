package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.Stations;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;

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
    void shouldComputeSimpleCostBetweenStations() {

        try(Transaction txn = database.beginTx()) {
            assertEquals(5, routeCostCalc.getApproxCostBetween(txn, Stations.NavigationRoad, Stations.Altrincham));
            assertEquals(6, routeCostCalc.getApproxCostBetween(txn, Stations.Altrincham, Stations.NavigationRoad));

            assertEquals(62, routeCostCalc.getApproxCostBetween(txn, Stations.Bury, Stations.Altrincham));
            assertEquals(62, routeCostCalc.getApproxCostBetween(txn, Stations.Altrincham, Stations.Bury));

            assertEquals(61, routeCostCalc.getApproxCostBetween(txn, Stations.MediaCityUK, Stations.ManAirport));
            assertEquals(61, routeCostCalc.getApproxCostBetween(txn, Stations.ManAirport, Stations.MediaCityUK));
        }
    }

}
