package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Journey;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.graph.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.BusTest;
import org.jetbrains.annotations.NotNull;
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
import static org.junit.jupiter.api.Assertions.*;

@BusTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class BusRouteCalculatorTest {

    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static IntegrationBusTestConfig testConfig;

    private RouteCalculatorTestFacade calculator;
    private CompositeStationRepository compositeStationRepository;

    private final LocalDate when = TestEnv.testDay();
    private Transaction txn;
    private int maxJourneyDuration;

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
        maxJourneyDuration = testConfig.getMaxJourneyDuration();
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        compositeStationRepository = componentContainer.get(CompositeStationRepository.class);
        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveStockToAltyJourneyAndBackAgainNoChanges() {

        CompositeStation stockport = compositeStationRepository.findByName("Stockport Bus Station");
        CompositeStation alty = compositeStationRepository.findByName("Altrincham Interchange");

        TramTime travelTime = TramTime.of(9,0);
        LocalDate nextMonday = TestEnv.nextMonday();

        JourneyRequest requestA = new JourneyRequest(new TramServiceDate(nextMonday), travelTime, false, 0,
                maxJourneyDuration, 1);
        Set<Journey> journeysA = calculator.calculateRouteAsSet(stockport, alty, requestA);
        assertFalse(journeysA.isEmpty());

        JourneyRequest requestB = new JourneyRequest(new TramServiceDate(nextMonday), travelTime, false, 0,
                maxJourneyDuration, 1);
        Set<Journey> journeysB = calculator.calculateRouteAsSet(alty, stockport, requestB);
        assertFalse(journeysB.isEmpty());
    }

    @Test
    void shouldHaveStockToAltyJourney() {

        CompositeStation stockport = compositeStationRepository.findByName("Stockport Bus Station");
        CompositeStation alty = compositeStationRepository.findByName("Altrincham Interchange");

        TramTime travelTime = TramTime.of(9, 0);
        LocalDate nextMonday = TestEnv.nextMonday();

        JourneyRequest requestA = new JourneyRequest(new TramServiceDate(nextMonday), travelTime, false, 0,
                maxJourneyDuration, 3);
        Set<Journey> journeys = calculator.calculateRouteAsSet(stockport, alty, requestA);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveAltyToStockport() {
        CompositeStation stockport = compositeStationRepository.findByName("Stockport Bus Station");
        CompositeStation alty = compositeStationRepository.findByName("Altrincham Interchange");

        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(TestEnv.nextMonday()),
                TramTime.of(9, 0), false, 1, maxJourneyDuration, 3);

        Set<Journey> journeysMaxChanges = calculator.calculateRouteAsSet(alty, stockport, journeyRequest);

        // algo seems to return very large number of changes even when 2 is possible??
        List<Journey> journeys2Stages = journeysMaxChanges.stream().
                filter(journey -> countNonConnectStages(journey) <= 3).
                collect(Collectors.toList());
        assertFalse(journeys2Stages.isEmpty());
    }

    private long countNonConnectStages(Journey journey) {
        return journey.getStages().stream().
                filter(stage -> stage.getMode().getTransportMode() != TransportMode.Connect).count();
    }

    @Test
    void shouldFindJourneyInFutureCorrectly() {
        // attempt to repro seen in ui where zero journeys
        CompositeStation start = compositeStationRepository.findByName("Taylor Road, Oldfield Brow, Altrincham");
        CompositeStation end = compositeStationRepository.findByName("Altrincham Interchange");

        final LocalDate futureDate = TestEnv.testDay().plusDays(14);
        JourneyRequest journeyRequest = new JourneyRequest(futureDate,
                TramTime.of(8,19), false, 3, maxJourneyDuration, 1);

        @NotNull Set<Journey> results = calculator.calculateRouteAsSet(start, end, journeyRequest);
        assertFalse(results.isEmpty(), "no journeys");

    }

    @Test
    void shouldHaveJourneyAltyToKnutsford() {
        CompositeStation start = compositeStationRepository.findByName("Altrincham Interchange");
        CompositeStation end = compositeStationRepository.findByName("Bus Station, Knutsford");

        TramTime time = TramTime.of(10, 40);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 1,
                120, 1);
        Set<Journey> results = calculator.calculateRouteAsSet(start, end, journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldHandleJourneyDirectViaSingleComposite() {

        CompositeStation piccadillyComp = compositeStationRepository.findByName("Piccadilly Rail Station");
        List<Station> stations = new ArrayList<>(piccadillyComp.getContained());
        assertTrue(stations.size()>2);
        Station start = stations.get(0);
        Station end = stations.get(1);

        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(7,30), false, 1,
                120, 1);

        Set<Journey> results = calculator.calculateRouteAsSet(start, end, journeyRequest);

        assertFalse(results.isEmpty());
        results.forEach(journey -> assertFalse(journey.getStages().isEmpty()));
    }

    @Test
    void shouldHaveJourneyKnutsfordToAlty() {
        CompositeStation start = compositeStationRepository.findByName("Bus Station, Knutsford");
        CompositeStation end = compositeStationRepository.findByName("Altrincham Interchange");

        TramTime time = TramTime.of(11, 20);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false,
                3, 120, 1);

        Set<Journey> results = calculator.calculateRouteAsSet(start, end, journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldNotRevisitSameBusStationAltyToKnutsford() {
        CompositeStation start = compositeStationRepository.findByName("Altrincham Interchange");
        CompositeStation end = compositeStationRepository.findByName("Bus Station, Knutsford");

        TramTime travelTime = TramTime.of(15, 55);

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 2,
                maxJourneyDuration, 1);
        Set<Journey> journeys = calculator.calculateRouteAsSet(start, end, request);

        assertFalse(journeys.isEmpty(), "no journeys");

        journeys.forEach(journey -> {
            List<String> seenId = new ArrayList<>();
            journey.getStages().forEach(stage -> {
                String actionStation = stage.getActionStation().forDTO();
                assertFalse(seenId.contains(actionStation), "Already seen " + actionStation + " for " + journey);
                seenId.add(actionStation);
            });
        });
    }

    @Test
    void shouldHavePiccadilyToStockportJourney() {
        int maxChanges = 2;
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(when), TramTime.of(8, 0), false, maxChanges,
                maxJourneyDuration, 3);
        Set<Journey> journeys = calculator.calculateRouteAsSet(PiccadilyStationStopA, StopAtStockportBusStation, journeyRequest);
        assertFalse(journeys.isEmpty());
        List<Journey> threeStagesOrLess = journeys.stream().filter(journey -> journey.getStages().size() <= (maxChanges + 1)).collect(Collectors.toList());
        assertFalse(threeStagesOrLess.isEmpty());
    }

    @Test
    void shouldReproIssueWithSlowPerformanceShudehillToBroadheathAsda() {
        int maxChanges = 3;

        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(when), TramTime.of(9,40),
                false, maxChanges, maxJourneyDuration, 3);

        CompositeStation shudehill = compositeStationRepository.findByName("Shudehill Interchange");
        Station asdaBroadhealth = compositeStationRepository.getStationById(StringIdFor.createId("1800SJ18511"));
        assertNotNull(asdaBroadhealth);

        Set<Journey> journeys = calculator.calculateRouteAsSet(shudehill, asdaBroadhealth, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldReproPerfIssueStockportToShudehillInterchange() {

        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(when), TramTime.of(8,45),
                false, 3, maxJourneyDuration, 3);

        Set<Journey> results = calculator.calculateRouteAsSet(StopAtStockportBusStation, ShudehillInterchange, journeyRequest);
        assertFalse(results.isEmpty());
    }

    @Test
    void shouldReproPerfIssueAltyToStockport() {

        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(when), TramTime.of(8,45),
                false, 3, maxJourneyDuration, 3);

        Set<Journey> results = calculator.calculateRouteAsSet(StopAtAltrinchamInterchange, StopAtStockportBusStation, journeyRequest);
        assertFalse(results.isEmpty());
    }

}
