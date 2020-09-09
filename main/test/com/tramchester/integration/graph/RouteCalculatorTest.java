package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.domain.Journey;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.VehicleStage;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.tramchester.testSupport.TestEnv.DAYS_AHEAD;
import static com.tramchester.testSupport.TestEnv.avoidChristmasDate;
import static com.tramchester.testSupport.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
public class RouteCalculatorTest {

    // Note this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static Dependencies dependencies;
    private static GraphDatabase database;
    private static IntegrationTramTestConfig config;
    private final int maxChanges = 3;

    private RouteCalculatorTestFacade calculator;
    private final LocalDate when = TestEnv.testDay();
    private Transaction txn;
    private int maxJourneyDuration;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        dependencies = new Dependencies();
        config = new IntegrationTramTestConfig();
        dependencies.initialise(config);
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
        calculator = new RouteCalculatorTestFacade(dependencies.get(RouteCalculator.class), stationRepository, txn);
        maxJourneyDuration = config.getMaxJourneyDuration();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldReproIssueWithChangesVeloToTraffordBar() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false, maxChanges, maxJourneyDuration);
        assertGetAndCheckJourneys(journeyRequest, TramStations.VeloPark, TramStations.TraffordBar);
    }

    @Test
    void shouldHaveSimpleOneStopJourneyNextDays() {
        checkRouteNextNDays(TramStations.TraffordBar, Altrincham, when, TramTime.of(9,0), DAYS_AHEAD);
    }

    @Test
    void shouldHaveSimpleManyStopSameLineJourney() {
        checkRouteNextNDays(Altrincham, TramStations.Cornbrook, when, TramTime.of(9,0), DAYS_AHEAD);
    }

    @DataExpiryCategory
    @Test
    void shouldHaveSimpleManyStopJourneyViaInterchangeNDaysAhead() {
        checkRouteNextNDays(Altrincham, TramStations.Bury, when, TramTime.of(9,0), DAYS_AHEAD);
    }

    @Test
    void shouldHaveSimpleJourney() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(10, 15), false, maxChanges, maxJourneyDuration);
        Set<Journey> journeys = calculator.calculateRouteAsSet(Altrincham, TramStations.Deansgate, journeyRequest, 1);
        Set<Journey> results = checkJourneys(Altrincham, TramStations.Deansgate, journeyRequest.getTime(), journeyRequest.getDate(), journeys);

        results.forEach(journey -> {
            List<Location> callingPoints = journey.getPath();
            assertEquals(11, callingPoints.size());
            assertEquals(Altrincham.getId(), callingPoints.get(0).getId());
            assertEquals(TramStations.Deansgate.getId(), callingPoints.get(10).getId());
        });
    }

    @Test
    void shouldHaveReasonableJourneyAltyToDeansgate() {
        TramServiceDate tramServiceDate = new TramServiceDate(when);
        JourneyRequest request = new JourneyRequest(tramServiceDate, TramTime.of(10, 15), false, maxChanges, maxJourneyDuration);

        Set<Journey> results = calculator.calculateRouteAsSet(Altrincham, TramStations.Deansgate, request);

        assertFalse(results.isEmpty());
        results.forEach(journey -> {
            assertEquals(1, journey.getStages().size()); // should be one stage only
            journey.getStages().stream().
                    map(raw -> (VehicleStage) raw).
                    map(VehicleStage::getCost).
                    forEach(cost -> assertTrue(cost>0));
            Optional<Integer> total = journey.getStages().stream().
                    map(raw -> (VehicleStage) raw).
                    map(VehicleStage::getCost).
                    reduce(Integer::sum);
            assertTrue(total.isPresent());
            assertTrue(total.get()>20);
        });
    }

    @Disabled("Failing due to temporarily less frequency service")
    @Test
    void shouldUseAllRoutesCorrectlWhenMultipleRoutesServDestination() {
        TramServiceDate today = new TramServiceDate(when);

        TramStations start = Altrincham;

        TramTime queryTime = TramTime.of(10, 21);
        Set<Journey> servedByBothRoutes = calculateRoutes(start, TramStations.Deansgate, queryTime, today);

        Set<Journey> altyToPiccGardens = calculateRoutes(start, TramStations.PiccadillyGardens, queryTime, today);
        Set<Journey> altyToMarketStreet = calculateRoutes(start, TramStations.MarketStreet, queryTime, today);

        assertEquals(altyToPiccGardens.size()+altyToMarketStreet.size(), servedByBothRoutes.size());
    }

    // over max wait, catch failure to accumulate journey times correctly
    @Test
    void shouldHaveSimpleButLongJoruneySameRoute() {
        checkRouteNextNDays(TramStations.ManAirport, TramStations.TraffordBar, when, TramTime.of(9,0), 1);
    }

    @Test
    void shouldHaveLongJourneyAcross() {
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, maxChanges, maxJourneyDuration);
        assertGetAndCheckJourneys(journeyRequest, Altrincham, TramStations.Rochdale);
    }

    @Test
    void shouldHaveReasonableLongJourneyAcrossFromInterchange() {
        TramTime am8 = TramTime.of(8, 0);

        Set<Journey> journeys = calculateRoutes(TramStations.Monsall, TramStations.RochdaleRail, am8, new TramServiceDate(when));

        assertFalse(journeys.isEmpty());
        journeys.forEach(journey -> {
            // direct, or change at shaw
            assertTrue(journey.getStages().size()<=2);
        });
    }

    @Test
    void shouldHaveSimpleManyStopJourneyStartAtInterchange() {
        checkRouteNextNDays(TramStations.Cornbrook, TramStations.Bury, when, TramTime.of(9,0), 1);
    }

    @Test
    void shouldLimitNumberChangesResultsInNoJourneys() {
        TramServiceDate today = new TramServiceDate(TestEnv.LocalNow().toLocalDate());

        JourneyRequest request = new JourneyRequest(today, TramTime.of(11, 43), false, 0,
                maxJourneyDuration);
        Set<Journey> results = calculator.calculateRouteAsSet(Altrincham, TramStations.ManAirport, request);

        assertEquals(0, results.size());
    }

    @Test
    void testJourneyFromAltyToAirport() {
        TramServiceDate today = new TramServiceDate(TestEnv.LocalNow().toLocalDate());

        JourneyRequest request = new JourneyRequest(today, TramTime.of(11, 43), false, maxChanges,
                maxJourneyDuration);
        Set<Journey> results =  calculator.calculateRouteAsSet(Altrincham, TramStations.ManAirport, request);

        assertTrue(results.size()>0, "no results");    // results is iterator
        for (Journey result : results) {
            List<TransportStage> stages = result.getStages();
            assertEquals(2,stages.size());
            VehicleStage firstStage = (VehicleStage) stages.get(0);
            assertEquals(Altrincham.getId(), firstStage.getFirstStation().getId());
            assertEquals(TramStations.TraffordBar.getId(), firstStage.getLastStation().getId());
            assertEquals(TransportMode.Tram, firstStage.getMode());
            assertEquals(7, firstStage.getPassedStops());

            VehicleStage finalStage = (VehicleStage) stages.get(stages.size()-1);
            //assertEquals(Stations.TraffordBar, secondStage.getFirstStation()); // THIS CAN CHANGE
            assertEquals(TramStations.ManAirport.getId(), finalStage.getLastStation().getId());
            assertEquals(TransportMode.Tram, finalStage.getMode());
        }
    }

    @Disabled("Temporary: trams finish at 2300")
    @Test
    void shouldHandleCrossingMidnightWithChange() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(23,20), false, maxChanges, maxJourneyDuration);
        assertGetAndCheckJourneys(journeyRequest, TramStations.Cornbrook, TramStations.ManAirport);
    }

    @Disabled("Temporary: trams finish at 2300")
    @Test
    void shouldHandleCrossingMidnightDirect() {
        JourneyRequest journeyRequestA = new JourneyRequest(when, TramTime.of(23,55), false, maxChanges, maxJourneyDuration);
        assertGetAndCheckJourneys(journeyRequestA, TramStations.Cornbrook, TramStations.StPetersSquare);

        JourneyRequest journeyRequestB = new JourneyRequest(when, TramTime.of(23,51), false, maxChanges, maxJourneyDuration);
        assertGetAndCheckJourneys(journeyRequestB, Altrincham, TramStations.TraffordBar);
    }

    @Disabled("Temporary: trams finish at 2300")
    @Test
    void shouldHandleAfterMidnightDirect() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(0,0), false, maxChanges, maxJourneyDuration);
        assertGetAndCheckJourneys(journeyRequest, Altrincham, TramStations.NavigationRoad);
    }

    @Test
    void shouldHaveHeatonParkToBurtonRoad() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(7, 30), false, maxChanges, maxJourneyDuration);
        assertGetAndCheckJourneys(journeyRequest, HeatonPark, TramStations.BurtonRoad);
    }

    @Test
    void shouldReproIssueRochInterchangeToBury() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(9, 0), false, maxChanges, maxJourneyDuration);
        assertGetAndCheckJourneys(journeyRequest, TramStations.Rochdale, TramStations.Bury);
    }

    @Test
    void shouldReproIssueWithMediaCityTrams() {

        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(12, 0), false, maxChanges, maxJourneyDuration);

        assertGetAndCheckJourneys(journeyRequest, TramStations.StPetersSquare, TramStations.MediaCityUK);
        assertGetAndCheckJourneys(journeyRequest, TramStations.ExchangeSquare, TramStations.MediaCityUK);
    }


    public static int costOfJourney(Journey journey) {
        List<TransportStage> stages = journey.getStages();
        TramTime departs = stages.get(0).getFirstDepartureTime();
        TramTime arrive = stages.get(stages.size() - 1).getExpectedArrivalTime();

        return TramTime.diffenceAsMinutes(departs, arrive);
    }

    @Test
    void shouldCheckCornbrookToStPetersSquareOnSundayMorning() {
        JourneyRequest journeyRequest = new JourneyRequest(when.plusDays(maxChanges), TramTime.of(11, 0), false, maxChanges, maxJourneyDuration);
        assertGetAndCheckJourneys(journeyRequest, TramStations.Cornbrook, TramStations.StPetersSquare);
    }

    @Test
    void shouldNotGenerateDuplicateJourneys() {

        Set<List<TransportStage>> stages = new HashSet<>();

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), TramTime.of(11, 45), false,
                4, maxJourneyDuration);
        Set<Journey> journeys =  calculator.calculateRouteAsSet(Bury, Altrincham, request, 3);

        assertTrue(journeys.size()>0);

        journeys.forEach(journey -> {
            assertFalse(stages.contains(journey.getStages()), stages.toString());
            stages.add(journey.getStages());
        });

    }

    @Test
    void shouldHaveInAndAroundCornbrookToEccles8amTuesday() {
        // catches issue with services, only some of which go to media city, while others direct to broadway
        JourneyRequest journeyRequest8am = new JourneyRequest(when, TramTime.of(8,0), false, maxChanges, maxJourneyDuration);
        assertGetAndCheckJourneys(journeyRequest8am, TramStations.Cornbrook, TramStations.Broadway);
        assertGetAndCheckJourneys(journeyRequest8am, TramStations.Cornbrook, TramStations.Eccles);

        JourneyRequest journeyRequest9am = new JourneyRequest(when, TramTime.of(9,0), false, maxChanges, maxJourneyDuration);
        assertGetAndCheckJourneys(journeyRequest9am, TramStations.Cornbrook, TramStations.Broadway);
        assertGetAndCheckJourneys(journeyRequest9am, TramStations.Cornbrook, TramStations.Eccles);
    }

    @Test
    void shouldReproIssueWithJourneysToEccles() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(9,0), false, maxChanges, maxJourneyDuration);

        assertGetAndCheckJourneys(journeyRequest, TramStations.Bury, TramStations.Broadway);
        assertGetAndCheckJourneys(journeyRequest, TramStations.Bury, TramStations.Eccles);
    }

    @Test
    void reproduceIssueEdgePerTrip() {
        // see also RouteCalculatorSubGraphTest
        JourneyRequest journeyRequestA = new JourneyRequest(when, TramTime.of(19,48), false, maxChanges, maxJourneyDuration);
        assertGetAndCheckJourneys(journeyRequestA, TramStations.PiccadillyGardens, TramStations.Pomona);

        JourneyRequest journeyRequestB = new JourneyRequest(when, TramTime.of(19,51), false, maxChanges, maxJourneyDuration);
        assertGetAndCheckJourneys(journeyRequestB, TramStations.StPetersSquare, TramStations.Pomona);

        JourneyRequest journeyRequestC = new JourneyRequest(when, TramTime.of(19,56), false, maxChanges, maxJourneyDuration);
        assertGetAndCheckJourneys(journeyRequestC, TramStations.StPetersSquare, TramStations.Pomona);

        JourneyRequest journeyRequestD = new JourneyRequest(when, TramTime.of(6,10), false, maxChanges, maxJourneyDuration);
        assertGetAndCheckJourneys(journeyRequestD, TramStations.Cornbrook, TramStations.Eccles);
    }

    @Test
    void shouldReproIssueWithStPetersToBeyondEcclesAt8AM() {
        List<TramTime> missingTimes = checkRangeOfTimes(TramStations.Cornbrook, TramStations.Eccles);
        assertTrue(missingTimes.isEmpty(), missingTimes.toString());
    }

    @Test
    void reproduceIssueWithImmediateDepartOffABoardedTram() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false, maxChanges, maxJourneyDuration);
        assertGetAndCheckJourneys(journeyRequest, TramStations.Deansgate, TramStations.Ashton);
    }

    @Test
    void reproduceIssueWithTramsSundayStPetersToDeansgate() {
        JourneyRequest journeyRequest = new JourneyRequest(TestEnv.nextSunday(), TramTime.of(9,0), false, maxChanges, maxJourneyDuration);
        assertGetAndCheckJourneys(journeyRequest, TramStations.StPetersSquare, TramStations.Deansgate);
    }

    @Test
    void reproduceIssueWithTramsSundayAshtonToEccles() {
        JourneyRequest journeyRequest = new JourneyRequest(TestEnv.nextSunday(), TramTime.of(9,0), false, maxChanges, maxJourneyDuration);
        assertGetAndCheckJourneys(journeyRequest, TramStations.Ashton, TramStations.Eccles);
    }

    @Test
    void reproduceIssueWithTramsSundayToFromEcclesAndCornbrook() {
        JourneyRequest journeyRequest = new JourneyRequest(TestEnv.nextSunday(), TramTime.of(9,0), false, maxChanges, maxJourneyDuration);

        assertGetAndCheckJourneys(journeyRequest, TramStations.Cornbrook, TramStations.Eccles);
        assertGetAndCheckJourneys(journeyRequest, TramStations.Eccles, TramStations.Cornbrook);
    }

    @Test
    void shouldReproduceIssueCornbrookToAshtonSatursdays() {
        JourneyRequest journeyRequest = new JourneyRequest(TestEnv.nextSaturday(), TramTime.of(9,0), false, maxChanges, maxJourneyDuration);
        assertGetAndCheckJourneys(journeyRequest, Cornbrook, Ashton);
    }

    @Test
    void shouldFindRouteVeloToHoltTownAt8RangeOfTimes() {
        for(int i=0; i<60; i++) {
            TramTime time = TramTime.of(8,i);
            JourneyRequest journeyRequest = new JourneyRequest(when, time, false, maxChanges, maxJourneyDuration);
            assertGetAndCheckJourneys(journeyRequest, TramStations.VeloPark, TramStations.HoltTown);
        }
    }

    @Test
    void reproIssueRochdaleToEccles() {
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, maxChanges, maxJourneyDuration);
        assertGetAndCheckJourneys(journeyRequest, TramStations.Rochdale, TramStations.Eccles);
    }

    private void checkRouteNextNDays(TramStations start, TramStations dest, LocalDate date, TramTime time, int numDays) {
        if (!dest.equals(start)) {
            for(int day = 0; day< numDays; day++) {
                LocalDate testDate = avoidChristmasDate(date.plusDays(day));
                JourneyRequest journeyRequest = new JourneyRequest(testDate, time, false, maxChanges, maxJourneyDuration);
                assertGetAndCheckJourneys(journeyRequest, start, dest);
            }
        }
    }

    private void assertGetAndCheckJourneys(JourneyRequest journeyRequest, TramStations start, TramStations dest) {
        Set<Journey> journeys = calculator.calculateRouteAsSet(start, dest, journeyRequest, 1);
        checkJourneys(start, dest, journeyRequest.getTime(), journeyRequest.getDate(), journeys);
    }

    @NotNull
    private Set<Journey> checkJourneys(TramStations start, TramStations dest, TramTime time, TramServiceDate date, Set<Journey> journeys) {
        String message = "from " + start.getId() + " to " + dest.getId() + " at " + time + " on " + date;
        assertTrue(journeys.size() > 0, "Unable to find journey " + message);
        journeys.forEach(journey -> assertFalse(journey.getStages().isEmpty(), message + " missing stages for journey" + journey));
        journeys.forEach(RouteCalculatorTest::checkStages);
        return journeys;
    }

    private List<TramTime> checkRangeOfTimes(TramStations start, TramStations dest) {

        // TODO Lockdown TEMPORARY 23 Changed to 21
        // TODO lockdown services after 6.10
        int minsOffset = 10;
        List<TramTime> missing = new LinkedList<>();
        int latestHour = 21;
        for (int hour = 6; hour < latestHour; hour++) {
            for (int minutes = minsOffset; minutes < 59; minutes=minutes+ maxChanges) {
                TramTime time = TramTime.of(hour, minutes);
                JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(when), time, false, maxChanges,
                        maxJourneyDuration);
                Set<Journey> journeys = calculator.calculateRouteAsSet(start, dest, journeyRequest, 1);
                if (journeys.isEmpty()) {
                    missing.add(time);
                }
            }
            minsOffset = 0;
        }
        return missing;
    }

    @Deprecated
    private Set<Journey> calculateRoutes(TramStations start, TramStations destination, TramTime queryTime, TramServiceDate today) {
        JourneyRequest journeyRequest = new JourneyRequest(today, queryTime, false, maxChanges, maxJourneyDuration);
        return calculator.calculateRouteAsSet(start, destination, journeyRequest);
    }

    @Deprecated
    private static void checkStages(Journey journey) {
        List<TransportStage> stages = journey.getStages();
        TramTime earliestAtNextStage = null;
        for (TransportStage stage : stages) {
            if (earliestAtNextStage!=null) {
                assertFalse(
                        stage.getFirstDepartureTime().isBefore(earliestAtNextStage), stage.toString() + " arrived before " + earliestAtNextStage);
            }
            earliestAtNextStage = stage.getFirstDepartureTime().plusMinutes(stage.getDuration());
        }
    }

}