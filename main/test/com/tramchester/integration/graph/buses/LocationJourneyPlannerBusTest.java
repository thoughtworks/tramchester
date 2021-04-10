package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.BusTest;
import com.tramchester.testSupport.LocationJourneyPlannerTestFacade;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.tramchester.testSupport.TestEnv.nearAltrinchamInterchange;
import static com.tramchester.testSupport.reference.BusStations.AltrinchamInterchange;
import static com.tramchester.testSupport.reference.BusStations.StockportBusStation;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class LocationJourneyPlannerBusTest {
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static TramchesterConfig testConfig;
    private int maxDuration;

    private final LocalDate nextMonday = TestEnv.nextMonday();
    private Transaction txn;
    private LocationJourneyPlannerTestFacade planner;
    private int maxWalkingConnections = 3;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder<>().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        database = componentContainer.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        planner = new LocationJourneyPlannerTestFacade(componentContainer.get(LocationJourneyPlanner.class), stationRepository, txn);
        maxDuration = testConfig.getMaxJourneyDuration();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @BusTest
    @Test
    void shouldHaveSimpleWalkAndBus() {
        TramTime travelTime = TramTime.of(8, 0);

        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(nextMonday), travelTime, false, 3,
                maxDuration);

        Set<Journey> results = planner.quickestRouteForLocation(nearAltrinchamInterchange, StockportBusStation, journeyRequest, 10);

        assertFalse(results.isEmpty());
    }

    @BusTest
    @Test
    void shouldHaveSimpleBusAndWalk() {
        TramTime travelTime = TramTime.of(8, 0);

        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(nextMonday), travelTime, false, 3,
                maxDuration);

        Set<Journey> results = planner.quickestRouteForLocation(StockportBusStation, nearAltrinchamInterchange, journeyRequest, 10);

        assertFalse(results.isEmpty());
    }

    @BusTest
    @Test
    void shouldFindAltyToKnutford() {
        TramTime travelTime = TramTime.of(10, 30);

        JourneyRequest request = new JourneyRequest(new TramServiceDate(nextMonday), travelTime, false, 3,
                maxDuration);
        Set<Journey> journeys =  planner.quickestRouteForLocation(AltrinchamInterchange, TestEnv.nearKnutsfordBusStation, request, 10);

        assertFalse(journeys.isEmpty());
    }



}
