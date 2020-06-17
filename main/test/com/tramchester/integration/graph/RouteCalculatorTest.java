package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.VehicleStage;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.Stations.HeatonPark;
import static com.tramchester.testSupport.TestEnv.DAYS_AHEAD;
import static com.tramchester.testSupport.TestEnv.avoidChristmasDate;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
public class RouteCalculatorTest {

    // Note this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static Dependencies dependencies;
    private static GraphDatabase database;

    private RouteCalculator calculator;
    private final LocalDate when = TestEnv.testDay();
    private Transaction txn;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        TramchesterConfig testConfig = new IntegrationTramTestConfig();
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
        calculator = dependencies.get(RouteCalculator.class);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldReproIssueWithChangesVeloToTraffordBar() {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.TraffordBar, TramTime.of(8,0), when);
    }

    @Test
    void shouldHaveSimpleOneStopJourneyNextDays() {
        checkRouteNextNDays(Stations.TraffordBar, Stations.Altrincham, when, TramTime.of(9,0), DAYS_AHEAD);
    }

    @Test
    void shouldHaveSimpleManyStopSameLineJourney() {
        checkRouteNextNDays(Stations.Altrincham, Stations.Cornbrook, when, TramTime.of(9,0), DAYS_AHEAD);
    }

    @Test
    void shouldHaveSimpleManyStopJourneyViaInterchangeNDaysAhead() {
        checkRouteNextNDays(Stations.Altrincham, Stations.Bury, when, TramTime.of(9,0), DAYS_AHEAD);
    }

    @Test
    void shouldHaveSimpleJourney() {
        validateAtLeastOneJourney(Stations.Altrincham, Stations.Deansgate, TramTime.of(10, 15), when);
    }

    @Test
    void shouldHaveReasonableJourneyAltyToDeansgate() {
        TramServiceDate tramServiceDate = new TramServiceDate(when);
        Set<Journey> results = calculator.calculateRoute(txn, Stations.Altrincham, Stations.Deansgate,
                new JourneyRequest(tramServiceDate, TramTime.of(10, 15), false)).collect(Collectors.toSet());
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

        Station start = Stations.Altrincham;

        TramTime queryTime = TramTime.of(10, 21);
        Set<Journey> servedByBothRoutes = calculateRoutes(start, Stations.Deansgate, queryTime, today);

        Set<Journey> altyToPiccGardens = calculateRoutes(start, Stations.PiccadillyGardens, queryTime, today);
        Set<Journey> altyToMarketStreet = calculateRoutes(start, Stations.MarketStreet, queryTime, today);

        assertEquals(altyToPiccGardens.size()+altyToMarketStreet.size(), servedByBothRoutes.size());
    }

    // over max wait, catch failure to accumulate journey times correctly
    @Test
    void shouldHaveSimpleButLongJoruneySameRoute() {
        checkRouteNextNDays(Stations.ManAirport, Stations.TraffordBar, when, TramTime.of(9,0), 1);
    }

    @Test
    void shouldHaveLongJourneyAcross() {
        validateAtLeastOneJourney(Stations.Altrincham, Stations.Rochdale, TramTime.of(9,0), when);
    }

    @Test
    void shouldHaveReasonableLongJourneyAcrossFromInterchange() {
        TramTime am8 = TramTime.of(8, 0);

        Set<Journey> journeys = calculateRoutes(Stations.Monsall, Stations.RochdaleRail, am8, new TramServiceDate(when));

        assertFalse(journeys.isEmpty());
        journeys.forEach(journey -> {
            // direct, or change at shaw
            assertTrue(journey.getStages().size()<=2);
        });
    }

    @Test
    void shouldHaveSimpleManyStopJourneyStartAtInterchange() {
        checkRouteNextNDays(Stations.Cornbrook, Stations.Bury, when, TramTime.of(9,0), 1);
    }

    @Test
    void shouldLimitNumberChangesResultsInNoJourneys() {
        TramServiceDate today = new TramServiceDate(TestEnv.LocalNow().toLocalDate());

        Stream<Journey> stream = calculator.calculateRoute(txn, Stations.Altrincham, Stations.ManAirport,
                new JourneyRequest(today, TramTime.of(11, 43), false, 0));
        Set<Journey> results = stream.collect(Collectors.toSet());
        stream.close();

        assertEquals(0, results.size());
    }

    @Test
    void testJourneyFromAltyToAirport() {
        TramServiceDate today = new TramServiceDate(TestEnv.LocalNow().toLocalDate());

        Stream<Journey> stream = calculator.calculateRoute(txn, Stations.Altrincham, Stations.ManAirport,
                new JourneyRequest(today, TramTime.of(11, 43), false));
        Set<Journey> results = stream.collect(Collectors.toSet());
        stream.close();

        assertTrue(results.size()>0, "no results");    // results is iterator
        for (Journey result : results) {
            List<TransportStage> stages = result.getStages();
            assertEquals(2,stages.size());
            VehicleStage firstStage = (VehicleStage) stages.get(0);
            assertEquals(Stations.Altrincham, firstStage.getFirstStation());
            assertEquals(Stations.TraffordBar, firstStage.getLastStation());
            assertEquals(TransportMode.Tram, firstStage.getMode());
            assertEquals(7, firstStage.getPassedStops());

            VehicleStage finalStage = (VehicleStage) stages.get(stages.size()-1);
            //assertEquals(Stations.TraffordBar, secondStage.getFirstStation()); // THIS CAN CHANGE
            assertEquals(Stations.ManAirport, finalStage.getLastStation());
            assertEquals(TransportMode.Tram, finalStage.getMode());
        }
    }

    @Disabled("Temporary: trams finish at 2300")
    @Test
    void shouldHandleCrossingMidnightWithChange() {
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.ManAirport, TramTime.of(23,20), when);
    }

    @Disabled("Temporary: trams finish at 2300")
    @Test
    void shouldHandleCrossingMidnightDirect() {
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.StPetersSquare, TramTime.of(23,55), when);
        validateAtLeastOneJourney(Stations.Altrincham, Stations.TraffordBar, TramTime.of(23,51), when);
    }

    @Disabled("Temporary: trams finish at 2300")
    @Test
    void shouldHandleAfterMidnightDirect() {
        validateAtLeastOneJourney(Stations.Altrincham, Stations.NavigationRoad, TramTime.of(0,0), when);
    }

    @Test
    void shouldHaveHeatonParkToBurtonRoad() {
        validateAtLeastOneJourney(HeatonPark, Stations.BurtonRoad, TramTime.of(7, 30), when);
    }

    @Test
    void shouldReproIssueRochInterchangeToBury() {
        validateAtLeastOneJourney(Stations.Rochdale, Stations.Bury, TramTime.of(9, 0), when);
    }

    @Test
    void shouldReproIssueWithMediaCityTrams() {
        TramTime time = TramTime.of(12, 0);

        validateAtLeastOneJourney(Stations.StPetersSquare, Stations.MediaCityUK, time, when);
        validateAtLeastOneJourney(Stations.ExchangeSquare, Stations.MediaCityUK, time, when);
    }


    public static int costOfJourney(Journey journey) {
        List<TransportStage> stages = journey.getStages();
        TramTime departs = stages.get(0).getFirstDepartureTime();
        TramTime arrive = stages.get(stages.size() - 1).getExpectedArrivalTime();

        return TramTime.diffenceAsMinutes(departs, arrive);
    }

    @Test
    void shouldCheckCornbrookToStPetersSquareOnSundayMorning() {
        TramTime time = TramTime.of(11, 0);
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.StPetersSquare, time, when.plusDays(5));
    }

    @Test
    void shouldNotGenerateDuplicateJourneys() {

        Set<List<TransportStage>> stages = new HashSet<>();

        Stream<Journey> stream = calculator.calculateRoute(txn, Stations.Bury, Stations.Altrincham,
                new JourneyRequest(new TramServiceDate(when), TramTime.of(11,45), false));
        Set<Journey> journeys = stream.collect(Collectors.toSet());
        stream.close();

        assertTrue(journeys.size()>0);

        journeys.forEach(journey -> {
            assertFalse(stages.contains(journey.getStages()), stages.toString());
            stages.add(journey.getStages());
        });

    }

    @Test
    void shouldHaveInAndAroundCornbrookToEccles8amTuesday() {
        // catches issue with services, only some of which go to media city, while others direct to broadway
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.Broadway, TramTime.of(8,0), when);
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.Eccles, TramTime.of(8,0), when);

        validateAtLeastOneJourney(Stations.Cornbrook, Stations.Broadway, TramTime.of(9,0), when);
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.Eccles, TramTime.of(9,0), when);
    }

    @Test
    void shouldReproIssueWithJourneysToEccles() {
        validateAtLeastOneJourney(Stations.Bury, Stations.Broadway, TramTime.of(9,0), when);
        validateAtLeastOneJourney(Stations.Bury, Stations.Eccles, TramTime.of(9,0), when);
    }

    @Test
    void reproduceIssueEdgePerTrip() {
        // see also RouteCalculatorSubGraphTest
        validateAtLeastOneJourney(Stations.PiccadillyGardens, Stations.Pomona, TramTime.of(19,48), when);
        validateAtLeastOneJourney(Stations.StPetersSquare, Stations.Pomona, TramTime.of(19,51), when);
        validateAtLeastOneJourney(Stations.StPetersSquare, Stations.Pomona, TramTime.of(19,56), when);
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.Eccles, TramTime.of(6,1), when);
    }

    @Test
    void shouldReproIssueWithStPetersToBeyondEcclesAt8AM() {
        List<TramTime> missingTimes = checkRangeOfTimes(Stations.Cornbrook, Stations.Eccles);
        assertTrue(missingTimes.isEmpty(), missingTimes.toString());
    }

    @Test
    void reproduceIssueWithImmediateDepartOffABoardedTram() {
        validateAtLeastOneJourney(Stations.Deansgate, Stations.Ashton, TramTime.of(8,0), when);
    }

    @Test
    void reproduceIssueWithTramsSundayStPetersToDeansgate() {
        validateAtLeastOneJourney(Stations.StPetersSquare, Stations.Deansgate, TramTime.of(9,0), TestEnv.nextSunday());
    }

    @Test
    void reproduceIssueWithTramsSundayAshtonToEccles() {
        validateAtLeastOneJourney(Stations.Ashton, Stations.Eccles, TramTime.of(9,0), TestEnv.nextSunday());
    }

    @Test
    void reproduceIssueWithTramsSundayToFromEcclesAndCornbrook() {
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.Eccles, TramTime.of(9,0), TestEnv.nextSunday());
        validateAtLeastOneJourney(Stations.Eccles, Stations.Cornbrook, TramTime.of(9,0), TestEnv.nextSunday());
    }

    @Test
    void shouldReproduceIssueCornbrookToAshtonSatursdays() {
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.Ashton, TramTime.of(9,0), TestEnv.nextSaturday());
    }

    @Test
    void shouldFindRouteVeloToHoltTownAt8RangeOfTimes() {
        for(int i=0; i<60; i++) {
            TramTime time = TramTime.of(8,i);
            validateAtLeastOneJourney(Stations.VeloPark, Stations.HoltTown, time, when);
        }
    }

    @Test
    void reproIssueRochdaleToEccles() {
        validateAtLeastOneJourney(Stations.Rochdale, Stations.Eccles, TramTime.of(9,0), when);
    }

    private void checkRouteNextNDays(Station start, Station dest, LocalDate date, TramTime time, int numDays) {
        if (!dest.equals(start)) {
            for(int day = 0; day< numDays; day++) {
                LocalDate testDate = avoidChristmasDate(date.plusDays(day));
                validateAtLeastOneJourney(start, dest, time, testDate);
            }
        }
    }

    private void validateAtLeastOneJourney(Station station, Station dest, TramTime time, LocalDate date) {
        validateAtLeastOneJourney(calculator, txn, station, dest, time, date, 5);
    }

    public static Set<Journey> validateAtLeastOneJourney(RouteCalculator theCalculator, Transaction transaction, Station start, Station destination,
                                                         TramTime time, LocalDate date, int maxChanges) {
        TramServiceDate queryDate = new TramServiceDate(date);
        Stream<Journey> journeyStream = theCalculator.calculateRoute(transaction, start, destination, new JourneyRequest(new TramServiceDate(date), time,
                false, maxChanges));
        Set<Journey> journeys = journeyStream.limit(1).collect(Collectors.toSet());
        journeyStream.close();

        String message = "from " + start.getName() + " to " + destination.getName() + " at " + time + " on " + queryDate;
        assertTrue(journeys.size() > 0, "Unable to find journey " + message);
        journeys.forEach(journey -> assertFalse(journey.getStages().isEmpty(), message + " missing stages for journey" + journey));
        journeys.forEach(RouteCalculatorTest::checkStages);
        return journeys;
    }

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

    private List<TramTime> checkRangeOfTimes(Station start, Station dest) {

        // TODO Lockdown TEMPORARY 23 Changed to 21
        List<TramTime> missing = new LinkedList<>();
        int latestHour = 21;
        for (int hour = 6; hour < latestHour; hour++) {
            for (int minutes = 0; minutes < 59; minutes=minutes+5) {
                TramTime time = TramTime.of(hour, minutes);
                Stream<Journey> journeys = calculator.calculateRoute(txn, start, dest,
                        new JourneyRequest(new TramServiceDate(when), time, false));
                if (journeys.limit(1).findFirst().isEmpty()) {
                    missing.add(time);
                }
                journeys.close();
            }
        }
        return missing;
    }


    private Set<Journey> calculateRoutes(Station start, Station destination, TramTime queryTime, TramServiceDate today) {
        Stream<Journey> journeyStream = calculator.calculateRoute(txn, start, destination, new JourneyRequest(today, queryTime, false));
        Set<Journey> journeySet = journeyStream.collect(Collectors.toSet());
        journeyStream.close();
        return journeySet;
    }


}