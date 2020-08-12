package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.domain.Journey;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.WalkingStage;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
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
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.TestEnv.*;
import static org.junit.jupiter.api.Assertions.*;

class LocationJourneyPlannerTest {
    private static final int TXN_TIMEOUT = 5*60;

    private static Dependencies dependencies;
    private static GraphDatabase database;
    private static IntegrationTramTestConfig testConfig;

    private final LocalDate when = TestEnv.testDay();
    private Transaction txn;
    private LocationJourneyPlanner planner;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
        database = dependencies.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        planner = dependencies.get(LocationJourneyPlanner.class);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveDirectWalkNearPiccadily() {
        TramServiceDate queryDate = new TramServiceDate(when);

        Set<Journey> unsortedResults = getJourneySet(
                new JourneyRequest(queryDate,TramTime.of(9, 0), false, 2, testConfig.getMaxJourneyDuration()) ,
                nearPiccGardens, Stations.PiccadillyGardens);

        assertFalse(unsortedResults.isEmpty());
        unsortedResults.forEach(journey -> {
            List<TransportStage> stages = journey.getStages();
            WalkingStage first = (WalkingStage) stages.get(0);
            assertEquals(nearPiccGardens, first.getStart().getLatLong());
            assertEquals(Stations.PiccadillyGardens, first.getDestination());
        });

        unsortedResults.forEach(journey -> {
            List<Location> callingPoints = journey.getPath();
            assertEquals(2, callingPoints.size());
            assertEquals(nearPiccGardens, callingPoints.get(0).getLatLong());
            assertEquals(Stations.PiccadillyGardens, callingPoints.get(1));
        });
    }

    private Set<Journey> getJourneySet(JourneyRequest journeyRequest, LatLong nearPiccGardens, Station dest) {
        Stream<Journey> journeyStream = planner.quickestRouteForLocation(txn, nearPiccGardens, dest, journeyRequest);
        Set<Journey> journeySet = journeyStream.collect(Collectors.toSet());
        journeyStream.close();
        return journeySet;
    }

    @Test
    void shouldHaveDirectWalkFromPiccadily() {
        TramServiceDate queryDate = new TramServiceDate(when);

        Stream<Journey> journeyStream = planner.quickestRouteForLocation(txn, Stations.PiccadillyGardens,
                nearPiccGardens, new JourneyRequest(queryDate, TramTime.of(9, 0),
                        false, 1, testConfig.getMaxJourneyDuration()));
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
    void shouldFindJourneyWithWalkingAtEndEarlyMorning() {
        List<Journey> results = getSortedJourneysForTramThenWalk(Stations.Deansgate, nearAltrincham,
                TramTime.of(8, 0), false, 3);
        List<Journey> twoStageJourneys = results.stream().
                filter(journey -> journey.getStages().size() == 2).
                limit(3).collect(Collectors.toList());

        assertFalse(twoStageJourneys.isEmpty());
        Journey firstJourney = twoStageJourneys.get(0);
        TransportStage tramStage = firstJourney.getStages().get(0);
        TransportStage walkStage = firstJourney.getStages().get(1);

        assertTrue(walkStage.getFirstDepartureTime().isAfter(tramStage.getExpectedArrivalTime()));
        assertEquals(Stations.Deansgate.forDTO(), tramStage.getFirstStation().forDTO());
        assertEquals(Stations.NavigationRoad.forDTO(), tramStage.getLastStation().forDTO());
        assertEquals(Stations.NavigationRoad.forDTO(), walkStage.getFirstStation().forDTO());
        assertEquals("MyLocationPlaceholderId", walkStage.getLastStation().forDTO());
        assertEquals(Stations.NavigationRoad.forDTO(), walkStage.getActionStation().forDTO());
    }

    @Test
    void shouldFindJourneyWithWalkingEarlyMorning() {
        Set<Journey> results = getJourneysForWalkThenTram(nearAltrincham, Stations.Deansgate,
                TramTime.of(8, 0), false, 2);

        assertFalse(results.isEmpty());
        results.forEach(journey -> assertEquals(2, journey.getStages().size()));
        results.forEach(journey -> assertEquals(TransportMode.Walk, journey.getStages().get(0).getMode()));

        results.forEach(result -> {
            assertTrue(result.getPath().size()==11 || result.getPath().size()==12);
        });

        // via nav road
        results.stream().filter(journey -> journey.getPath().size()==11).forEach(journey -> {
            List<Location> callingPoints = journey.getPath();
            assertEquals(nearAltrincham, callingPoints.get(0).getLatLong());
            assertEquals(Stations.NavigationRoad, callingPoints.get(1));
            assertEquals(Stations.Deansgate, callingPoints.get(10));
        });

        // via alty
        results.stream().filter(journey -> journey.getPath().size()==12).forEach(journey -> {
            List<Location> callingPoints = journey.getPath();
            assertEquals(nearAltrincham, callingPoints.get(0).getLatLong());
            assertEquals(Stations.Altrincham, callingPoints.get(1));
            assertEquals(Stations.Deansgate, callingPoints.get(11));
        });
    }

    @Test
    void shouldFindJourneyWithWalkingEarlyMorningArriveBy() {
        TramTime queryTime = TramTime.of(8, 0);
        Set<Journey> results = getJourneysForWalkThenTram(nearAltrincham, Stations.Deansgate,
                queryTime, true, 2);

        assertFalse(results.isEmpty());
        results.forEach(journey -> assertTrue(journey.getQueryTime().isBefore(queryTime)));
    }

    @Test
    void shouldFindJourneyWithWalkingAtEndEarlyMorningArriveBy() {
        TramTime queryTime = TramTime.of(8, 0);
        List<Journey> results = getSortedJourneysForTramThenWalk(Stations.Deansgate, nearAltrincham,
                queryTime, true, 3);

        assertFalse(results.isEmpty());
        results.forEach(journey -> assertTrue(journey.getQueryTime().isBefore(queryTime)));

        results.forEach(journey -> {
            List<Location> callingPoints = journey.getPath();
            assertEquals(11, callingPoints.size());
            assertEquals(Stations.Deansgate, callingPoints.get(0));
            assertEquals(Stations.NavigationRoad, callingPoints.get(9));
            assertEquals(nearAltrincham, callingPoints.get(10).getLatLong());
        });
    }

    @Test
    void shouldFindJourneyWithWalkingDirectAtEndNearShudehill() {
        TramTime queryTime = TramTime.of(8, 30);
        List<Journey> results = getSortedJourneysForTramThenWalk(Stations.Shudehill, nearShudehill,
                queryTime, false, 3);
        assertFalse(results.isEmpty());

        results.forEach(journey -> {
            List<Location> callingPoints = journey.getPath();
            assertEquals(2, callingPoints.size());
            assertEquals(Stations.Shudehill, callingPoints.get(0));
            assertEquals(nearShudehill, callingPoints.get(1).getLatLong());
        });
    }

    @Test
    void shouldFindJourneyWithWalkingAtEndDeansgateNearShudehill() {
        TramTime queryTime = TramTime.of(8, 35);
        List<Journey> results = getSortedJourneysForTramThenWalk(Stations.Altrincham, nearShudehill,
                queryTime, false, 3);

        assertFalse(results.isEmpty());

        // find the lowest cost journey, should be tram to shudehill and then a walk
        Journey lowestCostJourney = results.get(0);

        // 33 -> 39
        assertEquals(39, RouteCalculatorTest.costOfJourney(lowestCostJourney), lowestCostJourney.toString());

        List<TransportStage> stages = lowestCostJourney.getStages();
        assertTrue(stages.size() >= 2);

        // Post lock down, no direct trams

//        assertEquals(Stations.Shudehill, stages.get(0).getLastStation());
//        assertEquals(Stations.Shudehill, stages.get(1).getFirstStation());
        int lastStageIndex = stages.size() - 1;
        assertEquals(Stations.ExchangeSquare, stages.get(lastStageIndex-1).getLastStation());
        assertEquals(Stations.ExchangeSquare, stages.get(lastStageIndex).getFirstStation());
    }

    @Disabled("Temporary: trams finish at 2300")
    @Test
    void shouldFindJourneyWithWalkingEndOfDay() {
        Set<Journey> results = getJourneysForWalkThenTram(nearAltrincham, Stations.Deansgate,
                TramTime.of(23, 0), false, 2);
        assertFalse(results.isEmpty());
        results.forEach(journey -> assertEquals(2, journey.getStages().size()));
    }

    @Test
    void shouldFindWalkOnlyIfNearDestinationStationSingleStationWalk() {
        Set<Journey> results = getJourneysForWalkThenTram(nearPiccGardens, Stations.PiccadillyGardens,
                TramTime.of(9, 0), false, 2); //, new StationWalk(Stations.PiccadillyGardens, 3));
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
        TramServiceDate date = new TramServiceDate(when);

        return getJourneySet(new JourneyRequest(date, queryTime, arriveBy, maxChanges,
                testConfig.getMaxJourneyDuration()), latLong, destination);
    }

    private List<Journey> getSortedJourneysForTramThenWalk(Station start, LatLong latLong, TramTime queryTime, boolean arriveBy, int maxChanges) {
        TramServiceDate date = new TramServiceDate(when);

        Stream<Journey> journeyStream = planner.quickestRouteForLocation(txn, start, latLong,
                new JourneyRequest(date, queryTime, arriveBy, maxChanges, testConfig.getMaxJourneyDuration())).
                sorted(Comparator.comparingInt(RouteCalculatorTest::costOfJourney));
        List<Journey> journeyList = journeyStream.collect(Collectors.toList());
        journeyStream.close();
        return journeyList;

    }
}
