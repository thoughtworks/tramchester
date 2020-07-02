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
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.BusStations.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//@Disabled("WIP")
class BusRouteCalculatorTest {
    // TODO this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static Dependencies dependencies;
    private static GraphDatabase database;

    private RouteCalculator calculator;
    private final LocalDate when = TestEnv.testDay();
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

    @Test
    void shouldHaveAltyToStockJourney() {
        TramTime travelTime = TramTime.of(10, 45);
        Set<Journey> journeys = RouteCalculatorTest.validateAtLeastNJourney(calculator, 10, txn, AltrinchamInterchange,
                StockportBusStation, travelTime, when, 2);
        // 2 changes means 3 stages or less
        journeys.forEach(journey -> assertTrue(journey.getStages().size()<=3, journey.getStages().toString()));

        Set<Journey> journeysMaxChanges = RouteCalculatorTest.validateAtLeastNJourney(calculator, 10, txn, AltrinchamInterchange,
                StockportBusStation, travelTime, when, 8);
        // algo seems to return very large number of changes even when 2 is possible??
        List<Journey> journeys2Stages = journeysMaxChanges.stream().filter(journey -> journey.getStages().size() <= 3).collect(Collectors.toList());
        assertFalse(journeys2Stages.isEmpty());
    }

    @Test
    void shouldHaveShudehillToStockJourney() {
        int maxChanges = 2;
        Set<Journey> journeys = RouteCalculatorTest.validateAtLeastNJourney(calculator, 3, txn,
                ShudehillInterchange, StockportBusStation,
                TramTime.of(8, 0), when, maxChanges);
        journeys.forEach(journey -> Assertions.assertTrue(journey.getStages().size()<=(maxChanges+1)));
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    void shouldHaveSimpleTramJourney() {
        RouteCalculatorTest.validateAtLeastNJourney(calculator, 1, txn, Stations.Altrincham, Stations.Cornbrook,
                TramTime.of(8, 0), when, 5);
    }
}
