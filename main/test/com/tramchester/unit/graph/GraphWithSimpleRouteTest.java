package com.tramchester.unit.graph;

import com.tramchester.Dependencies;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.DTO.AreaDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.TransportDataSource;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GraphWithSimpleRouteTest {

    private static final String TMP_DB = "tmp.db";

    private static TransportDataSource transportData;
    private static RouteCalculator calculator;
    private static Dependencies dependencies;
    private static IntegrationTramTestConfig config;

    private TramServiceDate queryDate;
    private List<LocalTime> queryTimes;
    private Station firstStation;
    private LocalTime queryTime;

    @BeforeClass
    public static void onceBeforeAllTestRuns() throws IOException, TramchesterException {
        transportData = new TransportDataForTest();

        dependencies = new Dependencies();
        config = new IntegrationTramTestConfig(TMP_DB);
        FileUtils.deleteDirectory( new File(TMP_DB));

        dependencies.initialise(config, transportData);

        calculator = dependencies.get(RouteCalculator.class);
    }

    @AfterClass
    public static void onceAfterAllTestsRun() throws IOException {
        dependencies.close();
        FileUtils.deleteDirectory( new File(TMP_DB));
    }

    @Before
    public void beforeEachTestRuns() {
        queryDate = new TramServiceDate("20140630");
        // note: trams only run at specific times so still only getPlatformById one journey in results
        //queryTimes = Arrays.asList(new Integer[]{minutesPastMidnight, minutesPastMidnight+6});
        firstStation = transportData.getStation(TransportDataForTest.FIRST_STATION).get();
        queryTime = LocalTime.of(7, 57);
        queryTimes = Collections.singletonList(queryTime);
    }

    @Test
    public void shouldTestSimpleJourneyIsPossible() {
        Set<RawJourney> journeys = calculator.calculateRoute(TransportDataForTest.FIRST_STATION,
                TransportDataForTest.SECOND_STATION, queryTimes, queryDate, RouteCalculator.MAX_NUM_GRAPH_PATHS);
        assertEquals(1, journeys.size());
        assertFirstAndLast(journeys, TransportDataForTest.FIRST_STATION, TransportDataForTest.SECOND_STATION, 0, "RouteClass1");
    }

    @Test
    public void shouldHaveSimpleWalkAndTramTrip() {
        LatLong origin = new LatLong(180.001, 270.001);
        Station endStation = transportData.getStation(TransportDataForTest.SECOND_STATION).get();
        List<StationWalk> walks = Collections.singletonList(new StationWalk(firstStation, 1));

        Set<RawJourney> journeys = calculator.calculateRoute(origin, walks, endStation, queryTimes, queryDate, RouteCalculator.MAX_NUM_GRAPH_PATHS);
        assertFalse(journeys.isEmpty());
        journeys.forEach(journey ->{
            List<RawStage> stages = journey.getStages();
            assertEquals(2, stages.size());
            assertTrue(stages.get(0).getMode().isWalk());
            assertTrue(stages.get(1).getMode().isVehicle());
        });
    }

    @Test
    public void shouldTestSimpleJourneyIsPossibleToInterchange() {
        Set<RawJourney> journeys = calculator.calculateRoute(TransportDataForTest.FIRST_STATION,
                TransportDataForTest.INTERCHANGE, queryTimes, queryDate, RouteCalculator.MAX_NUM_GRAPH_PATHS);
        assertEquals(1, journeys.size());
        assertFirstAndLast(journeys, TransportDataForTest.FIRST_STATION, TransportDataForTest.INTERCHANGE, 1, "RouteClass1");
        checkForPlatforms(journeys);
        journeys.forEach(journey->assertEquals(1, journey.getStages().size()));
    }

    private void checkForPlatforms(Set<RawJourney> journeys) {
        journeys.forEach(journey ->{
            journey.getStages().forEach(stage -> assertTrue(((RawVehicleStage)stage).getBoardingPlatform().isPresent()));
        });
    }

    @Test
    public void shouldTestSimpleJourneyIsNotPossible() {
        Set<RawJourney> journeys = calculator.calculateRoute(TransportDataForTest.FIRST_STATION,
                TransportDataForTest.INTERCHANGE, Collections.singletonList(LocalTime.of(9, 0)), queryDate, RouteCalculator.MAX_NUM_GRAPH_PATHS);
        assertEquals(0, journeys.size());
    }

    @Test
    public void shouldTestJourneyEndOverWaitLimitIsPossible() {
        Set<RawJourney> journeys = calculator.calculateRoute(TransportDataForTest.FIRST_STATION,
                TransportDataForTest.LAST_STATION, queryTimes, queryDate, RouteCalculator.MAX_NUM_GRAPH_PATHS);
        assertEquals(1, journeys.size());
        assertFirstAndLast(journeys, TransportDataForTest.FIRST_STATION, TransportDataForTest.LAST_STATION, 2, "RouteClass1");
        journeys.forEach(journey->assertEquals(1, journey.getStages().size()));

    }

    @Test
    public void shouldTestNoJourneySecondToStart() {
        Set<RawJourney> journeys = calculator.calculateRoute(TransportDataForTest.SECOND_STATION,
                TransportDataForTest.FIRST_STATION, queryTimes, queryDate, RouteCalculator.MAX_NUM_GRAPH_PATHS);
        assertEquals(0,journeys.size());
    }

    @Test
    public void shouldTestJourneyInterchangeToFive() {
        Set<RawJourney> journeys = calculator.calculateRoute(TransportDataForTest.INTERCHANGE,
                TransportDataForTest.STATION_FIVE, queryTimes, queryDate, RouteCalculator.MAX_NUM_GRAPH_PATHS);
        assertFalse(journeys.size()>=1);
        List<LocalTime> laterQueryTimes = Arrays.asList(LocalTime.of(8,10));
        journeys = calculator.calculateRoute(TransportDataForTest.INTERCHANGE,
                TransportDataForTest.STATION_FIVE, laterQueryTimes, queryDate, RouteCalculator.MAX_NUM_GRAPH_PATHS);
        assertTrue(journeys.size()>=1);
        journeys.forEach(journey->assertEquals(1, journey.getStages().size()));

    }

    @Test
    public void shouldTestJourneyEndOverWaitLimitViaInterchangeIsPossible() {
        Set<RawJourney> journeys = calculator.calculateRoute(TransportDataForTest.FIRST_STATION,
                TransportDataForTest.STATION_FOUR, queryTimes, queryDate, RouteCalculator.MAX_NUM_GRAPH_PATHS);
       assertTrue(journeys.size()>=1);
       checkForPlatforms(journeys);
        journeys.forEach(journey->assertEquals(2, journey.getStages().size()));

    }

    @Test
    public void shouldTestJourneyAnotherWaitLimitViaInterchangeIsPossible() {
        Set<RawJourney> journeys = calculator.calculateRoute(TransportDataForTest.FIRST_STATION,
                TransportDataForTest.STATION_FIVE, queryTimes, queryDate, RouteCalculator.MAX_NUM_GRAPH_PATHS);
        assertTrue(journeys.size()>=1);
        checkForPlatforms(journeys);
        journeys.forEach(journey->assertEquals(2, journey.getStages().size()));

    }

    @Test
    public void shouldTestJourneyWithLocationBasedStart() {
        LatLong origin = new LatLong(180.001, 270.001);
        int walkCost = 1;

        List<StationWalk> stationWalks = Collections.singletonList(new StationWalk(firstStation, walkCost));
        Station endStation = transportData.getStation(TransportDataForTest.SECOND_STATION).get();

        Set<RawJourney> journeys = calculator.calculateRoute(origin, stationWalks, endStation, queryTimes, queryDate, RouteCalculator.MAX_NUM_GRAPH_PATHS);

        assertEquals(1, journeys.size());
        journeys.forEach(journey->assertEquals(2, journey.getStages().size()));

    }

    @Test
    public void createDiagramOfTestNetwork() throws IOException {
        NodeFactory nodeFactory = dependencies.get(NodeFactory.class);
        RelationshipFactory relationshipFactory = dependencies.get(RelationshipFactory.class);
        GraphDatabaseService graphDBService = dependencies.get(GraphDatabaseService.class);

        DiagramCreator creator = new DiagramCreator(nodeFactory, relationshipFactory, graphDBService, Integer.MAX_VALUE);
        creator.create("test_network.dot", TransportDataForTest.FIRST_STATION);
    }

    @Ignore("WIP")
    @Test
    public void shouldFindJourneyAreaToArea() {
        AreaDTO areaA = new AreaDTO("areaA");
        AreaDTO areaB = new AreaDTO("areaB");

        Set<RawJourney> journeys =  calculator.calculateRoute(areaA, areaB, queryTimes, queryDate, RouteCalculator.MAX_NUM_GRAPH_PATHS);
        assertEquals(1, journeys.size());

    }

    private void assertFirstAndLast(Set<RawJourney> journeys, String firstStation, String secondStation, int passedStops, String displayClass) {
        RawJourney journey = (RawJourney)journeys.toArray()[0];
        List<RawStage> stages = journey.getStages();
        RawVehicleStage vehicleStage = (RawVehicleStage) stages.get(0);
        assertEquals(firstStation, vehicleStage.getFirstStation().getId());
        assertEquals(secondStation, vehicleStage.getLastStation().getId());
        assertEquals(passedStops,  vehicleStage.getPassedStops());
        assertEquals(displayClass, vehicleStage.getDisplayClass());
        assertTrue(vehicleStage.getBoardingPlatform().isPresent());
        if (config.getEdgePerTrip()) {
            LocalTime departTime = vehicleStage.getDepartTime();
            assertTrue(departTime.isAfter(queryTime));
        }
        assertTrue(vehicleStage.getCost()>0);
    }
}
