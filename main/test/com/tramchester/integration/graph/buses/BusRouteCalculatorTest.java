package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Journey;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.BusTest;
import com.tramchester.integration.graph.testSupport.RouteCalculatorTestFacade;
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

    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static IntegrationBusTestConfig testConfig;
    private RouteCalculatorTestFacade calculator;

    private final LocalDate when = TestEnv.testDay();
    private Transaction txn;
    private int maxJourneyDuration;

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
        maxJourneyDuration = testConfig.getMaxJourneyDuration();
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @BusTest
    @Test
    void shouldHaveStockToAltyJourneyAndBackAgain() {
        TramTime travelTime = TramTime.of(8, 0);
        LocalDate nextMonday = TestEnv.nextMonday();

        JourneyRequest requestA = new JourneyRequest(new TramServiceDate(nextMonday), travelTime, false, 2,
                maxJourneyDuration);
        Set<Journey> journeys = calculator.calculateRouteAsSet(StopAtStockportBusStation, StopAtAltrinchamInterchange, requestA, 3);
        assertFalse(journeys.isEmpty());

        // At least one direct
        List<Journey> direct = journeys.stream().filter(journey -> journey.getStages().size() == 1).collect(Collectors.toList());
        assertFalse(direct.isEmpty());

        JourneyRequest requestB = new JourneyRequest(new TramServiceDate(nextMonday), travelTime, false, 8,
                maxJourneyDuration);
        Set<Journey> journeysMaxChanges = calculator.calculateRouteAsSet(StopAtAltrinchamInterchange, StopAtStockportBusStation, requestB, 3);

        // algo seems to return very large number of changes even when 2 is possible??
        List<Journey> journeys2Stages = journeysMaxChanges.stream().filter(journey -> journey.getStages().size() <= 3).collect(Collectors.toList());
        assertFalse(journeys2Stages.isEmpty());
    }

    @BusTest
    @Test
    void shouldFindAltyToKnutfordAtExpectedTime() {
        TramTime travelTime = TramTime.of(10, 20);

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 2,
                maxJourneyDuration);
        Set<Journey> journeys = calculator.calculateRouteAsSet(StopAtAltrinchamInterchange, KnutsfordStationStand3, request);

        assertFalse(journeys.isEmpty(), "no journeys");
    }

    @BusTest
    @Test
    void shouldNotRevisitSameBusStationAltyToKnutsford() {
        TramTime travelTime = TramTime.of(15, 55);

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 2,
                maxJourneyDuration);
        Set<Journey> journeys = calculator.calculateRouteAsSet(StopAtAltrinchamInterchange, KnutsfordStationStand3, request);

        assertFalse(journeys.isEmpty(), "no journeys");

        journeys.forEach(journey -> {
            List<String> seenId = new ArrayList<>();
            journey.getStages().forEach(stage -> {
                String actionStation = stage.getActionStation().forDTO();
                assertFalse(seenId.contains(actionStation), "Already seen " + actionStation + " for " + journey.toString());
                seenId.add(actionStation);
            });
        });
    }

    @BusTest
    @Test
    void shouldHavePiccadilyToStockportJourney() {
        int maxChanges = 2;
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(when), TramTime.of(8, 0), false, maxChanges,
                maxJourneyDuration);
        Set<Journey> journeys = calculator.calculateRouteAsSet(PiccadilyStationStopA, StopAtStockportBusStation, journeyRequest, 3);
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
