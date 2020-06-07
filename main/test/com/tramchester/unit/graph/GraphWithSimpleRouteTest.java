package com.tramchester.unit.graph;

import com.tramchester.Dependencies;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.Journey;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.TestEnv;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class GraphWithSimpleRouteTest {

    private static final String TMP_DB = "tmp.db";

    private static TransportDataForTest transportData;
    private static RouteCalculator calculator;
    private static Dependencies dependencies;
    private static GraphDatabase database;
    private static LocationJourneyPlanner locationJourneyPlanner;
    private static IntegrationTramTestConfig config;

    private TramServiceDate queryDate;
    private TramTime queryTime;
    private Transaction tx;
    private JourneyRequest journeyRequest;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        dependencies = new Dependencies();

        StationLocations stationLocations = dependencies.get(StationLocations.class);
        transportData = new TransportDataForTest(stationLocations);

        config = new IntegrationTramTestConfig(TMP_DB);
        FileUtils.deleteDirectory(config.getDBPath().toFile());

        dependencies.initialise(config, transportData);

        database = dependencies.get(GraphDatabase.class);
        calculator = dependencies.get(RouteCalculator.class);
        locationJourneyPlanner = dependencies.get(LocationJourneyPlanner.class);
    }

    @AfterAll
    static void onceAfterAllTestsRun() throws IOException {
        dependencies.close();
        FileUtils.deleteDirectory(config.getDBPath().toFile());
    }

    @BeforeEach
    void beforeEachTestRuns() {
        queryDate = new TramServiceDate(LocalDate.of(2014,6,30));
        queryTime = TramTime.of(7, 57);
        journeyRequest = new JourneyRequest(queryDate, queryTime, false);
        tx = database.beginTx();
    }

    @AfterEach
    void afterEachTestRuns()
    {
        tx.close();
    }

    @Test
    void shouldTestSimpleJourneyIsPossible() {
        Set<Journey> journeys = calculator.calculateRoute(transportData.getFirst(),
                transportData.getSecondStation(), journeyRequest).
                collect(Collectors.toSet());
        Assertions.assertEquals(1, journeys.size());
        assertFirstAndLast(journeys, TransportDataForTest.FIRST_STATION, TransportDataForTest.SECOND_STATION, 0, "RouteClass1");
    }

    @Test
    void shouldHaveJourneyWithLocationBasedStart() {
        LatLong origin = TestEnv.nearAltrincham;

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(origin,  transportData.getSecondStation(),
                new JourneyRequest(queryDate, TramTime.of(7,55), false)).collect(Collectors.toSet());

        Assertions.assertEquals(1, journeys.size());
        journeys.forEach(journey ->{
            List<TransportStage> stages = journey.getStages();
            Assertions.assertEquals(2, stages.size());
            Assertions.assertTrue(stages.get(0).getMode().isWalk());
        });
    }

    @Test
    void shouldHaveJourneyWithLocationBasedEnd() {
        LatLong origin = TestEnv.nearShudehill;

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(transportData.getSecond(), origin,
                new JourneyRequest(queryDate, TramTime.of(7,55), false)).collect(Collectors.toSet());

        Assertions.assertEquals(1, journeys.size());
        journeys.forEach(journey ->{
            List<TransportStage> stages = journey.getStages();
            Assertions.assertEquals(1, stages.size());
            Assertions.assertTrue(stages.get(0).getMode().isWalk());
        });
    }

    @Test
    void shouldTestSimpleJourneyIsPossibleToInterchange() {
        Set<Journey> journeys = calculator.calculateRoute(transportData.getFirst(),
                transportData.getInterchange(), journeyRequest).collect(Collectors.toSet());
        Assertions.assertEquals(1, journeys.size());
        assertFirstAndLast(journeys, TransportDataForTest.FIRST_STATION, TransportDataForTest.INTERCHANGE,
                1, "RouteClass1");
        checkForPlatforms(journeys);
        journeys.forEach(journey-> Assertions.assertEquals(1, journey.getStages().size()));
    }

    private void checkForPlatforms(Set<Journey> journeys) {
        journeys.forEach(journey -> journey.getStages().forEach(stage -> Assertions.assertTrue(stage.getBoardingPlatform().isPresent())));
    }

    @Test
    void shouldTestSimpleJourneyIsNotPossible() {
        Set<Journey> journeys = calculator.calculateRoute(transportData.getFirst(),
                transportData.getInterchange(),  new JourneyRequest(queryDate, TramTime.of(9, 0),
                        false)).collect(Collectors.toSet());
        Assertions.assertEquals(0, journeys.size());
    }

    @Test
    void shouldTestJourneyEndOverWaitLimitIsPossible() {
        Set<Journey> journeys = calculator.calculateRoute(transportData.getFirst(),
                transportData.getLast(), journeyRequest).collect(Collectors.toSet());
        Assertions.assertEquals(1, journeys.size());
        assertFirstAndLast(journeys, TransportDataForTest.FIRST_STATION, TransportDataForTest.LAST_STATION,
                2, "RouteClass1");
        journeys.forEach(journey-> Assertions.assertEquals(1, journey.getStages().size()));

    }

    @Test
    void shouldTestNoJourneySecondToStart() {
        Set<Journey> journeys = calculator.calculateRoute(transportData.getSecondStation(),
                transportData.getFirst(), journeyRequest).collect(Collectors.toSet());
        Assertions.assertEquals(0,journeys.size());
    }

    @Test
    void shouldTestJourneyInterchangeToFive() {
        Set<Journey> journeys = calculator.calculateRoute(transportData.getInterchange(),
                transportData.getFifthStation(), journeyRequest).collect(Collectors.toSet());
        Assertions.assertFalse(journeys.size()>=1);
        journeys = calculator.calculateRoute(transportData.getInterchange(),
                transportData.getFifthStation(),  new JourneyRequest(queryDate, TramTime.of(8, 10), false)).collect(Collectors.toSet());
        Assertions.assertTrue(journeys.size()>=1);
        journeys.forEach(journey-> Assertions.assertEquals(1, journey.getStages().size()));

    }

    @Test
    void shouldTestJourneyEndOverWaitLimitViaInterchangeIsPossible() {
        Set<Journey> journeys = calculator.calculateRoute(transportData.getFirst(),
                transportData.getFourthStation(), journeyRequest).collect(Collectors.toSet());
       Assertions.assertTrue(journeys.size()>=1);
       checkForPlatforms(journeys);
        journeys.forEach(journey-> Assertions.assertEquals(2, journey.getStages().size()));
    }

    @Test
    void shouldTestJourneyAnotherWaitLimitViaInterchangeIsPossible() {
        Set<Journey> journeys = calculator.calculateRoute(transportData.getFirst(),
                transportData.getFifthStation(), journeyRequest).collect(Collectors.toSet());
        Assertions.assertTrue(journeys.size()>=1);
        checkForPlatforms(journeys);
        journeys.forEach(journey-> Assertions.assertEquals(2, journey.getStages().size()));
    }

    @Test
    void createDiagramOfTestNetwork() {
        DiagramCreator creator = new DiagramCreator(database, Integer.MAX_VALUE);
        Assertions.assertAll(() -> creator.create("test_network.dot", TransportDataForTest.FIRST_STATION));
    }

    private void assertFirstAndLast(Set<Journey> journeys, String firstStation, String secondStation,
                                    int passedStops, String displayClass) {
        Journey journey = (Journey)journeys.toArray()[0];
        List<TransportStage> stages = journey.getStages();
        TransportStage vehicleStage = stages.get(0);
        Assertions.assertEquals(firstStation, vehicleStage.getFirstStation().getId());
        Assertions.assertEquals(secondStation, vehicleStage.getLastStation().getId());
        Assertions.assertEquals(passedStops,  vehicleStage.getPassedStops());
        Assertions.assertEquals(displayClass, vehicleStage.getDisplayClass());
        Assertions.assertTrue(vehicleStage.getBoardingPlatform().isPresent());

        TramTime departTime = vehicleStage.getFirstDepartureTime();
        Assertions.assertTrue(departTime.isAfter(queryTime));

        Assertions.assertTrue(vehicleStage.getDuration()>0);
    }
}
