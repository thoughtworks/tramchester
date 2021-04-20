package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Journey;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.transportStages.VehicleStage;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.graph.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.*;
import com.tramchester.testSupport.reference.TramStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.tramchester.testSupport.TestEnv.DAYS_AHEAD;
import static com.tramchester.testSupport.TestEnv.avoidChristmasDate;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
public class RouteCalculatorTest {

    // Note this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static IntegrationTramTestConfig config;
    private final int maxChanges = 2;

    private RouteCalculatorTestFacade calculator;
    private final LocalDate when = TestEnv.testDay();
    private Transaction txn;
    private int maxJourneyDuration;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        database = componentContainer.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);
        maxJourneyDuration = config.getMaxJourneyDuration();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldReproIssueWithChangesVeloToTraffordBar() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(8,0));
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
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(10, 15));
        Set<Journey> journeys = calculator.calculateRouteAsSet(Altrincham, TramStations.Deansgate, journeyRequest, 1);
        Set<Journey> results = checkJourneys(Altrincham, TramStations.Deansgate, journeyRequest.getTime(), journeyRequest.getDate(), journeys);

        results.forEach(journey -> {
            List<Location<?>> pathCallingPoints = journey.getPath();

            // TODO this is double counting start and end, should be 11
            assertEquals(13, pathCallingPoints.size());
            assertEquals(Altrincham.getId(), pathCallingPoints.get(0).getId());
            assertEquals(LocationType.Station, pathCallingPoints.get(0).getLocationType());
            assertEquals(TramStations.Deansgate.getId(), pathCallingPoints.get(12).getId());

            List<TransportStage<?, ?>> stages = journey.getStages();
            assertEquals(1, stages.size());
            TransportStage<?, ?> stage = stages.get(0);
            assertEquals(9, stage.getPassedStopsCount());
            List<StopCall> callingPoints = stage.getCallingPoints();
            assertEquals(9, callingPoints.size());
            assertEquals(NavigationRoad.getId(), callingPoints.get(0).getStationId());
            assertEquals(Cornbrook.getId(), callingPoints.get(8).getStationId());
        });
    }

    @Test
    void shouldHaveReasonableJourneyAltyToDeansgate() {
        JourneyRequest request = standardJourneyRequest(when, TramTime.of(10, 15));

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

        TramStations start = Altrincham;

        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(10, 21));

        Set<Journey> servedByBothRoutes = calculator.calculateRouteAsSet(start, TramStations.Deansgate, journeyRequest);
        Set<Journey> altyToPiccGardens = calculator.calculateRouteAsSet(start, TramStations.PiccadillyGardens, journeyRequest);
        Set<Journey> altyToMarketStreet = calculator.calculateRouteAsSet(start, TramStations.MarketStreet, journeyRequest);

        assertEquals(altyToPiccGardens.size()+altyToMarketStreet.size(), servedByBothRoutes.size());
    }


    // over max wait, catch failure to accumulate journey times correctly
    @Test
    void shouldHaveSimpleButLongJoruneySameRoute() {
        checkRouteNextNDays(TramStations.ManAirport, TramStations.TraffordBar, when, TramTime.of(9,0), 1);
    }

    @Test
    void shouldHaveLongJourneyAcross() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(9,0));
        assertGetAndCheckJourneys(journeyRequest, Altrincham, TramStations.Rochdale);
    }

    @Test
    void shouldHaveReasonableLongJourneyAcrossFromInterchange() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(8, 0));
        Set<Journey> journeys = calculator.calculateRouteAsSet(TramStations.Monsall, TramStations.RochdaleRail, journeyRequest);

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
            List<TransportStage<?,?>> stages = result.getStages();
            assertEquals(2,stages.size());
            VehicleStage firstStage = (VehicleStage) stages.get(0);
            assertEquals(Altrincham.getId(), firstStage.getFirstStation().getId());
            assertEquals(TramStations.TraffordBar.getId(), firstStage.getLastStation().getId());
            assertEquals(TransportMode.Tram, firstStage.getMode());
            assertEquals(7, firstStage.getPassedStopsCount());

            VehicleStage finalStage = (VehicleStage) stages.get(stages.size()-1);
            //assertEquals(Stations.TraffordBar, secondStage.getFirstStation()); // THIS CAN CHANGE
            assertEquals(TramStations.ManAirport.getId(), finalStage.getLastStation().getId());
            assertEquals(TransportMode.Tram, finalStage.getMode());
        }
    }

    @Test
    void shouldHandleCrossingMidnightWithChange() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(23,30));
        assertGetAndCheckJourneys(journeyRequest, Intu, TraffordBar);
    }

    @Test
    void shouldHandleCrossingMidnightDirect() {
        JourneyRequest journeyRequestA = standardJourneyRequest(when, TramTime.of(23,55));
        assertGetAndCheckJourneys(journeyRequestA, TramStations.Cornbrook, TramStations.StPetersSquare);

        JourneyRequest journeyRequestB = standardJourneyRequest(when, TramTime.of(0,0));
        assertGetAndCheckJourneys(journeyRequestB, Altrincham, OldTrafford); // depot run
    }

    @Test
    void shouldHandleAfterMidnightDirect() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(0,0));
        assertGetAndCheckJourneys(journeyRequest, Altrincham, TramStations.NavigationRoad); // depot run
    }

    @Test
    void shouldHaveHeatonParkToBurtonRoad() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(7, 30));
        assertGetAndCheckJourneys(journeyRequest, HeatonPark, TramStations.BurtonRoad);
    }

    @Test
    void shouldReproIssueRochInterchangeToBury() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(9, 0));
        assertGetAndCheckJourneys(journeyRequest, TramStations.Rochdale, TramStations.Bury);
    }

    @Test
    void shouldReproIssueWithMediaCityTrams() {

        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(12, 0));

        assertGetAndCheckJourneys(journeyRequest, TramStations.StPetersSquare, TramStations.MediaCityUK);
        assertGetAndCheckJourneys(journeyRequest, TramStations.ExchangeSquare, TramStations.MediaCityUK);
    }


    public static int costOfJourney(Journey journey) {
        List<TransportStage<?,?>> stages = journey.getStages();
        TramTime departs = stages.get(0).getFirstDepartureTime();
        TramTime arrive = stages.get(stages.size() - 1).getExpectedArrivalTime();

        return TramTime.diffenceAsMinutes(departs, arrive);
    }

    @Test
    void shouldCheckCornbrookToStPetersSquareOnSundayMorning() {
        JourneyRequest journeyRequest = standardJourneyRequest(when.plusDays(maxChanges), TramTime.of(11, 0));
        assertGetAndCheckJourneys(journeyRequest, TramStations.Cornbrook, TramStations.StPetersSquare);
    }

    @Test
    void shouldNotGenerateDuplicateJourneys() {

        Set<List<TransportStage<?,?>>> stages = new HashSet<>();

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
        JourneyRequest journeyRequest8am = standardJourneyRequest(when, TramTime.of(8,0));
        assertGetAndCheckJourneys(journeyRequest8am, TramStations.Cornbrook, TramStations.Broadway);
        assertGetAndCheckJourneys(journeyRequest8am, TramStations.Cornbrook, TramStations.Eccles);

        JourneyRequest journeyRequest9am = standardJourneyRequest(when, TramTime.of(9,0));
        assertGetAndCheckJourneys(journeyRequest9am, TramStations.Cornbrook, TramStations.Broadway);
        assertGetAndCheckJourneys(journeyRequest9am, TramStations.Cornbrook, TramStations.Eccles);
    }

    @Test
    void shouldReproIssueWithJourneysToEccles() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(9,0));

        assertGetAndCheckJourneys(journeyRequest, TramStations.Bury, TramStations.Broadway);
        assertGetAndCheckJourneys(journeyRequest, TramStations.Bury, TramStations.Eccles);
    }

    @Test
    void reproduceIssueEdgePerTrip() {
        // see also RouteCalculatorSubGraphTest
        JourneyRequest journeyRequestA = standardJourneyRequest(when, TramTime.of(19,48));
        assertGetAndCheckJourneys(journeyRequestA, TramStations.PiccadillyGardens, TramStations.Pomona);

        JourneyRequest journeyRequestB = standardJourneyRequest(when, TramTime.of(19,51));
        assertGetAndCheckJourneys(journeyRequestB, TramStations.StPetersSquare, TramStations.Pomona);

        JourneyRequest journeyRequestC = standardJourneyRequest(when, TramTime.of(19,56));
        assertGetAndCheckJourneys(journeyRequestC, TramStations.StPetersSquare, TramStations.Pomona);

        JourneyRequest journeyRequestD = standardJourneyRequest(when, TramTime.of(6,40));
        assertGetAndCheckJourneys(journeyRequestD, TramStations.Cornbrook, TramStations.Eccles);
    }

    @Test
    void shouldReproIssueWithStPetersToBeyondEcclesAt8AM() {
        List<TramTime> missingTimes = checkRangeOfTimes(TramStations.Cornbrook, TramStations.Eccles);
        assertTrue(missingTimes.isEmpty(), missingTimes.toString());
    }

    @Test
    void reproduceIssueWithImmediateDepartOffABoardedTram() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(8,0));
        assertGetAndCheckJourneys(journeyRequest, TramStations.Deansgate, TramStations.Ashton);
    }

    @Test
    void reproduceIssueWithTramsSundayStPetersToDeansgate() {
        JourneyRequest journeyRequest = standardJourneyRequest(TestEnv.nextSunday(), TramTime.of(9,0));
        assertGetAndCheckJourneys(journeyRequest, TramStations.StPetersSquare, TramStations.Deansgate);
    }

    @Test
    void reproduceIssueWithTramsSundayAshtonToEccles() {
        JourneyRequest journeyRequest = standardJourneyRequest(TestEnv.nextSunday(), TramTime.of(9,0));
        assertGetAndCheckJourneys(journeyRequest, TramStations.Ashton, TramStations.Eccles);
    }

    @Test
    void reproduceIssueWithTramsSundayToFromEcclesAndCornbrook() {
        JourneyRequest journeyRequest = standardJourneyRequest(TestEnv.nextSunday(), TramTime.of(9,0));

        assertGetAndCheckJourneys(journeyRequest, TramStations.Cornbrook, TramStations.Eccles);
        assertGetAndCheckJourneys(journeyRequest, TramStations.Eccles, TramStations.Cornbrook);
    }

    @Test
    void shouldReproduceIssueCornbrookToAshtonSatursdays() {
        JourneyRequest journeyRequest = standardJourneyRequest(TestEnv.nextSaturday(), TramTime.of(9,0));
        assertGetAndCheckJourneys(journeyRequest, Cornbrook, Ashton);
    }

    @Test
    void shouldFindRouteVeloToHoltTownAt8RangeOfTimes() {
        for(int i=0; i<60; i++) {
            TramTime time = TramTime.of(8,i);
            JourneyRequest journeyRequest = standardJourneyRequest(when, time);
            assertGetAndCheckJourneys(journeyRequest, TramStations.VeloPark, TramStations.HoltTown);
        }
    }

    @Test
    void reproIssueRochdaleToEccles() {
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = standardJourneyRequest(when, time);
        assertGetAndCheckJourneys(journeyRequest, TramStations.Rochdale, TramStations.Eccles);
    }

    @NotNull
    private JourneyRequest standardJourneyRequest(LocalDate date, TramTime time) {
        return new JourneyRequest(date, time, false, maxChanges, maxJourneyDuration);
    }

    private void checkRouteNextNDays(TramStations start, TramStations dest, LocalDate date, TramTime time, int numDays) {
        if (!dest.equals(start)) {
            for(int day = 0; day< numDays; day++) {
                LocalDate testDate = avoidChristmasDate(date.plusDays(day));
                JourneyRequest journeyRequest = standardJourneyRequest(testDate, time);
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
        journeys.forEach(journey -> {
            List<TransportStage<?,?>> stages = journey.getStages();
            TramTime earliestAtNextStage = null;
            for (TransportStage<?,?> stage : stages) {
                if (earliestAtNextStage!=null) {
                    assertFalse(
                            stage.getFirstDepartureTime().isBefore(earliestAtNextStage), stage.toString() + " arrived before " + earliestAtNextStage);
                }
                earliestAtNextStage = stage.getFirstDepartureTime().plusMinutes(stage.getDuration());
            }
        });
        return journeys;
    }

    private List<TramTime> checkRangeOfTimes(TramStations start, TramStations dest) {

        // TODO lockdown services after 6.10
        int minsOffset = 10;
        List<TramTime> missing = new LinkedList<>();
        int latestHour = 23;
        for (int hour = 7; hour < latestHour; hour++) {
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

}