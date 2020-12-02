package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.domain.*;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.LocationJourneyPlannerTestFacade;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramStations;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.*;
import static org.junit.jupiter.api.Assertions.*;

class LocationJourneyPlannerTest {
    private static final int TXN_TIMEOUT = 5*60;

    private static Dependencies dependencies;
    private static GraphDatabase database;
    private static IntegrationTramTestConfig testConfig;

    private final LocalDate when = TestEnv.testDay();
    private Transaction txn;
    private LocationJourneyPlannerTestFacade planner;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
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
        StationRepository stationRepository = dependencies.get(StationRepository.class);
        planner = new LocationJourneyPlannerTestFacade(dependencies.get(LocationJourneyPlanner.class), stationRepository, txn);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveDirectWalkNearPiccadily() {
        TramServiceDate queryDate = new TramServiceDate(when);

        JourneyRequest journeyRequest = new JourneyRequest(queryDate, TramTime.of(9, 0), false,
                2, testConfig.getMaxJourneyDuration());
        Set<Journey> unsortedResults = getJourneySet(
                journeyRequest,
                nearPiccGardens, TramStations.PiccadillyGardens, 3);

        assertFalse(unsortedResults.isEmpty());
        unsortedResults.forEach(journey -> {
            List<TransportStage<?,?>> stages = journey.getStages();
            WalkingToStationStage first = (WalkingToStationStage) stages.get(0);
            assertEquals(nearPiccGardens, first.getFirstStation().getLatLong());
            assertEquals(TramStations.PiccadillyGardens.getId(), first.getLastStation().getId());
        });

        unsortedResults.forEach(journey -> {
            List<Location<?>> callingPoints = journey.getPath();
            assertEquals(2, callingPoints.size());
            assertEquals(nearPiccGardens, callingPoints.get(0).getLatLong());
            assertEquals(TramStations.PiccadillyGardens.getId(), callingPoints.get(1).getId());
        });
    }

    private Set<Journey> getJourneySet(JourneyRequest journeyRequest, LatLong nearPiccGardens, TramStations dest, int maxStages) {
        return planner.quickestRouteForLocation(nearPiccGardens, dest, journeyRequest, maxStages);
    }

    @Test
    void shouldHaveDirectWalkFromPiccadily() {
        TramServiceDate queryDate = new TramServiceDate(when);

        JourneyRequest journeyRequest = new JourneyRequest(queryDate, TramTime.of(9, 0),
                false, 1, testConfig.getMaxJourneyDuration());
        Set<Journey> unsortedResults = planner.quickestRouteForLocation(TramStations.PiccadillyGardens,
                nearPiccGardens, journeyRequest, 2);

        assertFalse(unsortedResults.isEmpty());
        unsortedResults.forEach(journey -> {
            List<TransportStage<?,?>> stages = journey.getStages();
            WalkingFromStationStage first = (WalkingFromStationStage) stages.get(0);
            assertEquals(TramStations.PiccadillyGardens.getId(), first.getFirstStation().getId());
            assertEquals(nearPiccGardens, first.getLastStation().getLatLong());
        });
    }

    @Test
    void shouldFindJourneyWithWalkingAtEndEarlyMorning() {
        List<Journey> results = getSortedJourneysForTramThenWalk(TramStations.Deansgate, nearAltrincham,
                TramTime.of(8, 0), false, 3);
        List<Journey> twoStageJourneys = results.stream().
                filter(journey -> journey.getStages().size() == 2).
                limit(3).collect(Collectors.toList());

        assertFalse(twoStageJourneys.isEmpty());
        Journey firstJourney = twoStageJourneys.get(0);
        TransportStage<?,?> tramStage = firstJourney.getStages().get(0);
        TransportStage<?,?> walkStage = firstJourney.getStages().get(1);

        assertTrue(walkStage.getFirstDepartureTime().isAfter(tramStage.getExpectedArrivalTime()));
        assertEquals(TramStations.Deansgate.getId(), tramStage.getFirstStation().getId());
        assertEquals(TramStations.NavigationRoad.getId(), tramStage.getLastStation().getId());
        assertEquals(TramStations.NavigationRoad.getId(), walkStage.getFirstStation().getId());
        assertEquals("MyLocationPlaceholderId", walkStage.getLastStation().forDTO());
        assertEquals(TramStations.NavigationRoad.getId(), walkStage.getActionStation().getId());
    }

    @Test
    void shouldFindJourneyWithWalkingEarlyMorning() {
        Set<Journey> results = getJourneysForWalkThenTram(nearAltrincham, TramStations.Deansgate,
                TramTime.of(8, 0), false, 2);

        assertFalse(results.isEmpty());
        results.forEach(journey -> assertEquals(2, journey.getStages().size()));
        results.forEach(journey -> assertEquals(TransportMode.Walk, journey.getStages().get(0).getMode()));

        results.forEach(result -> assertTrue(result.getPath().size()==11 || result.getPath().size()==12));

        // via nav road
        results.stream().filter(journey -> journey.getPath().size()==11).forEach(journey -> {
            List<Location<?>> callingPoints = journey.getPath();
            assertEquals(nearAltrincham, callingPoints.get(0).getLatLong());
            assertEquals(TramStations.NavigationRoad.getId(), callingPoints.get(1).getId());
            assertEquals(TramStations.Deansgate.getId(), callingPoints.get(10).getId());
        });

        // via alty
        results.stream().filter(journey -> journey.getPath().size()==12).forEach(journey -> {
            List<Location<?>> callingPoints = journey.getPath();
            assertEquals(nearAltrincham, callingPoints.get(0).getLatLong());
            assertEquals(TramStations.Altrincham.getId(), callingPoints.get(1).getId());
            assertEquals(TramStations.Deansgate.getId(), callingPoints.get(11).getId());
        });
    }

    @Test
    void shouldFindJourneyWithWalkingEarlyMorningArriveBy() {
        TramTime queryTime = TramTime.of(8, 0);
        Set<Journey> results = getJourneysForWalkThenTram(nearAltrincham, TramStations.Deansgate,
                queryTime, true, 2);

        assertFalse(results.isEmpty());
        results.forEach(journey -> assertTrue(journey.getQueryTime().isBefore(queryTime)));
    }

    @Test
    void shouldFindJourneyWithWalkingAtEndEarlyMorningArriveBy() {
        TramTime queryTime = TramTime.of(8, 0);
        List<Journey> results = getSortedJourneysForTramThenWalk(TramStations.Deansgate, nearAltrincham,
                queryTime, true, 3);

        assertFalse(results.isEmpty());
        results.forEach(journey -> assertTrue(journey.getQueryTime().isBefore(queryTime)));

        results.forEach(journey -> {
            List<Location<?>> callingPoints = journey.getPath();
            assertEquals(11, callingPoints.size());
            assertEquals(TramStations.Deansgate.getId(), callingPoints.get(0).getId());
            assertEquals(TramStations.NavigationRoad.getId(), callingPoints.get(9).getId());
            assertEquals(nearAltrincham, callingPoints.get(10).getLatLong());
        });
    }

    @Test
    void shouldFindJourneyWithWalkingDirectAtEndNearShudehill() {
        TramTime queryTime = TramTime.of(8, 30);
        List<Journey> results = getSortedJourneysForTramThenWalk(TramStations.Shudehill, nearShudehill,
                queryTime, false, 3);
        assertFalse(results.isEmpty());

        results.forEach(journey -> {
            List<Location<?>> callingPoints = journey.getPath();
            assertEquals(2, callingPoints.size());
            assertEquals(TramStations.Shudehill.getId(), callingPoints.get(0).getId());
            assertEquals(nearShudehill, callingPoints.get(1).getLatLong());
        });
    }

    @Test
    void shouldFindJourneyWithWalkingAtEndDeansgateNearShudehill() {
        TramTime queryTime = TramTime.of(8, 35);
        List<Journey> results = getSortedJourneysForTramThenWalk(TramStations.Altrincham, nearShudehill,
                queryTime, false, 3);

        assertFalse(results.isEmpty());

        // find the lowest cost journey, should be tram to shudehill and then a walk
        Journey lowestCostJourney = results.get(0);

        // 33 -> 39
        assertEquals(39, RouteCalculatorTest.costOfJourney(lowestCostJourney), lowestCostJourney.toString());

        List<TransportStage<?,?>> stages = lowestCostJourney.getStages();
        assertTrue(stages.size() >= 2);

        // Post lock down, no direct trams

//        assertEquals(Stations.Shudehill, stages.get(0).getLastStation());
//        assertEquals(Stations.Shudehill, stages.get(1).getFirstStation());
        int lastStageIndex = stages.size() - 1;
        List<IdFor<Station>> nearStationIds = Arrays.asList(TramStations.Shudehill.getId(), TramStations.ExchangeSquare.getId());
        assertTrue(nearStationIds.contains(stages.get(lastStageIndex-1).getLastStation().getId()));
        assertTrue(nearStationIds.contains(stages.get(lastStageIndex).getFirstStation().getId()));
    }

    @Test
    void shouldFindJourneyWithWalkingEndOfDay() {
        Set<Journey> results = getJourneysForWalkThenTram(nearAltrincham, TramStations.Deansgate,
                TramTime.of(23, 0), false, 2);
        assertFalse(results.isEmpty());
        results.forEach(journey -> assertEquals(2, journey.getStages().size()));
    }

    @Test
    void shouldFindWalkOnlyIfNearDestinationStationSingleStationWalk() {
        Set<Journey> results = getJourneysForWalkThenTram(nearPiccGardens, TramStations.PiccadillyGardens,
                TramTime.of(9, 0), false, 2); //, new StationWalk(Stations.PiccadillyGardens, 3));
        assertFalse(results.isEmpty());
        results.forEach(journey-> {
            assertEquals(1,journey.getStages().size());
            TransportStage<?,?> rawStage = journey.getStages().get(0);
            assertEquals(TransportMode.Walk, rawStage.getMode());
            assertEquals(TramStations.PiccadillyGardens.getId(), rawStage.getLastStation().getId());
            assertEquals(nearPiccGardens, rawStage.getFirstStation().getLatLong());
            assertEquals(3, rawStage.getDuration());
        });
    }

    private Set<Journey> getJourneysForWalkThenTram(LatLong latLong, TramStations destination, TramTime queryTime, boolean arriveBy, int maxChanges) {
        TramServiceDate date = new TramServiceDate(when);

        return getJourneySet(new JourneyRequest(date, queryTime, arriveBy, maxChanges,
                testConfig.getMaxJourneyDuration()), latLong, destination, maxChanges+1);
    }

    private List<Journey> getSortedJourneysForTramThenWalk(TramStations start, LatLong latLong, TramTime queryTime, boolean arriveBy, int maxChanges) {
        TramServiceDate date = new TramServiceDate(when);

        Set<Journey> journeySet = planner.quickestRouteForLocation(start, latLong,
                new JourneyRequest(date, queryTime, arriveBy, maxChanges, testConfig.getMaxJourneyDuration()), maxChanges+1);

        List<Journey> journeyList = new LinkedList<>(journeySet);
        journeyList.sort(Comparator.comparingInt(RouteCalculatorTest::costOfJourney));
        return journeyList;

    }
}
