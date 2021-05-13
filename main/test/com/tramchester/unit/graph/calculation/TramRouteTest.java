package com.tramchester.unit.graph.calculation;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.Journey;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.WalkingStage;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.LocationJourneyPlannerTestFacade;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.nearWythenshaweHosp;
import static com.tramchester.testSupport.reference.TramTransportDataForTestFactory.TramTransportDataForTest.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TramRouteTest {

    private static ComponentContainer componentContainer;
    private static LocationJourneyPlannerTestFacade locationJourneyPlanner;
    private static SimpleGraphConfig config;

    private TramTransportDataForTestFactory.TramTransportDataForTest transportData;
    private RouteCalculator calculator;

    private TramServiceDate queryDate;
    private TramTime queryTime;
    private Transaction txn;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        config = new SimpleGraphConfig("tramroutetest.db");
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                overrideProvider(TramTransportDataForTestFactory.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void onceAfterAllTestsRun() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = (TramTransportDataForTestFactory.TramTransportDataForTest) componentContainer.get(TransportData.class);
        GraphDatabase database = componentContainer.get(GraphDatabase.class);
        calculator = componentContainer.get(RouteCalculator.class);

        queryDate = new TramServiceDate(LocalDate.of(2014,6,30));
        queryTime = TramTime.of(7, 57);
        StationRepository stationRepo = componentContainer.get(StationRepository.class);

        txn = database.beginTx();

        locationJourneyPlanner = new LocationJourneyPlannerTestFacade(componentContainer.get(LocationJourneyPlanner.class),
                stationRepo, txn);
    }

    @NotNull
    private JourneyRequest createJourneyRequest(TramTime queryTime, int maxChanges) {
        return new JourneyRequest(queryDate, queryTime, false, maxChanges, config.getMaxJourneyDuration());
    }

    @AfterEach
    void afterEachTestRuns()
    {
        txn.close();
    }

    @Test
    void shouldTestSimpleJourneyIsPossible() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 0);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getSecond(), journeyRequest).
                collect(Collectors.toSet());
        assertEquals(1, journeys.size());
        assertFirstAndLast(journeys, FIRST_STATION, SECOND_STATION, 0, queryTime);
    }

    @Test
    void shouldHaveJourneyWithLocationBasedStartViaComposite() {
        LatLong origin = TestEnv.nearAltrincham;

        JourneyRequest journeyRequest = createJourneyRequest(TramTime.of(7, 57), 0);

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(origin,  transportData.getSecond(),
                journeyRequest, 3);

        assertEquals(2, journeys.size(), journeys.toString());
        journeys.forEach(journey ->{
            List<TransportStage<?,?>> stages = journey.getStages();
            assertEquals(2, stages.size(), "stages: " + stages);
            assertEquals(stages.get(0).getMode(), TransportMode.Walk);
            assertEquals(stages.get(1).getMode(), TransportMode.Tram);
        });
    }

    @Test
    void shouldHaveJourneyWithLocationBasedStart() {
        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(nearWythenshaweHosp,  transportData.getInterchange(),
                createJourneyRequest(queryTime, 0), 2);

        assertEquals(1, journeys.size());
        journeys.forEach(journey ->{
            List<TransportStage<?,?>> stages = journey.getStages();
            assertEquals(2, stages.size());
            assertEquals(stages.get(0).getMode(), TransportMode.Walk);
        });
    }

    @Test
    void shouldHaveJourneyWithLocationBasedEnd() {

        final JourneyRequest journeyRequest = createJourneyRequest(queryTime, 1);

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(transportData.getSecond(),
                TestEnv.nearShudehill,
                journeyRequest, 3);

        // TODO investigate why getting duplication here
        assertEquals(2, journeys.size());
        journeys.forEach(journey ->{
            List<TransportStage<?,?>> stages = journey.getStages();
            assertEquals(2, stages.size());
            assertEquals(stages.get(0).getLastStation(), transportData.getInterchange());
            assertEquals(stages.get(1).getMode(), TransportMode.Walk);
        });
    }

    @Test
    void shouldTestSimpleJourneyIsPossibleToInterchangeFromSecondStation() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 1);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getSecond(),
                transportData.getInterchange(), journeyRequest).collect(Collectors.toSet());

        // TODO Investigate why getting duplicates here
        assertEquals(2, journeys.size(), journeys.toString());
    }

    @Test
    void shouldTestSimpleJourneyIsPossibleToInterchange() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 0);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getInterchange(), journeyRequest).collect(Collectors.toSet());
        assertEquals(1, journeys.size());
        assertFirstAndLast(journeys, FIRST_STATION, INTERCHANGE, 1, queryTime);
        checkForPlatforms(journeys);
        journeys.forEach(journey-> assertEquals(1, journey.getStages().size()));
    }

    private void checkForPlatforms(Set<Journey> journeys) {
        journeys.forEach(journey -> journey.getStages().forEach(stage -> assertTrue(stage.hasBoardingPlatform())));
    }

    @Test
    void shouldTestSimpleJourneyIsNotPossible() {

        JourneyRequest journeyRequest = createJourneyRequest(TramTime.of(10, 0), 1);
//        journeyRequest.setDiag(true);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getInterchange(),
                journeyRequest).collect(Collectors.toSet());

        assertEquals(Collections.emptySet(), journeys);
    }

    @Test
    void shouldTestJourneyEndOverWaitLimitIsPossible() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 0);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getLast(), journeyRequest).collect(Collectors.toSet());
        assertEquals(1, journeys.size());
        assertFirstAndLast(journeys, FIRST_STATION, LAST_STATION, 2, queryTime);
        journeys.forEach(journey-> assertEquals(1, journey.getStages().size()));
    }

    @Test
    void shouldTestNoJourneySecondToStart() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 0);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getSecond(),
                transportData.getFirst(), journeyRequest).collect(Collectors.toSet());
        assertEquals(0,journeys.size());
    }

    @Test
    void shouldTestJourneyInterchangeToFive() {
        JourneyRequest journeyRequest = createJourneyRequest(TramTime.of(7,56), 0);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getInterchange(),
                transportData.getFifthStation(), journeyRequest).collect(Collectors.toSet());
        Assertions.assertFalse(journeys.size()>=1);

        JourneyRequest journeyRequestB = createJourneyRequest(TramTime.of(8, 10), 3);
        journeys = calculator.calculateRoute(txn, transportData.getInterchange(),
                transportData.getFifthStation(), journeyRequestB).collect(Collectors.toSet());
        assertTrue(journeys.size()>=1);
        journeys.forEach(journey-> assertEquals(1, journey.getStages().size()));
    }

    @Test
    void shouldTestJourneyEndOverWaitLimitViaInterchangeIsPossible() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 1);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getFourthStation(), journeyRequest).collect(Collectors.toSet());
        assertTrue(journeys.size()>=1);
        checkForPlatforms(journeys);
        journeys.forEach(journey-> assertEquals(2, journey.getStages().size()));
    }

    @Test
    void shouldReturnZeroJourneysIfStartOutOfRange() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 1);

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(transportData.getFirst(),
                TestEnv.nearGreenwichLondon, journeyRequest,3);
        assertTrue(journeys.isEmpty());
    }

    @Test
    void shouldReturnZeroJourneysIfDestOutOfRange() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 1);

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(TestEnv.nearGreenwichLondon,
                transportData.getFirst(), journeyRequest,3);
        assertTrue(journeys.isEmpty());
    }

    @Test
    void shouldTestJourneyEndOverWaitLimitViaInterchangeLocationFinishIsPossible() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 1);

        // nearStockportBus == station 5
        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(transportData.getFirst(),
                TestEnv.nearStockportBus, journeyRequest,3);

        assertTrue(journeys.size()>=1);
        journeys.forEach(journey-> {
            assertEquals(3, journey.getStages().size(), journey.getStages().toString());
            assertEquals(transportData.getInterchange(), journey.getStages().get(0).getLastStation());
            assertEquals(transportData.getFifthStation(), journey.getStages().get(1).getLastStation());
            assertTrue(journey.getStages().get(2) instanceof WalkingStage);
        });
    }

    @Test
    void shouldTestJourneyAnotherWaitLimitViaInterchangeIsPossible() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 1);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getFifthStation(), journeyRequest).collect(Collectors.toSet());
        assertTrue(journeys.size()>=1);
        checkForPlatforms(journeys);
        journeys.forEach(journey-> assertEquals(2, journey.getStages().size()));
    }

    @Test
    void createDiagramOfTestNetwork() {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        Assertions.assertAll(() -> creator.create(Path.of("test_network.dot"),
                transportData.getFirst(), 100, false));
    }

    private static void assertFirstAndLast(Set<Journey> journeys, String firstStation, String secondStation,
                                          int passedStops, TramTime queryTime) {
        Journey journey = (Journey)journeys.toArray()[0];
        List<TransportStage<?,?>> stages = journey.getStages();
        TransportStage<?,?> vehicleStage = stages.get(0);
        assertEquals(firstStation, vehicleStage.getFirstStation().forDTO());
        assertEquals(secondStation, vehicleStage.getLastStation().forDTO());
        assertEquals(passedStops,  vehicleStage.getPassedStopsCount());
        assertTrue(vehicleStage.hasBoardingPlatform());

        TramTime departTime = vehicleStage.getFirstDepartureTime();
        assertTrue(departTime.isAfter(queryTime));

        assertTrue(vehicleStage.getDuration()>0);
    }

}
