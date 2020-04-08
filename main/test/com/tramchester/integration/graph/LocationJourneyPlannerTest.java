package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.Station;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.WalkingStage;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.*;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.TestEnv.nearAltrincham;
import static com.tramchester.testSupport.TestEnv.nearPiccGardens;
import static org.junit.Assert.*;

public class LocationJourneyPlannerTest {
    private static final int TXN_TIMEOUT = 5*60;

    private static Dependencies dependencies;
    private static TramchesterConfig testConfig;
    private static GraphDatabase database;

    private LocalDate nextTuesday = TestEnv.nextTuesday(0);
    private Transaction tx;
    private LocationJourneyPlanner planner;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
        database = dependencies.get(GraphDatabase.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Before
    public void beforeEachTestRuns() {
        tx = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        planner = dependencies.get(LocationJourneyPlanner.class);
    }

    @After
    public void afterEachTestRuns() {
        tx.close();
    }

    @Test
    public void shouldHaveDirectWalkNearPiccadily() {
        TramServiceDate queryDate = new TramServiceDate(nextTuesday);

        Set<Journey> unsortedResults = getJourneySet(new JourneyRequest(queryDate,TramTime.of(9, 0), false) ,
                nearPiccGardens, Stations.PiccadillyGardens);

        assertFalse(unsortedResults.isEmpty());
        unsortedResults.forEach(journey -> {
            List<TransportStage> stages = journey.getStages();
            WalkingStage first = (WalkingStage) stages.get(0);
            assertEquals(nearPiccGardens, first.getStart().getLatLong());
            assertEquals(Stations.PiccadillyGardens, first.getDestination());
        });
    }

    private Set<Journey> getJourneySet(JourneyRequest journeyRequest, LatLong nearPiccGardens, Station dest) {
        Stream<Journey> journeyStream = planner.quickestRouteForLocation(nearPiccGardens, dest, journeyRequest);
        Set<Journey> journeySet = journeyStream.collect(Collectors.toSet());
        journeyStream.close();
        return journeySet;
    }

    @Test
    public void shouldHaveDirectWalkFromPiccadily() {
        TramServiceDate queryDate = new TramServiceDate(nextTuesday);

        Stream<Journey> journeyStream = planner.quickestRouteForLocation(Stations.PiccadillyGardens.getId(),
                nearPiccGardens, new JourneyRequest(queryDate, TramTime.of(9, 0), false));
        Set<Journey> unsortedResults = journeyStream.collect(Collectors.toSet());
        journeyStream.close();

        assertFalse(unsortedResults.isEmpty());
        unsortedResults.forEach(journey -> {
            List<TransportStage> stages = journey.getStages();
            WalkingStage first = (WalkingStage) stages.get(0);
            assertEquals(Stations.PiccadillyGardens, first.getStart());
            assertEquals(nearPiccGardens, first.getDestination().getLatLong());
        });
    }

    @Test
    public void shouldFindJourneyWithWalkingAtEndEarlyMorning() {
        List<Journey> results = getSortedJourneysForTramThenWalk(Stations.Deansgate.getId(), nearAltrincham,
                TramTime.of(8,00), false);
        List<Journey> twoStageJourneys = results.stream().filter(journey -> journey.getStages().size() == 2).collect(Collectors.toList());

        assertFalse(twoStageJourneys.isEmpty());
        Journey firstJourney = twoStageJourneys.get(0);
        TransportStage tramStage = firstJourney.getStages().get(0);
        TransportStage walkStage = firstJourney.getStages().get(1);

        assertTrue(walkStage.getFirstDepartureTime().isAfter(tramStage.getExpectedArrivalTime()));
        assertEquals(Stations.Deansgate.getId(), tramStage.getFirstStation().getId());
        assertEquals(Stations.NavigationRoad.getId(), tramStage.getLastStation().getId());
        assertEquals(Stations.NavigationRoad.getId(), walkStage.getFirstStation().getId());
        assertEquals("MyLocationPlaceholderId", walkStage.getLastStation().getId());
        assertEquals(Stations.NavigationRoad.getId(), walkStage.getActionStation().getId());
    }

    @Test
    public void shouldFindJourneyWithWalkingEarlyMorning() {
        Set<Journey> results = getJourneysForWalkThenTram(nearAltrincham, Stations.Deansgate,
                TramTime.of(8,00), false, 2);

        assertFalse(results.isEmpty());
        results.forEach(journey -> assertEquals(2, journey.getStages().size()));
        results.forEach(journey -> assertEquals(TransportMode.Walk, journey.getStages().get(0).getMode()));
    }

    @Test
    public void shouldFindJourneyWithWalkingEarlyMorningArriveBy() {
        TramTime queryTime = TramTime.of(8, 00);
        Set<Journey> results = getJourneysForWalkThenTram(nearAltrincham, Stations.Deansgate,
                queryTime, true, 2);

        assertFalse(results.isEmpty());
        results.forEach(journey -> assertTrue(journey.getQueryTime().isBefore(queryTime)));
    }

    @Test
    public void shouldFindJourneyWithWalkingAtEndEarlyMorningArriveBy() {
        TramTime queryTime = TramTime.of(8, 00);
        List<Journey> results = getSortedJourneysForTramThenWalk(Stations.Deansgate.getId(), nearAltrincham,
                queryTime, true);

        assertFalse(results.isEmpty());
        results.forEach(journey -> assertTrue(journey.getQueryTime().isBefore(queryTime)));
    }

    @Test
    public void shouldFindJourneyWithWalkingAtEndNearShudehill() {
        TramTime queryTime = TramTime.of(8, 30);
        List<Journey> results = getSortedJourneysForTramThenWalk(Stations.Shudehill.getId(), TestEnv.nearShudehill,
                queryTime, false);
        assertFalse(results.isEmpty());
    }

    @Ignore("No longer passing due to increased wait times")
    @Test
    public void shouldFindJourneyWithWalkingAtEndDeansgateNearShudehill() {
        TramTime queryTime = TramTime.of(8, 35);
        List<Journey> results = getSortedJourneysForTramThenWalk(Stations.Altrincham.getId(), TestEnv.nearShudehill,
                queryTime, false);

        assertFalse(results.isEmpty());

        // find the lowest cost journey, should be tram to shudehill and then a walk
        Journey lowestCostJourney = results.get(0);

        // 33 -> 41
        assertEquals(lowestCostJourney.toString(), 33, RouteCalculatorTest.costOfJourney(lowestCostJourney));

        List<TransportStage> stages = lowestCostJourney.getStages();
        assertTrue(stages.size() >= 2);
        // lowest cost should be walk from shudehill
        assertEquals(Stations.Shudehill, stages.get(0).getLastStation());
        assertEquals(Stations.Shudehill, stages.get(1).getFirstStation());
    }

    @Ignore("Temporary: trams finish at 2300")
    @Test
    public void shouldFindJourneyWithWalkingEndOfDay() {
        Set<Journey> results = getJourneysForWalkThenTram(nearAltrincham, Stations.Deansgate,
                TramTime.of(23,00), false, 2);
        assertFalse(results.isEmpty());
        results.forEach(journey -> assertEquals(2, journey.getStages().size()));
    }

    @Test
    public void shouldFindWalkOnlyIfNearDestinationStationSingleStationWalk() {
        Set<Journey> results = getJourneysForWalkThenTram(nearPiccGardens, Stations.PiccadillyGardens,
                TramTime.of(9,00), false, 2); //, new StationWalk(Stations.PiccadillyGardens, 3));
        assertFalse(results.isEmpty());
        results.forEach(journey-> {
            assertEquals(1,journey.getStages().size());
            TransportStage rawStage = journey.getStages().get(0);
            assertEquals(TransportMode.Walk, rawStage.getMode());
            assertEquals(Stations.PiccadillyGardens, ((WalkingStage) rawStage).getDestination());
            assertEquals(nearPiccGardens, ((WalkingStage) rawStage).getStart().getLatLong());
            assertEquals(3, rawStage.getDuration());
        });
    }

    private Set<Journey> getJourneysForWalkThenTram(LatLong latLong, Station destination, TramTime queryTime, boolean arriveBy, int maxChanges) {
        TramServiceDate date = new TramServiceDate(nextTuesday);

        return getJourneySet(new JourneyRequest(date, queryTime, arriveBy, maxChanges), latLong, destination);
    }

    private List<Journey> getSortedJourneysForTramThenWalk(String startId, LatLong latLong, TramTime queryTime, boolean arriveBy) {
        TramServiceDate date = new TramServiceDate(nextTuesday);

        Stream<Journey> journeyStream = planner.quickestRouteForLocation(startId, latLong,
                new JourneyRequest(date, queryTime, arriveBy, Integer.MAX_VALUE)).sorted(Comparator.comparingInt(RouteCalculatorTest::costOfJourney));
        List<Journey> journeyList = journeyStream.collect(Collectors.toList());
        journeyStream.close();
        return journeyList;

    }
}
