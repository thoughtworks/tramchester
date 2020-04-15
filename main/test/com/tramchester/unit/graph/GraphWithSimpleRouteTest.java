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
import org.junit.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GraphWithSimpleRouteTest {

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

    @BeforeClass
    public static void onceBeforeAllTestRuns() throws IOException {
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

    @AfterClass
    public static void onceAfterAllTestsRun() throws IOException {
        dependencies.close();
        FileUtils.deleteDirectory(config.getDBPath().toFile());
    }

    @Before
    public void beforeEachTestRuns() {
        queryDate = new TramServiceDate(LocalDate.of(2014,6,30));
        queryTime = TramTime.of(7, 57);
        journeyRequest = new JourneyRequest(queryDate, queryTime, false);
        tx = database.beginTx();
    }

    @After
    public void afterEachTestRuns()
    {
        tx.close();
    }

    @Test
    public void shouldTestSimpleJourneyIsPossible() {
        Set<Journey> journeys = calculator.calculateRoute(TransportDataForTest.FIRST_STATION,
                transportData.getSecondStation(), journeyRequest).
                collect(Collectors.toSet());
        assertEquals(1, journeys.size());
        assertFirstAndLast(journeys, TransportDataForTest.FIRST_STATION, TransportDataForTest.SECOND_STATION, 0, "RouteClass1");
    }

    @Test
    public void shouldHaveJourneyWithLocationBasedStart() {
        LatLong origin = TestEnv.nearAltrincham;

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(origin,  transportData.getSecondStation(),
                new JourneyRequest(queryDate, TramTime.of(7,55), false)).collect(Collectors.toSet());

        assertEquals(1, journeys.size());
        journeys.forEach(journey ->{
            List<TransportStage> stages = journey.getStages();
            assertEquals(2, stages.size());
            assertTrue(stages.get(0).getMode().isWalk());
        });
    }

    @Test
    public void shouldHaveJourneyWithLocationBasedEnd() {
        LatLong origin = TestEnv.nearShudehill;

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(TransportDataForTest.SECOND_STATION, origin,
                new JourneyRequest(queryDate, TramTime.of(7,55), false)).collect(Collectors.toSet());

        assertEquals(1, journeys.size());
        journeys.forEach(journey ->{
            List<TransportStage> stages = journey.getStages();
            assertEquals(1, stages.size());
            assertTrue(stages.get(0).getMode().isWalk());
        });
    }

    @Test
    public void shouldTestSimpleJourneyIsPossibleToInterchange() {
        Set<Journey> journeys = calculator.calculateRoute(TransportDataForTest.FIRST_STATION,
                transportData.getInterchange(), journeyRequest).collect(Collectors.toSet());
        assertEquals(1, journeys.size());
        assertFirstAndLast(journeys, TransportDataForTest.FIRST_STATION, TransportDataForTest.INTERCHANGE,
                1, "RouteClass1");
        checkForPlatforms(journeys);
        journeys.forEach(journey->assertEquals(1, journey.getStages().size()));
    }

    private void checkForPlatforms(Set<Journey> journeys) {
        journeys.forEach(journey -> journey.getStages().forEach(stage -> assertTrue(stage.getBoardingPlatform().isPresent())));
    }

    @Test
    public void shouldTestSimpleJourneyIsNotPossible() {
        Set<Journey> journeys = calculator.calculateRoute(TransportDataForTest.FIRST_STATION,
                transportData.getInterchange(),  new JourneyRequest(queryDate, TramTime.of(9, 0),
                        false)).collect(Collectors.toSet());
        assertEquals(0, journeys.size());
    }

    @Test
    public void shouldTestJourneyEndOverWaitLimitIsPossible() {
        Set<Journey> journeys = calculator.calculateRoute(TransportDataForTest.FIRST_STATION,
                transportData.getLast(), journeyRequest).collect(Collectors.toSet());
        assertEquals(1, journeys.size());
        assertFirstAndLast(journeys, TransportDataForTest.FIRST_STATION, TransportDataForTest.LAST_STATION,
                2, "RouteClass1");
        journeys.forEach(journey->assertEquals(1, journey.getStages().size()));

    }

    @Test
    public void shouldTestNoJourneySecondToStart() {
        Set<Journey> journeys = calculator.calculateRoute(TransportDataForTest.SECOND_STATION,
                transportData.getFirst(), journeyRequest).collect(Collectors.toSet());
        assertEquals(0,journeys.size());
    }

    @Test
    public void shouldTestJourneyInterchangeToFive() {
        Set<Journey> journeys = calculator.calculateRoute(TransportDataForTest.INTERCHANGE,
                transportData.getFifthStation(), journeyRequest).collect(Collectors.toSet());
        assertFalse(journeys.size()>=1);
        journeys = calculator.calculateRoute(TransportDataForTest.INTERCHANGE,
                transportData.getFifthStation(),  new JourneyRequest(queryDate, TramTime.of(8, 10), false)).collect(Collectors.toSet());
        assertTrue(journeys.size()>=1);
        journeys.forEach(journey->assertEquals(1, journey.getStages().size()));

    }

    @Test
    public void shouldTestJourneyEndOverWaitLimitViaInterchangeIsPossible() {
        Set<Journey> journeys = calculator.calculateRoute(TransportDataForTest.FIRST_STATION,
                transportData.getFourthStation(), journeyRequest).collect(Collectors.toSet());
       assertTrue(journeys.size()>=1);
       checkForPlatforms(journeys);
        journeys.forEach(journey->assertEquals(2, journey.getStages().size()));
    }

    @Test
    public void shouldTestJourneyAnotherWaitLimitViaInterchangeIsPossible() {
        Set<Journey> journeys = calculator.calculateRoute(TransportDataForTest.FIRST_STATION,
                transportData.getFifthStation(), journeyRequest).collect(Collectors.toSet());
        assertTrue(journeys.size()>=1);
        checkForPlatforms(journeys);
        journeys.forEach(journey->assertEquals(2, journey.getStages().size()));
    }

    @Test
    public void createDiagramOfTestNetwork() throws IOException {
        DiagramCreator creator = new DiagramCreator(database, Integer.MAX_VALUE);
        creator.create("test_network.dot", TransportDataForTest.FIRST_STATION);
    }

//    @Ignore("WIP")
//    @Test
//    public void shouldFindJourneyAreaToArea() {
//        AreaDTO areaA = new AreaDTO("areaA");
//        AreaDTO areaB = new AreaDTO("areaB");
//
//        Set<RawJourney> journeys =  calculator.calculateRoute(areaA, areaB, queryTimes, queryDate, RouteCalculator.MAX_NUM_GRAPH_PATHS);
//        assertEquals(1, journeys.size());
//
//    }

    private void assertFirstAndLast(Set<Journey> journeys, String firstStation, String secondStation,
                                    int passedStops, String displayClass) {
        Journey journey = (Journey)journeys.toArray()[0];
        List<TransportStage> stages = journey.getStages();
        TransportStage vehicleStage = stages.get(0);
        assertEquals(firstStation, vehicleStage.getFirstStation().getId());
        assertEquals(secondStation, vehicleStage.getLastStation().getId());
        assertEquals(passedStops,  vehicleStage.getPassedStops());
        assertEquals(displayClass, vehicleStage.getDisplayClass());
        assertTrue(vehicleStage.getBoardingPlatform().isPresent());

        TramTime departTime = vehicleStage.getFirstDepartureTime();
        assertTrue(departTime.isAfter(queryTime));

        assertTrue(vehicleStage.getDuration()>0);
    }
}
