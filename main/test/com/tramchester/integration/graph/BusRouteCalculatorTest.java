package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.IntegrationBusTestConfig;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.tramchester.testSupport.BusStations.*;

@Disabled("experimental")
class BusRouteCalculatorTest {
    // TODO this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static Dependencies dependencies;
    private static GraphDatabase database;

    private RouteCalculator calculator;
    private final LocalDate nextTuesday = TestEnv.nextTuesday(0);
    private Transaction txn;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        TramchesterConfig testConfig = new IntegrationBusTestConfig();
        dependencies.initialise(testConfig);
        database = dependencies.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        calculator = dependencies.get(RouteCalculator.class);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    void shouldHaveAltyToStockJourney() {
        RouteCalculatorTest.validateAtLeastOneJourney(calculator, txn, AltrinchamInterchange, StockportBusStation,
                TramTime.of(8, 0), nextTuesday, 5);
    }

    @Test
    void shouldHaveShudehillToStockJourney() {
        int maxChanges = 1;
        Set<Journey> journeys = RouteCalculatorTest.validateAtLeastOneJourney(calculator, txn, ShudehillInterchange, StockportBusStation,
                TramTime.of(8, 0), nextTuesday, maxChanges);
        Journey journey = journeys.toArray(new Journey[1])[0];
        Assertions.assertFalse(journey.getStages().size()>(maxChanges+1));
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    void shouldHaveSimpleTramJourney() {
        RouteCalculatorTest.validateAtLeastOneJourney(calculator, txn, Stations.Altrincham, Stations.Cornbrook,
                TramTime.of(8, 0), nextTuesday, 5);
    }
}
