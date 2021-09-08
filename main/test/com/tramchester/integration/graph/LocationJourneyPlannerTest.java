package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.WalkingFromStationStage;
import com.tramchester.domain.transportStages.WalkingToStationStage;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.LocationJourneyPlannerTestFacade;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

class LocationJourneyPlannerTest {
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static IntegrationTramTestConfig testConfig;

    private final LocalDate when = TestEnv.testDay();
    private Transaction txn;
    private LocationJourneyPlannerTestFacade planner;
    private TramServiceDate date;
    private int maxJourneyDuration;
    private long maxNumberOfJourneys;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationTramTestConfig();
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
        date = new TramServiceDate(when);
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        planner = new LocationJourneyPlannerTestFacade(componentContainer.get(LocationJourneyPlanner.class), stationRepository, txn);
        maxNumberOfJourneys = 3;
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveDirectWalkNearPiccadily() {
        TramServiceDate queryDate = new TramServiceDate(when);

        JourneyRequest journeyRequest = new JourneyRequest(queryDate, TramTime.of(9, 0), false,
                0, maxJourneyDuration, maxNumberOfJourneys);
        Set<Journey> unsortedResults = planner.quickestRouteForLocation(nearPiccGardens, PiccadillyGardens,
                journeyRequest, 3);

        assertFalse(unsortedResults.isEmpty(),"no results");

        unsortedResults.forEach(journey -> {
            List<TransportStage<?,?>> stages = journey.getStages();
            WalkingToStationStage first = (WalkingToStationStage) stages.get(0);
            assertEquals(nearPiccGardens, first.getFirstStation().getLatLong());
            assertEquals(PiccadillyGardens.getId(), first.getLastStation().getId());
        });

        unsortedResults.forEach(journey -> {
            List<Location<?>> callingPoints = journey.getPath();
            assertEquals(2, callingPoints.size());
            assertEquals(nearPiccGardens, callingPoints.get(0).getLatLong());
            assertEquals(PiccadillyGardens.getId(), callingPoints.get(1).getId());
        });
    }

    @Test
    void shouldHaveDirectWalkFromPiccadily() {
        TramServiceDate queryDate = new TramServiceDate(when);

        JourneyRequest journeyRequest = new JourneyRequest(queryDate, TramTime.of(9, 0),
                false, 1, maxJourneyDuration, maxNumberOfJourneys);
        Set<Journey> unsortedResults = planner.quickestRouteForLocation(PiccadillyGardens,
                nearPiccGardens, journeyRequest, 2);

        assertFalse(unsortedResults.isEmpty());
        unsortedResults.forEach(journey -> {
            List<TransportStage<?,?>> stages = journey.getStages();
            WalkingFromStationStage first = (WalkingFromStationStage) stages.get(0);
            assertEquals(PiccadillyGardens.getId(), first.getFirstStation().getId());
            assertEquals(nearPiccGardens, first.getLastStation().getLatLong());
        });
    }

    @Test
    void shouldFindJourneyWithWalkingAtEndEarlyMorning() {
        final JourneyRequest request = new JourneyRequest(date, TramTime.of(8, 0), false,
                3, maxJourneyDuration, 1);
        //request.setDiag(true);
        final TramStations walkChangeStation = NavigationRoad;
        final TramStations start = TraffordBar;

        Set<Journey> journeySet = planner.quickestRouteForLocation(start, nearAltrincham, request, 2);
        assertFalse(journeySet.isEmpty());

        List<Journey> journeyList = sortByCost(journeySet);

        List<Journey> twoStageJourneys = journeyList.stream().
                filter(journey -> journey.getStages().size() == 2).
                limit(3).collect(Collectors.toList());

        assertFalse(twoStageJourneys.isEmpty());
        Journey firstJourney = twoStageJourneys.get(0);
        TransportStage<?,?> tramStage = firstJourney.getStages().get(0);
        TransportStage<?,?> walkStage = firstJourney.getStages().get(1);

        assertTrue(walkStage.getFirstDepartureTime().isAfter(tramStage.getExpectedArrivalTime()), walkStage.toString());
        assertEquals(start.getId(), tramStage.getFirstStation().getId());
        assertEquals(walkChangeStation.getId(), tramStage.getLastStation().getId());
        assertEquals(walkChangeStation.getId(), walkStage.getFirstStation().getId());
        assertEquals("MyLocationPlaceholderId", walkStage.getLastStation().forDTO());
        assertEquals(walkChangeStation.getId(), walkStage.getActionStation().getId());
    }

    @Test
    void shouldFindJourneyWithWalkingEarlyMorning() {

        final JourneyRequest request = new JourneyRequest(new TramServiceDate(when), TramTime.of(8, 0),
                false, 2, maxJourneyDuration, maxNumberOfJourneys);
        Set<Journey> results = planner.quickestRouteForLocation(nearAltrincham, Deansgate, request, 3);

        assertFalse(results.isEmpty());
        results.forEach(journey -> assertEquals(2, journey.getStages().size()));
        results.forEach(journey -> assertEquals(TransportMode.Walk, journey.getStages().get(0).getMode()));

        results.forEach(result -> assertTrue(result.getPath().size()==11 || result.getPath().size()==12));

        // via nav road
        results.stream().filter(journey -> journey.getPath().size()==11).forEach(journey -> {
            List<Location<?>> callingPoints = journey.getPath();
            assertEquals(nearAltrincham, callingPoints.get(0).getLatLong());
            assertEquals(NavigationRoad.getId(), callingPoints.get(1).getId());
            assertEquals(Deansgate.getId(), callingPoints.get(10).getId());
        });

        // via alty
        results.stream().filter(journey -> journey.getPath().size()==12).forEach(journey -> {
            List<Location<?>> callingPoints = journey.getPath();
            assertEquals(nearAltrincham, callingPoints.get(0).getLatLong());
            assertEquals(TramStations.Altrincham.getId(), callingPoints.get(1).getId());
            assertEquals(Deansgate.getId(), callingPoints.get(11).getId());
        });
    }

    @Test
    void shouldFindJourneyWithWalkingEarlyMorningArriveBy() {
        TramTime queryTime = TramTime.of(8, 0);

        final JourneyRequest request = new JourneyRequest(new TramServiceDate(when), queryTime, true, 3,
                maxJourneyDuration, maxNumberOfJourneys);
        Set<Journey> results = planner.quickestRouteForLocation(nearAltrincham, Deansgate, request, 3);

        assertFalse(results.isEmpty());
        results.forEach(journey -> assertTrue(journey.getQueryTime().isBefore(queryTime)));
    }

    @Test
    void shouldFindJourneyWithWalkingAtEndEarlyMorningArriveBy() {
        TramTime queryTime = TramTime.of(8, 0);
        final JourneyRequest request = new JourneyRequest(date, queryTime, true, 2,
                maxJourneyDuration, 1);

        Set<Journey> journeySet = planner.quickestRouteForLocation(Deansgate, nearAltrincham, request, 3);
        List<Journey> journeyList = sortByCost(journeySet);

        assertFalse(journeyList.isEmpty());
        journeyList.forEach(journey -> assertTrue(journey.getQueryTime().isBefore(queryTime)));

        journeyList.forEach(journey -> {
            List<Location<?>> callingPoints = journey.getPath();
            assertEquals(Deansgate.getId(), callingPoints.get(0).getId());
            final int numCallingPoints = 11;
            assertEquals(numCallingPoints, callingPoints.size());
            assertEquals(NavigationRoad.getId(), callingPoints.get(numCallingPoints-2).getId());
            assertEquals(nearAltrincham, callingPoints.get(numCallingPoints-1).getLatLong());
        });
    }

    @Test
    void shouldFindJourneyWithWalkingDirectAtEndNearShudehill() {
        TramTime queryTime = TramTime.of(8, 30);
        final JourneyRequest request = new JourneyRequest(date, queryTime, false, 3, maxJourneyDuration, maxNumberOfJourneys);

        Set<Journey> journeySet = planner.quickestRouteForLocation(TramStations.Shudehill, nearShudehill, request, 4);

        List<Journey> journeyList = sortByCost(journeySet);

        assertFalse(journeyList.isEmpty());

        journeyList.forEach(journey -> {
            List<Location<?>> callingPoints = journey.getPath();
            assertEquals(2, callingPoints.size());
            assertEquals(TramStations.Shudehill.getId(), callingPoints.get(0).getId());
            assertEquals(nearShudehill, callingPoints.get(1).getLatLong());
        });
    }

    @Test
    void shouldFindJourneyWithWalkingAtEndDeansgateNearShudehill() {
        TramTime queryTime = TramTime.of(8, 35);
        final JourneyRequest request = new JourneyRequest(date, queryTime, false, 2,
                maxJourneyDuration, maxNumberOfJourneys);

        Set<Journey> journeySet = planner.quickestRouteForLocation(Altrincham, nearShudehill, request, 3);

        List<Journey> journeyList = sortByCost(journeySet);

        assertFalse(journeyList.isEmpty());

        // find the lowest cost journey, should be tram to shudehill and then a walk
        Journey lowestCostJourney = journeyList.get(0);

        assertEquals(33, RouteCalculatorTest.costOfJourney(lowestCostJourney), journeySet.toString());

        List<TransportStage<?,?>> stages = lowestCostJourney.getStages();
        assertTrue(stages.size() >= 2);

        int lastStageIndex = stages.size() - 1;
        List<IdFor<Station>> nearStationIds = Arrays.asList(Shudehill.getId(), ExchangeSquare.getId());
        assertTrue(nearStationIds.contains(stages.get(lastStageIndex-1).getLastStation().getId()));
        assertTrue(nearStationIds.contains(stages.get(lastStageIndex).getFirstStation().getId()));
    }

    @Test
    void shouldFindJourneyWithWalkingEndOfDay() {
        final JourneyRequest request = new JourneyRequest(new TramServiceDate(when), TramTime.of(23, 0),
                false, 2, maxJourneyDuration, maxNumberOfJourneys);

        Set<Journey> results = planner.quickestRouteForLocation(nearAltrincham, Deansgate, request, 3);
        assertFalse(results.isEmpty());
        results.forEach(journey -> assertEquals(2, journey.getStages().size()));
    }

    @Test
    void shouldHaveNearAltyToDeansgate() {
        // mirrors test in AppUserJourneyLocationsTest

        // TODO Need to increase maxNumberJourneys at start so we try all walks at the beginning??
        final JourneyRequest request = new JourneyRequest(new TramServiceDate(when), TramTime.of(10, 15),
                false, 1, maxJourneyDuration, this.maxNumberOfJourneys);

        Set<Journey> unsorted = planner.quickestRouteForLocation(nearAltrincham, Deansgate, request, 2);
        assertFalse(unsorted.isEmpty());

        List<Journey> sorted = unsorted.stream().
                sorted(Comparator.comparing(Journey::getDepartTime)).collect(Collectors.toList());

        Journey earliestDepart = sorted.get(0);
        assertEquals(TramTime.of(10,19), earliestDepart.getDepartTime());
    }

    @Test
    void shouldFindWalkOnlyIfNearDestinationStationSingleStationWalk() {
        final JourneyRequest request = new JourneyRequest(new TramServiceDate(when), TramTime.of(9, 0),
                false, 0, maxJourneyDuration, maxNumberOfJourneys);

        // set max stages to 1, because there is another path via walk to market street and then tram
        Set<Journey> results = planner.quickestRouteForLocation(nearPiccGardens, PiccadillyGardens, request, 1);
        assertFalse(results.isEmpty(),"no results");

        results.forEach(journey-> {
//            assertEquals(1, journey.getStages().size(), journey.getStages().toString());
            TransportStage<?,?> rawStage = journey.getStages().get(0);
            assertEquals(TransportMode.Walk, rawStage.getMode());
            assertEquals(PiccadillyGardens.getId(), rawStage.getLastStation().getId());
            assertEquals(nearPiccGardens, rawStage.getFirstStation().getLatLong());
            assertEquals(3, rawStage.getDuration());
        });
    }

    @NotNull
    private List<Journey> sortByCost(Set<Journey> journeySet) {
        List<Journey> journeyList = new LinkedList<>(journeySet);
        journeyList.sort(Comparator.comparingInt(RouteCalculatorTest::costOfJourney));
        return journeyList;
    }
}
