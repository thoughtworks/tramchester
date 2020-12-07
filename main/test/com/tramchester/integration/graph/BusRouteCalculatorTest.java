package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.domain.Journey;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.IntegrationBusTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.RouteCalculatorTestFacade;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.BusStations.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class BusRouteCalculatorTest {
    // TODO this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static Dependencies dependencies;
    private static GraphDatabase database;
    private static IntegrationBusTestConfig testConfig;
    private RouteCalculatorTestFacade calculator;

    private final LocalDate when = TestEnv.testDay();
    private Transaction txn;
    private int maxJourneyDuration;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        dependencies = new Dependencies();
        testConfig = new IntegrationBusTestConfig();
        dependencies.initialise(testConfig);
        database = dependencies.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        maxJourneyDuration = testConfig.getMaxJourneyDuration();
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        StationRepository stationRepository = dependencies.get(StationRepository.class);
        calculator = new RouteCalculatorTestFacade(dependencies.get(RouteCalculator.class), stationRepository, txn);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveStockToALtyJourney() {
        TramTime travelTime = TramTime.of(8, 0);
        LocalDate nextMonday = TestEnv.nextMonday();

        JourneyRequest requestA = new JourneyRequest(new TramServiceDate(nextMonday), travelTime, false, 2,
                maxJourneyDuration);
        Set<Journey> journeys = calculator.calculateRouteAsSet(StockportBusStation, AltrinchamInterchange, requestA, 3);
        assertFalse(journeys.isEmpty());

        // At least one direct
        List<Journey> direct = journeys.stream().filter(journey -> journey.getStages().size() == 1).collect(Collectors.toList());
        assertFalse(direct.isEmpty());

        JourneyRequest requestB = new JourneyRequest(new TramServiceDate(nextMonday), travelTime, false, 8,
                maxJourneyDuration);
        Set<Journey> journeysMaxChanges = calculator.calculateRouteAsSet(AltrinchamInterchange, StockportBusStation, requestB, 3);

        // algo seems to return very large number of changes even when 2 is possible??
        List<Journey> journeys2Stages = journeysMaxChanges.stream().filter(journey -> journey.getStages().size() <= 3).collect(Collectors.toList());
        assertFalse(journeys2Stages.isEmpty());
    }

    @Test
    void shouldFindAltyToKnutfordAtExpectedTime() {
        TramTime travelTime = TramTime.of(9, 55);

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 8,
                maxJourneyDuration);
        Set<Journey> journeys =  calculator.calculateRouteAsSet(AltrinchamInterchange, KnutsfordStationStand3, request);

        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldNotRevisitSameBusStationAltyToKnutsford() {
        TramTime travelTime = TramTime.of(15, 25);

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 8,
                maxJourneyDuration);
        Set<Journey> journeys = calculator.calculateRouteAsSet(AltrinchamInterchange, KnutsfordStationStand3, request);

        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            List<String> seenId = new ArrayList<>();
            journey.getStages().forEach(stage -> {
                String actionStation = stage.getActionStation().forDTO();
                assertFalse(seenId.contains(actionStation), "Already seen " + actionStation + " for " + journey.toString());
                seenId.add(actionStation);
            });
        });

    }

    @Test
    void shouldHavePiccadilyToStockportJourney() {
        int maxChanges = 2;
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(when), TramTime.of(8, 0), false, maxChanges,
                maxJourneyDuration);
        Set<Journey> journeys = calculator.calculateRouteAsSet(PiccadilyStationStopA, StockportBusStation, journeyRequest, 3);
        assertFalse(journeys.isEmpty());
        List<Journey> threeStagesOrLess = journeys.stream().filter(journey -> journey.getStages().size() <= (maxChanges + 1)).collect(Collectors.toList());
        assertFalse(threeStagesOrLess.isEmpty());
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Disabled("not loading tram stations")
    @Test
    void shouldHaveSimpleTramJourney() {
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(when), TramTime.of(8, 0), false, 5,
                maxJourneyDuration);
        calculator.calculateRouteAsSet(TramStations.Altrincham, TramStations.Cornbrook, journeyRequest, 1);
    }
}
