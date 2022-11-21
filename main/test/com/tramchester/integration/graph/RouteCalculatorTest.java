package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.VehicleStage;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.ConfigParameterResolver;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.graphdb.Transaction;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.domain.time.Durations.greaterOrEquals;
import static com.tramchester.testSupport.TestEnv.DAYS_AHEAD;
import static com.tramchester.testSupport.TestEnv.avoidChristmasDate;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
@ExtendWith(ConfigParameterResolver.class)
@DataUpdateTest
//@DualTest
public class RouteCalculatorTest {

    // Note this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static TramchesterConfig config;

    private final int maxChanges = 4;

    private static Set<TransportMode> requestedModes;

    private RouteCalculatorTestFacade calculator;
    private final TramDate when = TestEnv.testDay();
    private Transaction txn;
    private Duration maxJourneyDuration;
    private int maxNumResults;

    @BeforeAll
    static void onceBeforeAnyTestsRun(TramchesterConfig tramchesterConfig) {
        config = tramchesterConfig;
        requestedModes = Collections.singleton(Tram);
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
        maxJourneyDuration = Duration.ofMinutes(config.getMaxJourneyDuration());
        maxNumResults = config.getMaxNumResults();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldReproIssueWithChangesVeloToTraffordBar() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(8,0), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, VeloPark, TraffordBar);
    }

    @PiccGardens2022
    @Test
    void shouldPlanSimpleJourneyFromAltyToAshtonCheckInterchanges() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(17,45), false,
                3, maxJourneyDuration, 5, requestedModes);

        // todo added market street and shudehill
        Set<String> expected = Stream.of(Cornbrook, StPetersSquare, Deansgate, Piccadilly, MarketStreet, Shudehill).
                map(TramStations::getName).collect(Collectors.toSet());

        Set<Journey> journeys = calculator.calculateRouteAsSet(Altrincham, Ashton, journeyRequest);
        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            TransportStage<?, ?> firstStage = journey.getStages().get(0);
            String interchange = firstStage.getLastStation().getName();
            assertTrue(expected.contains(interchange), interchange + " not in " + expected);
        });

    }

    @Test
    void shouldHaveSimpleOneStopJourneyNextDays() {
        checkRouteNextNDays(TraffordBar, Altrincham, when, TramTime.of(9,0), DAYS_AHEAD);
    }

    @Test
    void shouldHaveSimpleManyStopSameLineJourney() {
        checkRouteNextNDays(Altrincham, Cornbrook, when, TramTime.of(9,0), DAYS_AHEAD);
    }

    @DataExpiryCategory
    @Test
    void shouldHaveSimpleManyStopJourneyViaInterchangeNDaysAhead() {
        checkRouteNextNDays(StPetersSquare, ManAirport, when, TramTime.of(12,0), DAYS_AHEAD);
    }

    @Test
    void shouldHaveSimpleJourney() {
        final TramTime originalQueryTime = TramTime.of(10, 15);
        JourneyRequest journeyRequest = new JourneyRequest(when, originalQueryTime, false, maxChanges,
                maxJourneyDuration, maxNumResults, requestedModes);
        Set<Journey> journeys = calculator.calculateRouteAsSet(Altrincham, Deansgate, journeyRequest);
        Set<Journey> results = checkJourneys(Altrincham, Deansgate, originalQueryTime, journeyRequest.getDate(), journeys);

        results.forEach(journey -> {
            List<Location<?>> pathCallingPoints = journey.getPath();

            assertEquals(11, pathCallingPoints.size());
            assertEquals(Altrincham.getId(), pathCallingPoints.get(0).getId());
            assertEquals(LocationType.Station, pathCallingPoints.get(0).getLocationType());
            assertEquals(Deansgate.getId(), pathCallingPoints.get(10).getId());

            List<TransportStage<?, ?>> stages = journey.getStages();
            assertEquals(1, stages.size(), "wrong number stages " + stages);
            TransportStage<?, ?> stage = stages.get(0);
            assertEquals(9, stage.getPassedStopsCount());
            List<StopCall> callingPoints = stage.getCallingPoints();
            assertEquals(9, callingPoints.size());
            assertEquals(NavigationRoad.getId(), callingPoints.get(0).getStationId());
            assertEquals(Cornbrook.getId(), callingPoints.get(8).getStationId());
        });
    }

    @Test
    void shouldHaveFirstResultWithinReasonableTimeOfQuery() {
        final TramTime queryTime = TramTime.of(17, 45);
        JourneyRequest journeyRequest = standardJourneyRequest(when, queryTime, maxNumResults);

        Set<Journey> journeys = calculator.calculateRouteAsSet(Altrincham, Ashton, journeyRequest);

        Optional<Journey> earliest = journeys.stream().min(TramTime.comparing(Journey::getDepartTime));
        assertTrue(earliest.isPresent());

        final TramTime firstDeparttime = earliest.get().getDepartTime();
        Duration elapsed = TramTime.difference(queryTime, firstDeparttime);
        assertFalse(greaterOrEquals(elapsed, Duration.ofMinutes(16)), "first result too far in future " + firstDeparttime);
    }

    @Test
    void shouldHaveSameResultWithinReasonableTime() {
        final TramTime queryTimeA = TramTime.of(8, 50);
        final TramTime queryTimeB = queryTimeA.plusMinutes(3);

        JourneyRequest journeyRequestA = standardJourneyRequest(when, queryTimeA, maxNumResults);
        JourneyRequest journeyRequestB = standardJourneyRequest(when, queryTimeB, maxNumResults);

        Set<Journey> journeysA = calculator.calculateRouteAsSet(ManAirport, EastDidsbury, journeyRequestA);
        Set<Journey> journeysB = calculator.calculateRouteAsSet(ManAirport, EastDidsbury, journeyRequestB);

        Optional<Journey> earliestA = journeysA.stream().min(TramTime.comparing(Journey::getDepartTime));
        assertTrue(earliestA.isPresent());

        Optional<Journey> earliestB = journeysB.stream().min(TramTime.comparing(Journey::getDepartTime));
        assertTrue(earliestB.isPresent());

        final TramTime firstDeparttimeA = earliestA.get().getDepartTime();
        final TramTime firstDeparttimeB = earliestB.get().getDepartTime();

        assertTrue(firstDeparttimeA.isAfter(queryTimeB) || firstDeparttimeA.equals(queryTimeB),
                firstDeparttimeA + " not after " + queryTimeB); // check assumption first
        assertEquals(firstDeparttimeA, firstDeparttimeB);
    }

    @Test
    void shouldHaveReasonableJourneyAltyToDeansgate() {
        JourneyRequest request = standardJourneyRequest(when, TramTime.of(10, 15), maxNumResults);

        Set<Journey> results = calculator.calculateRouteAsSet(Altrincham, Deansgate, request);

        assertFalse(results.isEmpty());
        results.forEach(journey -> {
            assertEquals(1, journey.getStages().size()); // should be one stage only
            journey.getStages().stream().
                    map(raw -> (VehicleStage) raw).
                    map(VehicleStage::getCost).
                    forEach(cost -> assertFalse(cost.isZero() || cost.isNegative()));
            Duration total = journey.getStages().stream().
                    map(raw -> (VehicleStage) raw).
                    map(VehicleStage::getCost).
                    reduce(Duration.ZERO, Duration::plus);
            assertTrue(total.compareTo(Duration.ofMinutes(20))>0);
        });
    }

    @Disabled("Failing due to temporarily less frequency service")
    @Test
    void shouldUseAllRoutesCorrectlWhenMultipleRoutesServDestination() {

        TramStations start = Altrincham;

        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(10, 21), maxNumResults);

        Set<Journey> servedByBothRoutes = calculator.calculateRouteAsSet(start, Deansgate, journeyRequest);
        Set<Journey> altyToPiccGardens = calculator.calculateRouteAsSet(start, PiccadillyGardens, journeyRequest);
        Set<Journey> altyToMarketStreet = calculator.calculateRouteAsSet(start, MarketStreet, journeyRequest);

        assertEquals(altyToPiccGardens.size()+altyToMarketStreet.size(), servedByBothRoutes.size());
    }


    // over max wait, catch failure to accumulate journey times correctly
    @Test
    void shouldHaveSimpleButLongJoruneySameRoute() {
        checkRouteNextNDays(ManAirport, TraffordBar, when, TramTime.of(9,0), 1);
    }

    @Test
    void shouldHaveLongJourneyAcross() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(9,0), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, Altrincham, Rochdale);
    }

    @Test
    void shouldHaveReasonableLongJourneyAcrossFromInterchange() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(8, 0), maxNumResults);
        Set<Journey> journeys = calculator.calculateRouteAsSet(Monsall, RochdaleRail, journeyRequest);

        assertFalse(journeys.isEmpty());
        journeys.forEach(journey -> {
            // direct, or change at shaw
            assertTrue(journey.getStages().size()<=2);
        });
    }

    @Test
    void shouldHaveSimpleManyStopJourneyStartAtInterchange() {
        checkRouteNextNDays(Cornbrook, Bury, when, TramTime.of(9,0), 1);
    }

    @Test
    void shouldLimitNumberChangesResultsInNoJourneys() {
        TramDate today = TramDate.from(TestEnv.LocalNow());

        JourneyRequest request = new JourneyRequest(today, TramTime.of(11, 43), false, 0,
                maxJourneyDuration, 1, requestedModes);
        Set<Journey> results = calculator.calculateRouteAsSet(Altrincham, ManAirport, request);

        assertEquals(0, results.size());
    }

    @Test
    void shouldNotReturnBackToStartOnJourney() {
        TramDate today = TramDate.from(TestEnv.LocalNow());

        JourneyRequest request = new JourneyRequest(today, TramTime.of(20, 9), false, 2,
                maxJourneyDuration, 6, requestedModes);

        Set<Journey> results = calculator.calculateRouteAsSet(Deansgate, ManAirport, request);

        assertFalse(results.isEmpty(),"no journeys found");

        results.forEach(result -> {
            long seenStart = result.getPath().stream().filter(location -> location.getId().equals(Deansgate.getId())).count();
            assertEquals(1, seenStart, "seen start location again");
        });
    }

    @Test
    void testJourneyFromAltyToAirport() {
        TramDate today = TramDate.from(TestEnv.LocalNow());

        JourneyRequest request = new JourneyRequest(today, TramTime.of(11, 43), false, maxChanges,
                maxJourneyDuration, maxNumResults, requestedModes);
        Set<Journey> results =  calculator.calculateRouteAsSet(Altrincham, ManAirport, request);

        assertTrue(results.size()>0, "no results");    // results is iterator
        for (Journey result : results) {
            List<TransportStage<?,?>> stages = result.getStages();
            assertEquals(2,stages.size());
            VehicleStage firstStage = (VehicleStage) stages.get(0);
            assertEquals(Altrincham.getId(), firstStage.getFirstStation().getId());
            assertEquals(TraffordBar.getId(), firstStage.getLastStation().getId(), stages.toString());
            assertEquals(Tram, firstStage.getMode());
            assertEquals(7, firstStage.getPassedStopsCount());

            VehicleStage finalStage = (VehicleStage) stages.get(stages.size()-1);
            //assertEquals(Stations.TraffordBar, secondStage.getFirstStation()); // THIS CAN CHANGE
            assertEquals(ManAirport.getId(), finalStage.getLastStation().getId());
            assertEquals(Tram, finalStage.getMode());
        }
    }

    @Test
    void shouldHandleCrossingMidnightWithChange() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(23,30), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, TraffordCentre, TraffordBar);
    }

    @Test
    void shouldHandleCrossingMidnightWithChangeCentral() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(22,15), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, Monsall, Piccadilly);
    }

    @Test
    void shouldHandleCrossingMidnightDirect() {
        JourneyRequest journeyRequestA = standardJourneyRequest(when, TramTime.of(23,55), maxNumResults);
        assertGetAndCheckJourneys(journeyRequestA, Cornbrook, StPetersSquare);

        JourneyRequest journeyRequestB = standardJourneyRequest(when, TramTime.of(0,0), maxNumResults);
        assertGetAndCheckJourneys(journeyRequestB, Altrincham, OldTrafford);
    }

    @Test
    void shouldHandleAfterMidnightDirect() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(0,0), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, Altrincham, NavigationRoad);
    }

    @PiccGardens2022
    @Test
    void shouldHandleAfterMidnightDirectCentral() {
        TramDate testDate = when;

        JourneyRequest journeyRequestA = standardJourneyRequest(testDate, TramTime.of(23,59), maxNumResults);
        assertGetAndCheckJourneys(journeyRequestA, StPetersSquare, MarketStreet);

        JourneyRequest journeyRequestB = standardJourneyRequest(testDate, TramTime.nextDay(0,0), maxNumResults);
        assertGetAndCheckJourneys(journeyRequestB, StPetersSquare, MarketStreet);

        JourneyRequest journeyRequestC = standardJourneyRequest(testDate, TramTime.nextDay(0,1), maxNumResults);
        assertGetAndCheckJourneys(journeyRequestC, StPetersSquare, MarketStreet);

        // last tram is now earlier
//        JourneyRequest journeyRequestD = standardJourneyRequest(testDate, TramTime.of(0,0), maxNumResults);
//        assertGetAndCheckJourneys(journeyRequestD, StPetersSquare, MarketStreet);
    }

    @Test
    void shouldHaveHeatonParkToBurtonRoad() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(7, 30), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, HeatonPark, BurtonRoad);
    }

    @Test
    void shouldReproIssueRochInterchangeToBury() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(9, 0), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, Rochdale, Bury);
    }

    @Test
    void shouldReproIssueWithMediaCityTrams() {

        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(12, 0), maxNumResults);

        assertGetAndCheckJourneys(journeyRequest, StPetersSquare, MediaCityUK);
        assertGetAndCheckJourneys(journeyRequest, ExchangeSquare, MediaCityUK);
    }

    public static Duration costOfJourney(Journey journey) {
        List<TransportStage<?,?>> stages = journey.getStages();
        TramTime departs = stages.get(0).getFirstDepartureTime();
        TramTime arrive = stages.get(stages.size() - 1).getExpectedArrivalTime();

        return TramTime.difference(departs, arrive);
    }

    @Test
    void shouldCheckCornbrookToStPetersSquareOnSundayMorning() {
        JourneyRequest journeyRequest = standardJourneyRequest(TestEnv.nextSunday(), TramTime.of(11, 0), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, Cornbrook, StPetersSquare);
    }

    @Test
    void shouldNotGenerateDuplicateJourneysForSameReqNumChanges() {

        JourneyRequest request = new JourneyRequest(when, TramTime.of(11, 45), false,
                4, maxJourneyDuration, 3, requestedModes);
        Set<Journey> journeys =  calculator.calculateRouteAsSet(Bury, Altrincham, request);

        assertTrue(journeys.size()>0);

        Set<Integer> reqNumChanges = journeys.stream().map(Journey::getRequestedNumberChanges).collect(Collectors.toSet());

        reqNumChanges.forEach(numChange -> {
            Set<List<TransportStage<?,?>>> uniqueStages = new HashSet<>();

            journeys.stream().filter(journey -> numChange.equals(journey.getRequestedNumberChanges())).forEach(journey -> {

                assertFalse(uniqueStages.contains(journey.getStages()),
                        journey.getStages() + " seen before in " + journeys);
                uniqueStages.add(journey.getStages());
            });
        });
    }

    @PiccGardens2022
    @Test
    void ShouldReproIssueWithSomeMediaCityJourneys() {
        // max changes 1 -> 2
        JourneyRequest request = new JourneyRequest(when, TramTime.of(8, 5), false,
                2, maxJourneyDuration, 2, requestedModes);

        assertFalse(calculator.calculateRouteAsSet(MediaCityUK, Etihad, request).isEmpty());
        assertFalse(calculator.calculateRouteAsSet(MediaCityUK, VeloPark, request).isEmpty());
        assertFalse(calculator.calculateRouteAsSet(MediaCityUK, Ashton, request).isEmpty());
    }

    @Test
    void shouldHaveInAndAroundCornbrookToEccles8amTuesday() {
        // catches issue with services, only some of which go to media city, while others direct to broadway
        JourneyRequest journeyRequest8am = standardJourneyRequest(when, TramTime.of(8,0), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest8am, Cornbrook, Broadway);
        assertGetAndCheckJourneys(journeyRequest8am, Cornbrook, Eccles);

        JourneyRequest journeyRequest9am = standardJourneyRequest(when, TramTime.of(9,0), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest9am, Cornbrook, Broadway);
        assertGetAndCheckJourneys(journeyRequest9am, Cornbrook, Eccles);
    }

    @Test
    void shouldReproIssueWithJourneysToEccles() {
        TramDate testDate = this.when.plusWeeks(1);

        JourneyRequest journeyRequest = standardJourneyRequest(testDate, TramTime.of(9,0), maxNumResults);

        assertGetAndCheckJourneys(journeyRequest, Bury, Broadway);
        assertGetAndCheckJourneys(journeyRequest, Bury, Eccles);
    }

    @PiccGardens2022
    @Test
    void reproduceIssueEdgePerTrip() {
        // see also RouteCalculatorSubGraphTest
//        JourneyRequest journeyRequestA = standardJourneyRequest(when, TramTime.of(19,48), maxNumResults);
//        assertGetAndCheckJourneys(journeyRequestA, PiccadillyGardens, Pomona);

        JourneyRequest journeyRequestB = standardJourneyRequest(when, TramTime.of(19,51), maxNumResults);
        assertGetAndCheckJourneys(journeyRequestB, StPetersSquare, Pomona);

        JourneyRequest journeyRequestC = standardJourneyRequest(when, TramTime.of(19,56), maxNumResults);
        assertGetAndCheckJourneys(journeyRequestC, StPetersSquare, Pomona);

        JourneyRequest journeyRequestD = standardJourneyRequest(when, TramTime.of(6,40), maxNumResults);
        assertGetAndCheckJourneys(journeyRequestD, Cornbrook, Eccles);
    }

    @Test
    void shouldReproIssueWithStPetersToBeyondEcclesAt8AM() {
        List<TramTime> missingTimes = checkRangeOfTimes(Cornbrook, Eccles);
        assertTrue(missingTimes.isEmpty(), missingTimes.toString());
    }

    @Test
    void reproduceIssueWithImmediateDepartOffABoardedTram() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(8,0), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, Deansgate, Ashton);
    }

    @Test
    void reproduceIssueWithTramsSundayStPetersToDeansgate() {
        JourneyRequest journeyRequest = standardJourneyRequest(TestEnv.nextSunday(), TramTime.of(9,0), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, StPetersSquare, Deansgate);
    }

    @PiccGardens2022
    @Test
    void reproduceIssueWithTramsSundayAshtonToEccles() {
        // todo set max changes to 3
        JourneyRequest journeyRequest = new JourneyRequest(TestEnv.nextSunday(), TramTime.of(9, 0), false,
                4, maxJourneyDuration, maxNumResults, requestedModes);
        assertGetAndCheckJourneys(journeyRequest, Ashton, Eccles);
    }

    @Test
    void reproduceIssueWithTramsSundayToFromEcclesAndCornbrook() {
        JourneyRequest journeyRequest = standardJourneyRequest(TestEnv.nextSunday(), TramTime.of(9,0), maxNumResults);

        assertGetAndCheckJourneys(journeyRequest, Cornbrook, Eccles);
        assertGetAndCheckJourneys(journeyRequest, Eccles, Cornbrook);
    }

    @Test
    void shouldReproduceIssueCornbrookToAshtonSatursdays() {
        JourneyRequest journeyRequest = standardJourneyRequest(TestEnv.nextSaturday(), TramTime.of(9,0), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, Cornbrook, Ashton);
    }

    @Test
    void shouldFindRouteVeloToHoltTownAt8RangeOfTimes() {
        for(int i=0; i<60; i++) {
            TramTime time = TramTime.of(8,i);
            JourneyRequest journeyRequest = standardJourneyRequest(when, time, maxNumResults);
            assertGetAndCheckJourneys(journeyRequest, VeloPark, HoltTown);
        }
    }

    @Test
    void reproIssueRochdaleToEccles() {
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = standardJourneyRequest(when, time, maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, Rochdale, Eccles);
    }

    @NotNull
    private JourneyRequest standardJourneyRequest(TramDate date, TramTime time, long maxNumberJourneys) {
        return new JourneyRequest(date, time, false, maxChanges, maxJourneyDuration, maxNumberJourneys, requestedModes);
    }

    private void checkRouteNextNDays(TramStations start, TramStations dest, TramDate date, TramTime time, int numDays) {
        if (!dest.equals(start)) {
            for(int day = 0; day< numDays; day++) {
                TramDate testDate = avoidChristmasDate(date.plusDays(day));
                JourneyRequest journeyRequest = standardJourneyRequest(testDate, time, maxNumResults);
                assertGetAndCheckJourneys(journeyRequest, start, dest);
            }
        }
    }

    private void assertGetAndCheckJourneys(JourneyRequest journeyRequest, TramStations start, TramStations dest) {
        Set<Journey> journeys = calculator.calculateRouteAsSet(start, dest, journeyRequest);
        checkJourneys(start, dest, journeyRequest.getOriginalTime(), journeyRequest.getDate(), journeys);
    }

    @NotNull
    private Set<Journey> checkJourneys(TramStations start, TramStations dest, TramTime time, TramDate date, Set<Journey> journeys) {
        String message = "from " + start.getId() + " to " + dest.getId() + " at " + time + " on " + date;
        assertTrue(journeys.size() > 0, "Unable to find journey " + message);
        journeys.forEach(journey -> assertFalse(journey.getStages().isEmpty(), message + " missing stages for journey" + journey));
        journeys.forEach(journey -> {
            List<TransportStage<?,?>> stages = journey.getStages();
            TramTime earliestAtNextStage = null;
            for (TransportStage<?,?> stage : stages) {
                if (earliestAtNextStage!=null) {
                    assertFalse(
                            stage.getFirstDepartureTime().isBefore(earliestAtNextStage), stage + " arrived before " + earliestAtNextStage);
                }
                earliestAtNextStage = stage.getFirstDepartureTime().plus(stage.getDuration());
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
                JourneyRequest journeyRequest = new JourneyRequest(when, time, false, maxChanges,
                        maxJourneyDuration, 1, requestedModes);
                Set<Journey> journeys = calculator.calculateRouteAsSet(start, dest, journeyRequest);
                if (journeys.isEmpty()) {
                    missing.add(time);
                }
            }
            minsOffset = 0;
        }
        return missing;
    }

}