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
import org.junit.*;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.tramchester.testSupport.BusStations.*;
import static org.junit.Assert.assertFalse;

@Ignore("experimental")
public class BusRouteCalculatorTest {
    // TODO this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static Dependencies dependencies;
    private static GraphDatabase database;

    private RouteCalculator calculator;
    private LocalDate nextTuesday = TestEnv.nextTuesday(0);
    private Transaction tx;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        TramchesterConfig testConfig = new IntegrationBusTestConfig();
        dependencies.initialise(testConfig);
        database = dependencies.get(GraphDatabase.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Before
    public void beforeEachTestRuns() {
        tx = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        calculator = dependencies.get(RouteCalculator.class);
    }

    @After
    public void afterEachTestRuns() {
        tx.close();
    }

    @Test
    public void shouldHaveAltyToStockJourney() {
        RouteCalculatorTest.validateAtLeastOneJourney(calculator, AltrinchamInterchange, StockportBusStation,
                TramTime.of(8, 0), nextTuesday, 5);
    }

    @Test
    public void shouldHaveShudehillToStockJourney() {
        int maxChanges = 1;
        Set<Journey> journeys = RouteCalculatorTest.validateAtLeastOneJourney(calculator, ShudehillInterchange, StockportBusStation,
                TramTime.of(8, 0), nextTuesday, maxChanges);
        Journey journey = journeys.toArray(new Journey[1])[0];
        assertFalse(journey.getStages().size()>(maxChanges+1));
    }

    @Test
    public void shouldHaveSimpleTramJourney() {
        RouteCalculatorTest.validateAtLeastOneJourney(calculator, Stations.Altrincham, Stations.Cornbrook,
                TramTime.of(8, 0), nextTuesday, 5);
    }
}
