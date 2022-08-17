package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.dates.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.LocationJourneyPlannerTestFacade;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations.Composites;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.tramchester.testSupport.reference.KnownLocations.nearAltrinchamInterchange;
import static com.tramchester.testSupport.reference.KnownLocations.nearKnutsfordBusStation;
import static org.junit.jupiter.api.Assertions.assertFalse;

@BusTest
class LocationJourneyPlannerBusTest {
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static TramchesterConfig testConfig;
    private Duration maxDuration;

    private final LocalDate nextMonday = TestEnv.nextMonday();
    private Transaction txn;
    private LocationJourneyPlannerTestFacade planner;
    private StationGroupsRepository stationGroupsRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
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
        stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);
        planner = new LocationJourneyPlannerTestFacade(componentContainer.get(LocationJourneyPlanner.class), stationRepository, txn);
        maxDuration = Duration.ofMinutes(testConfig.getMaxJourneyDuration());
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveSimpleWalkAndBus() {
        TramTime travelTime = TramTime.of(8, 0);

        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(nextMonday), travelTime, false, 3,
                maxDuration, 1, getRequestedModes());

        StationGroup end = stationGroupsRepository.findByName(Composites.StockportTempBusStation.getName());

        Set<Journey> results = planner.quickestRouteForLocation(nearAltrinchamInterchange, end, journeyRequest, 5);

        assertFalse(results.isEmpty());
    }

    private Set<TransportMode> getRequestedModes() {
        return Collections.emptySet();
    }

    @Test
    void shouldHaveSimpleBusAndWalk() {

        StationGroup stockportBusStation = stationGroupsRepository.findByName(Composites.StockportTempBusStation.getName());

        TramTime travelTime = TramTime.of(8, 0);

        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(nextMonday), travelTime, false, 3,
                maxDuration, 1, getRequestedModes());

        Set<Journey> results = planner.quickestRouteForLocation(stockportBusStation, nearAltrinchamInterchange,
                journeyRequest, 5);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldFindAltyToKnutford() {

        StationGroup alty = stationGroupsRepository.findByName(Composites.AltrinchamInterchange.getName());

        TramTime travelTime = TramTime.of(10, 30);

        JourneyRequest request = new JourneyRequest(new TramServiceDate(nextMonday), travelTime, false, 3,
                maxDuration, 1, getRequestedModes());
        Set<Journey> journeys =  planner.quickestRouteForLocation(alty, nearKnutsfordBusStation, request, 5);

        assertFalse(journeys.isEmpty());
    }



}
