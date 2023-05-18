package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.WalkingFromStationStage;
import com.tramchester.domain.transportStages.WalkingToStationStage;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.LocationJourneyPlannerTestFacade;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.TramsOnly;
import static com.tramchester.testSupport.TestEnv.assertMinutesEquals;
import static com.tramchester.testSupport.reference.KnownLocations.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

class LocationJourneyPlannerTest {
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static IntegrationTramTestConfig testConfig;

    private final TramDate when = TestEnv.testDay();
    private Transaction txn;
    private LocationJourneyPlannerTestFacade planner;
    private TramDate date;
    private Duration maxJourneyDuration;
    private long maxNumberOfJourneys;
    private StationRepository stationRepository;

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
        maxJourneyDuration = Duration.ofMinutes(testConfig.getMaxJourneyDuration());
        date = when;
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        stationRepository = componentContainer.get(StationRepository.class);
        planner = new LocationJourneyPlannerTestFacade(componentContainer.get(LocationJourneyPlanner.class), stationRepository, txn);
        maxNumberOfJourneys = 3;
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveDirectWalkNearPiccadillyGardens() {

        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(9, 0), false,
                0, maxJourneyDuration, maxNumberOfJourneys, getRequestedModes());
        Set<Journey> unsortedResults = planner.quickestRouteForLocation(nearPiccGardens, PiccadillyGardens,
                journeyRequest, 3);

        assertFalse(unsortedResults.isEmpty(),"no results");

        unsortedResults.forEach(journey -> {
            List<TransportStage<?,?>> stages = journey.getStages();
            WalkingToStationStage first = (WalkingToStationStage) stages.get(0);
            assertEquals(nearPiccGardens.latLong(), first.getFirstStation().getLatLong());
            assertEquals(PiccadillyGardens.getId(), first.getLastStation().getId());
        });

        unsortedResults.forEach(journey -> {
            List<Location<?>> callingPoints = journey.getPath();
            assertEquals(2, callingPoints.size());
            assertEquals(nearPiccGardens.latLong(), callingPoints.get(0).getLatLong());
            assertEquals(PiccadillyGardens.getId(), callingPoints.get(1).getId());
        });
    }

    private EnumSet<TransportMode> getRequestedModes() {
        return TramsOnly;
    }

    @Test
    void shouldHaveDirectWalkFromPiccadily() {

        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(9, 0),
                false, 1, maxJourneyDuration, maxNumberOfJourneys, getRequestedModes());
        Set<Journey> unsortedResults = planner.quickestRouteForLocation(PiccadillyGardens, nearPiccGardens, journeyRequest, 2);

        assertFalse(unsortedResults.isEmpty());
        unsortedResults.forEach(journey -> {
            List<TransportStage<?,?>> stages = journey.getStages();
            WalkingFromStationStage first = (WalkingFromStationStage) stages.get(0);
            assertEquals(PiccadillyGardens.getId(), first.getFirstStation().getId());
            assertEquals(nearPiccGardens.latLong(), first.getLastStation().getLatLong());
        });
    }

    @Test
    void shouldFindJourneyWithWalkingAtEndEarlyMorning() {
        final JourneyRequest request = new JourneyRequest(date, TramTime.of(8, 0), false,
                3, maxJourneyDuration, 1, getRequestedModes());
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

        assertTrue(walkStage.getFirstDepartureTime().isAfterOrSame(tramStage.getExpectedArrivalTime()), walkStage.toString());
        assertEquals(start.getId(), tramStage.getFirstStation().getId());
        assertEquals(walkChangeStation.getId(), tramStage.getLastStation().getId());
        assertEquals(walkChangeStation.getId(), walkStage.getFirstStation().getId());
        assertEquals(new IdForDTO("53.387483,-2.351463"), IdForDTO.createFor(walkStage.getLastStation()));
        assertEquals(nearAltrincham.latLong(), walkStage.getLastStation().getLatLong());
        assertEquals(walkChangeStation.getId(), walkStage.getActionStation().getId());
    }

    @Test
    void shouldFindJourneyWithWalkingEarlyMorning() {

        final JourneyRequest request = new JourneyRequest(when, TramTime.of(8, 0),
                false, 2, maxJourneyDuration, maxNumberOfJourneys, getRequestedModes());
        Set<Journey> results = planner.quickestRouteForLocation(nearAltrincham, Deansgate, request, 3);

        assertFalse(results.isEmpty());
        results.forEach(journey -> assertEquals(2, journey.getStages().size()));
        results.forEach(journey -> assertEquals(TransportMode.Walk, journey.getStages().get(0).getMode()));

        results.forEach(result -> assertTrue(result.getPath().size()==11 || result.getPath().size()==12));

        // via nav road
        results.stream().filter(journey -> journey.getPath().size()==11).forEach(journey -> {
            List<Location<?>> callingPoints = journey.getPath();
            assertEquals(nearAltrincham.latLong(), callingPoints.get(0).getLatLong());
            assertEquals(NavigationRoad.getId(), callingPoints.get(1).getId());
            assertEquals(Deansgate.getId(), callingPoints.get(10).getId());
        });

        // via alty
        results.stream().filter(journey -> journey.getPath().size()==12).forEach(journey -> {
            List<Location<?>> callingPoints = journey.getPath();
            assertEquals(nearAltrincham.latLong(), callingPoints.get(0).getLatLong());
            assertEquals(TramStations.Altrincham.getId(), callingPoints.get(1).getId());
            assertEquals(Deansgate.getId(), callingPoints.get(11).getId());
        });
    }

    @Test
    void shouldFindJourneyWithWalkingEarlyMorningArriveBy() {
        TramTime queryTime = TramTime.of(8, 0);

        final JourneyRequest request = new JourneyRequest(when, queryTime, true, 3,
                maxJourneyDuration, maxNumberOfJourneys, getRequestedModes());
        Set<Journey> results = planner.quickestRouteForLocation(nearAltrincham, Deansgate, request, 3);

        assertFalse(results.isEmpty());
        results.forEach(journey -> assertTrue(journey.getQueryTime().isBefore(queryTime)));
    }

    @Test
    void shouldFindJourneyWithWalkingAtEndEarlyMorningArriveBy() {
        TramTime queryTime = TramTime.of(8, 0);
        final JourneyRequest request = new JourneyRequest(date, queryTime, true, 2,
                maxJourneyDuration, 1, getRequestedModes());

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
            assertEquals(nearAltrincham.latLong(), callingPoints.get(numCallingPoints-1).getLatLong());
        });
    }

    @Test
    void shouldFindJourneyWithWalkingDirectAtEndNearShudehill() {
        TramTime queryTime = TramTime.of(8, 30);
        final JourneyRequest request = new JourneyRequest(date, queryTime, false, 3, maxJourneyDuration, maxNumberOfJourneys, getRequestedModes());

        Set<Journey> journeySet = planner.quickestRouteForLocation(TramStations.Shudehill, nearShudehill, request, 4);

        List<Journey> journeyList = sortByCost(journeySet);

        assertFalse(journeyList.isEmpty());

        journeyList.forEach(journey -> {
            List<Location<?>> callingPoints = journey.getPath();
            assertEquals(2, callingPoints.size());
            assertEquals(TramStations.Shudehill.getId(), callingPoints.get(0).getId());
            assertEquals(nearShudehill.latLong(), callingPoints.get(1).getLatLong());
        });
    }

    @Test
    void shouldFindJourneyWithWalkingAtEndDeansgateNearShudehill() {
        TramTime queryTime = TramTime.of(8, 35);
        final JourneyRequest request = new JourneyRequest(date, queryTime, false, 2,
                maxJourneyDuration, maxNumberOfJourneys, getRequestedModes());

        Set<Journey> journeySet = planner.quickestRouteForLocation(Altrincham, nearShudehill, request, 3);

        List<Journey> journeyList = sortByCost(journeySet);

        assertFalse(journeyList.isEmpty());

        // find the lowest cost journey, should be tram to shudehill and then a walk
        Journey lowestCostJourney = journeyList.get(0);

        assertEquals(Duration.ofMinutes(33), RouteCalculatorTest.costOfJourney(lowestCostJourney), journeySet.toString());

        List<TransportStage<?,?>> stages = lowestCostJourney.getStages();
        assertTrue(stages.size() >= 2);

        int lastStageIndex = stages.size() - 1;
        List<IdFor<Station>> nearStationIds = Arrays.asList(Shudehill.getId(), ExchangeSquare.getId());
        assertTrue(nearStationIds.contains(stages.get(lastStageIndex-1).getLastStation().getId()));
        assertTrue(nearStationIds.contains(stages.get(lastStageIndex).getFirstStation().getId()));
    }

    @Test
    void shouldFindJourneyWithWalkingEndOfDay() {
        final JourneyRequest request = new JourneyRequest(when, TramTime.of(23, 0),
                false, 2, maxJourneyDuration, maxNumberOfJourneys, getRequestedModes());

        Set<Journey> results = planner.quickestRouteForLocation(nearAltrincham, Deansgate, request, 3);
        assertFalse(results.isEmpty());

        results.forEach(journey -> assertEquals(2, journey.getStages().size()));
    }

    @Test
    void shouldHaveNearAltyToDeansgate() {
        // mirrors test in AppUserJourneyLocationsTest

        final TramTime queryTime = TramTime.of(10, 15);

        final double walkingMPH = testConfig.getWalkingMPH();
        int toNavigation = TestEnv.calcCostInMinutes(nearAltrincham.location(), NavigationRoad.from(stationRepository), walkingMPH);
        int toAltrincham = TestEnv.calcCostInMinutes(nearAltrincham.location(), Altrincham.from(stationRepository), walkingMPH);

        int lowestCost = Math.min(toAltrincham, toNavigation);
        TramTime earliestDepart = queryTime.plusMinutes(lowestCost);

        final JourneyRequest request = new JourneyRequest(when, queryTime,
                false, 1, maxJourneyDuration, maxNumberOfJourneys, getRequestedModes());

        Set<Journey> unsorted = planner.quickestRouteForLocation(nearAltrincham, Deansgate, request, 2);
        assertFalse(unsorted.isEmpty());

        List<Journey> sorted = unsorted.stream().
                sorted(Comparator.comparing(Journey::getDepartTime)).collect(Collectors.toList());

        Journey earliestJourney = sorted.get(0);
        final TramTime actualDepartTime = earliestJourney.getDepartTime();
        assertTrue(actualDepartTime.isAfter(queryTime), actualDepartTime.toString());
        assertTrue(actualDepartTime.isAfter(earliestDepart) || actualDepartTime.equals(earliestDepart));
    }

    @Test
    void shouldFindWalkOnlyIfNearDestinationStationSingleStationWalk() {
        final JourneyRequest request = new JourneyRequest(when, TramTime.of(9, 0),
                false, 0, maxJourneyDuration, maxNumberOfJourneys, getRequestedModes());

        // set max stages to 1, because there is another path via walk to market street and then tram
        Set<Journey> results = planner.quickestRouteForLocation(nearPiccGardens, PiccadillyGardens, request, 1);
        assertFalse(results.isEmpty(),"no results");

        results.forEach(journey-> {
            TransportStage<?,?> rawStage = journey.getStages().get(0);
            assertEquals(TransportMode.Walk, rawStage.getMode());
            assertEquals(PiccadillyGardens.getId(), rawStage.getLastStation().getId());
            assertEquals(nearPiccGardens.latLong(), rawStage.getFirstStation().getLatLong());
            assertMinutesEquals(3, rawStage.getDuration());
        });
    }

    @NotNull
    private List<Journey> sortByCost(Set<Journey> journeySet) {
        List<Journey> journeyList = new LinkedList<>(journeySet);
        //Comparator.comparingInt(RouteCalculatorTest::costOfJourney)
        journeyList.sort(Comparator.comparing(RouteCalculatorTest::costOfJourney));
        return journeyList;
    }
}
