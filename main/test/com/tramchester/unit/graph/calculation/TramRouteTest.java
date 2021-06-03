package com.tramchester.unit.graph.calculation;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.WalkingStage;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCostCalculator;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.geo.CoordinateTransforms.calcCostInMinutes;
import static com.tramchester.testSupport.reference.TramTransportDataForTestFactory.TramTransportDataForTest.*;
import static org.junit.jupiter.api.Assertions.*;

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
        return new JourneyRequest(queryDate, queryTime, false, maxChanges, config.getMaxJourneyDuration(), 3);
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

        Journey journey = journeys.iterator().next();
        final TransportStage<?, ?> transportStage = journey.getStages().get(0);
        assertEquals(transportData.getFirst(), transportStage.getFirstStation());
        assertEquals(transportData.getSecond(), transportStage.getLastStation());
        assertEquals(0, transportStage.getPassedStopsCount());
        assertEquals("Red Line", transportStage.getRoute().getShortName());
        assertEquals(transportStage.getFirstStation(), transportStage.getActionStation());
        assertEquals(11, transportStage.getDuration());
        assertEquals(TramTime.of(8,0), transportStage.getFirstDepartureTime());
        assertEquals(TramTime.of(8,11), transportStage.getExpectedArrivalTime()); // +1 for dep cost
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
        final LatLong start = TestEnv.nearWythenshaweHosp;
        final Station destination = transportData.getInterchange();
        final Station midway = transportData.getSecond();

        int walkCost = calcCostInMinutes(start, midway, config.getWalkingMPH());
        assertEquals(4, walkCost);

        int tramDur = 9;
        TramTime tramBoard = TramTime.of(8,11);

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(start, destination,
                createJourneyRequest(queryTime, 0), 2);

        assertEquals(1, journeys.size());
        journeys.forEach(journey ->{
            List<TransportStage<?,?>> stages = journey.getStages();
            assertEquals(2, stages.size());

            final TransportStage<?, ?> walk = stages.get(0);
            final TransportStage<?, ?> tram = stages.get(1);

            assertEquals(midway, walk.getLastStation());
            assertEquals(walk.getMode(), TransportMode.Walk);
            assertEquals(walkCost, walk.getDuration());
            final int boardingCost =  2;
            assertEquals(tramBoard.minusMinutes(boardingCost + walkCost), walk.getFirstDepartureTime());
            assertEquals(tramBoard.minusMinutes(boardingCost), walk.getExpectedArrivalTime());

            assertEquals(midway, tram.getFirstStation());
            assertEquals(destination, tram.getLastStation());
            assertEquals(tramDur, tram.getDuration());
            assertEquals(tramBoard, tram.getFirstDepartureTime());
            assertEquals(tramBoard.plusMinutes(tramDur), tram.getExpectedArrivalTime());
        });
    }

    @Test
    void shouldHaveWalkDirectFromStart() {
        final JourneyRequest journeyRequest = createJourneyRequest(queryTime, 0);
        final LatLong start = TestEnv.nearWythenshaweHosp;
        final Station destination = transportData.getSecond();

        int walkCost = calcCostInMinutes(start, destination, config.getWalkingMPH());
        assertEquals(4, walkCost);

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(start, destination,
                journeyRequest, 2);

        assertEquals(1, journeys.size());
        journeys.forEach(journey -> {
            assertEquals(1, journey.getStages().size());
            TransportStage<?, ?> walk = journey.getStages().get(0);
            assertEquals(TransportMode.Walk, walk.getMode());
            assertEquals(destination, walk.getLastStation());
            assertEquals(queryTime, walk.getFirstDepartureTime());
            assertEquals(walkCost, walk.getDuration());
        });
    }

    @Test
    void shouldHaveWalkDirectAtEnd() {
        final JourneyRequest journeyRequest = createJourneyRequest(queryTime, 0);
        final Station start = transportData.getSecond();
        final LatLong destination = TestEnv.nearWythenshaweHosp;

        int walkCost = calcCostInMinutes(destination, start, config.getWalkingMPH());
        assertEquals(4, walkCost);

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(start, destination,
                journeyRequest, 2);

        assertEquals(1, journeys.size());
        journeys.forEach(journey -> {
            assertEquals(1, journey.getStages().size());
            TransportStage<?, ?> walk = journey.getStages().get(0);
            assertEquals(TransportMode.Walk, walk.getMode());
            assertEquals(start, walk.getFirstStation());
            assertEquals(queryTime, walk.getFirstDepartureTime());
            assertEquals(walkCost, walk.getDuration());
        });
    }

    @Test
    void shouldHaveWalkAtStartAndEnd() {
        final JourneyRequest journeyRequest = createJourneyRequest(queryTime, 2);

        final LatLong start = TestEnv.nearWythenshaweHosp;
        final LatLong destination = TestEnv.atMancArena;

        final Station endFirstWalk = transportData.getSecond();
        final Station startSecondWalk = transportData.getInterchange();

        int walk1Cost = calcCostInMinutes(start, endFirstWalk, config.getWalkingMPH());
        int walk2Cost = calcCostInMinutes(destination, startSecondWalk, config.getWalkingMPH());

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(start,
                destination,
                journeyRequest, 3);
        assertTrue(journeys.size() >= 1, "journeys");
        journeys.forEach(journey -> {
            assertEquals(3, journey.getStages().size());
            TransportStage<?, ?> walk1 = journey.getStages().get(0);
            TransportStage<?, ?> tram = journey.getStages().get(1);
            TransportStage<?, ?> walk2 = journey.getStages().get(2);

            assertEquals(TransportMode.Walk, walk1.getMode());
            assertEquals(TransportMode.Walk, walk2.getMode());
            assertEquals(TransportMode.Tram, tram.getMode());

            assertEquals(walk1Cost, walk1.getDuration());
            assertEquals(walk2Cost, walk2.getDuration());

            assertEquals(endFirstWalk, walk1.getLastStation());
            assertEquals(endFirstWalk, tram.getFirstStation());
            assertEquals(startSecondWalk, tram.getLastStation());
            assertEquals(startSecondWalk, walk2.getFirstStation());

            assertTrue(tram.getFirstDepartureTime().isAfter(walk1.getExpectedArrivalTime()), "tram after walk 1");
            assertTrue(tram.getExpectedArrivalTime().isBefore(walk2.getFirstDepartureTime()), "walk 2 affter tram");

            });

    }

    @Test
    void shouldHaveJourneyWithLocationBasedEnd() {

        final JourneyRequest journeyRequest = createJourneyRequest(queryTime, 1);

        final LatLong destination = TestEnv.atMancArena;
        final Station start = transportData.getSecond();
        final Station midway = transportData.getInterchange();

        int walkCost = calcCostInMinutes(destination, midway, config.getWalkingMPH());
        assertEquals(5, walkCost);

        TramTime boardTime = TramTime.of(8,11);
        final int tramDuration = 9;
        final int depart = 1;

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(start,
                destination,
                journeyRequest, 3);

        // TODO investigate why getting duplication here
        assertEquals(2, journeys.size());
        journeys.forEach(journey ->{
            List<TransportStage<?,?>> stages = journey.getStages();
            assertEquals(2, stages.size());
            final TransportStage<?, ?> tram = stages.get(0);
            final TransportStage<?, ?> walk = stages.get(1);

            assertEquals(tram.getFirstStation(), start);
            assertEquals(tram.getLastStation(), midway);
            assertEquals(tram.getFirstDepartureTime(), boardTime);
            assertEquals(tram.getExpectedArrivalTime(), boardTime.plusMinutes(tramDuration));

            assertEquals(walk.getMode(), TransportMode.Walk);
            assertEquals(walk.getFirstStation(), midway);
            assertEquals(walkCost, walk.getDuration());
            assertEquals(walk.getFirstDepartureTime(), boardTime.plusMinutes(tramDuration+depart));
            assertEquals(boardTime.plusMinutes(tramDuration+depart+walkCost), walk.getExpectedArrivalTime());

            assertTrue(walk.getFirstDepartureTime().isAfter(tram.getExpectedArrivalTime()),
                    tram.getExpectedArrivalTime().toString());

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
        List<Station> expectedPath = Arrays.asList(transportData.getFirst(),
                transportData.getSecond(), transportData.getInterchange(), transportData.getFifthStation());

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getFifthStation(), journeyRequest).collect(Collectors.toSet());
        assertTrue(journeys.size()>=1);
        checkForPlatforms(journeys);
        journeys.forEach(journey-> {
            assertEquals(2, journey.getStages().size());
            final TransportStage<?, ?> firstStage = journey.getStages().get(0);
            final TransportStage<?, ?> secondStage = journey.getStages().get(1);

            assertEquals(1, firstStage.getPassedStopsCount());
            assertEquals(11+9, firstStage.getDuration());

            assertEquals(0, secondStage.getPassedStopsCount());
            assertEquals(4, secondStage.getDuration());
            assertEquals(expectedPath, journey.getPath());

        });
    }

    @Test
    void shouldHaveRouteCostCalculationAsExpected() {
        RouteCostCalculator costCalculator = componentContainer.get(RouteCostCalculator.class);
        assertEquals(43, costCalculator.getApproxCostBetween(txn, transportData.getFirst(), transportData.getLast()));
        assertEquals(-1, costCalculator.getApproxCostBetween(txn, transportData.getLast(), transportData.getFirst()));
    }

    @Test
    void shouldHaveNumberHopsAsExpected() {
        RouteCostCalculator costCalculator = componentContainer.get(RouteCostCalculator.class);
        assertEquals(1, costCalculator.getNumberHops(txn, transportData.getFirst(), transportData.getSecond()));
        assertEquals(2, costCalculator.getNumberHops(txn, transportData.getFirst(), transportData.getInterchange()));
        assertEquals(3, costCalculator.getNumberHops(txn, transportData.getFirst(), transportData.getFourthStation()));
        assertEquals(3, costCalculator.getNumberHops(txn, transportData.getFirst(), transportData.getFifthStation()));
        assertEquals(3, costCalculator.getNumberHops(txn, transportData.getFirst(), transportData.getLast()));

        assertEquals(-1, costCalculator.getNumberHops(txn, transportData.getLast(), transportData.getFirst()));
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
