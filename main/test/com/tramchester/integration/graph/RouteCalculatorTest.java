package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.domain.HasId;
import com.tramchester.domain.Journey;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.VehicleStage;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.*;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private RouteCalculator calculator;
    private final LocalDate when = TestEnv.testDay();
    private Transaction txn;
    private StationRepository stationRepository;

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
        calculator = dependencies.get(RouteCalculator.class);
        stationRepository = dependencies.get(StationRepository.class);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldReproIssueWithChangesVeloToTraffordBar() {
        validateAtLeastNJourney(1, TramStations.VeloPark, TramStations.TraffordBar, TramTime.of(8,0), when);
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
        Set<Journey> results = validateAtLeastNJourney(1, Altrincham, TramStations.Deansgate,
                TramTime.of(10, 15), when);
        results.forEach(journey -> {
            List<Location> callingPoints = journey.getPath();
            assertEquals(11, callingPoints.size());
            assertEquals(Altrincham.getId(), callingPoints.get(0).getId());
            assertEquals(Stations.Deansgate.getId(), callingPoints.get(10).getId());
        });
    }

    @Test
    void shouldHaveReasonableJourneyAltyToDeansgate() {
        TramServiceDate tramServiceDate = new TramServiceDate(when);
        JourneyRequest request = new JourneyRequest(tramServiceDate, TramTime.of(10, 15), false, 3, config.getMaxJourneyDuration());
        Set<Journey> results = RouteCalculatorTest.calculateRoute(calculator, stationRepository, txn, Altrincham,
                TramStations.Deansgate,
                request).collect(Collectors.toSet());
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
        validateAtLeastNJourney(1, Altrincham, TramStations.Rochdale, TramTime.of(9,0), when);
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
                config.getMaxJourneyDuration());
        Stream<Journey> stream = RouteCalculatorTest.calculateRoute(calculator, stationRepository, txn,
                Altrincham, TramStations.ManAirport,
                request);
        Set<Journey> results = stream.collect(Collectors.toSet());
        stream.close();

        assertEquals(0, results.size());
    }

    @Test
    void testJourneyFromAltyToAirport() {
        TramServiceDate today = new TramServiceDate(TestEnv.LocalNow().toLocalDate());

        JourneyRequest request = new JourneyRequest(today, TramTime.of(11, 43), false, 3, config.getMaxJourneyDuration());
        Stream<Journey> stream = RouteCalculatorTest.calculateRoute(calculator, stationRepository, txn,
                Altrincham, TramStations.ManAirport, request);
        Set<Journey> results = stream.collect(Collectors.toSet());
        stream.close();

        assertTrue(results.size()>0, "no results");    // results is iterator
        for (Journey result : results) {
            List<TransportStage> stages = result.getStages();
            assertEquals(2,stages.size());
            VehicleStage firstStage = (VehicleStage) stages.get(0);
            assertEquals(Altrincham.getId(), firstStage.getFirstStation().getId());
            assertEquals(Stations.TraffordBar.getId(), firstStage.getLastStation().getId());
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
        validateAtLeastNJourney(1, TramStations.Cornbrook, TramStations.ManAirport, TramTime.of(23,20), when);
    }

    @Disabled("Temporary: trams finish at 2300")
    @Test
    void shouldHandleCrossingMidnightDirect() {
        validateAtLeastNJourney(1, TramStations.Cornbrook, TramStations.StPetersSquare, TramTime.of(23,55), when);
        validateAtLeastNJourney(1, Altrincham, TramStations.TraffordBar, TramTime.of(23,51), when);
    }

    @Disabled("Temporary: trams finish at 2300")
    @Test
    void shouldHandleAfterMidnightDirect() {
        validateAtLeastNJourney(1, Altrincham, TramStations.NavigationRoad, TramTime.of(0,0), when);
    }

    @Test
    void shouldHaveHeatonParkToBurtonRoad() {
        validateAtLeastNJourney(1, HeatonPark, TramStations.BurtonRoad, TramTime.of(7, 30), when);
    }

    @Test
    void shouldReproIssueRochInterchangeToBury() {
        validateAtLeastNJourney(1, TramStations.Rochdale, TramStations.Bury, TramTime.of(9, 0), when);
    }

    @Test
    void shouldReproIssueWithMediaCityTrams() {
        TramTime time = TramTime.of(12, 0);

        validateAtLeastNJourney(1, TramStations.StPetersSquare, TramStations.MediaCityUK, time, when);
        validateAtLeastNJourney(1, TramStations.ExchangeSquare, TramStations.MediaCityUK, time, when);
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
        validateAtLeastNJourney(1, TramStations.Cornbrook, TramStations.StPetersSquare, time, when.plusDays(5));
    }

    @Test
    void shouldNotGenerateDuplicateJourneys() {

        Set<List<TransportStage>> stages = new HashSet<>();

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), TramTime.of(11, 45), false,
                4, config.getMaxJourneyDuration());
        Stream<Journey> stream = RouteCalculatorTest.calculateRoute(calculator, stationRepository, txn,
                Bury, Altrincham, request);
        Set<Journey> journeys = stream.limit(3).collect(Collectors.toSet());
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
        validateAtLeastNJourney(1, TramStations.Cornbrook, TramStations.Broadway, TramTime.of(8,0), when);
        validateAtLeastNJourney(1, TramStations.Cornbrook, TramStations.Eccles, TramTime.of(8,0), when);

        validateAtLeastNJourney(1, TramStations.Cornbrook, TramStations.Broadway, TramTime.of(9,0), when);
        validateAtLeastNJourney(1, TramStations.Cornbrook, TramStations.Eccles, TramTime.of(9,0), when);
    }

    @Test
    void shouldReproIssueWithJourneysToEccles() {
        validateAtLeastNJourney(1, TramStations.Bury, TramStations.Broadway, TramTime.of(9,0), when);
        validateAtLeastNJourney(1, TramStations.Bury, TramStations.Eccles, TramTime.of(9,0), when);
    }

    @Test
    void reproduceIssueEdgePerTrip() {
        // see also RouteCalculatorSubGraphTest
        validateAtLeastNJourney(1, TramStations.PiccadillyGardens, TramStations.Pomona, TramTime.of(19,48), when);
        validateAtLeastNJourney(1, TramStations.StPetersSquare, TramStations.Pomona, TramTime.of(19,51), when);
        validateAtLeastNJourney(1, TramStations.StPetersSquare, TramStations.Pomona, TramTime.of(19,56), when);
        validateAtLeastNJourney(1, TramStations.Cornbrook, TramStations.Eccles, TramTime.of(6,10), when);
    }

    @Test
    void shouldReproIssueWithStPetersToBeyondEcclesAt8AM() {
        List<TramTime> missingTimes = checkRangeOfTimes(TramStations.Cornbrook, TramStations.Eccles);
        assertTrue(missingTimes.isEmpty(), missingTimes.toString());
    }

    @Test
    void reproduceIssueWithImmediateDepartOffABoardedTram() {
        validateAtLeastNJourney(1, TramStations.Deansgate, TramStations.Ashton, TramTime.of(8,0), when);
    }

    @Test
    void reproduceIssueWithTramsSundayStPetersToDeansgate() {
        validateAtLeastNJourney(1, TramStations.StPetersSquare, TramStations.Deansgate, TramTime.of(9,0), TestEnv.nextSunday());
    }

    @Test
    void reproduceIssueWithTramsSundayAshtonToEccles() {
        validateAtLeastNJourney(1, TramStations.Ashton, TramStations.Eccles, TramTime.of(9,0), TestEnv.nextSunday());
    }

    @Test
    void reproduceIssueWithTramsSundayToFromEcclesAndCornbrook() {
        validateAtLeastNJourney(1, TramStations.Cornbrook, TramStations.Eccles, TramTime.of(9,0), TestEnv.nextSunday());
        validateAtLeastNJourney(1, TramStations.Eccles, TramStations.Cornbrook, TramTime.of(9,0), TestEnv.nextSunday());
    }

    @Test
    void shouldReproduceIssueCornbrookToAshtonSatursdays() {
        validateAtLeastNJourney(1, TramStations.Cornbrook, TramStations.Ashton, TramTime.of(9,0), TestEnv.nextSaturday());
    }

    @Test
    void shouldFindRouteVeloToHoltTownAt8RangeOfTimes() {
        for(int i=0; i<60; i++) {
            TramTime time = TramTime.of(8,i);
            validateAtLeastNJourney(1, TramStations.VeloPark, TramStations.HoltTown, time, when);
        }
    }

    @Test
    void reproIssueRochdaleToEccles() {
        validateAtLeastNJourney(1, TramStations.Rochdale, TramStations.Eccles, TramTime.of(9,0), when);
    }

    private void checkRouteNextNDays(TramStations start, TramStations dest, LocalDate date, TramTime time, int numDays) {
        if (!dest.equals(start)) {
            for(int day = 0; day< numDays; day++) {
                LocalDate testDate = avoidChristmasDate(date.plusDays(day));
                validateAtLeastNJourney(1, start, dest, time, testDate);
            }
        }
    }

    private Set<Journey> validateAtLeastNJourney(int maxToReturn, HasId<Station>  station, HasId<Station>  dest, TramTime time, LocalDate date) {
        return validateAtLeastNJourney(calculator, stationRepository, maxToReturn, txn, station, dest, time, date, 5, config.getMaxJourneyDuration());
    }

    public static Stream<Journey> calculateRoute(RouteCalculator calculator, StationRepository stationRepository, Transaction txn,
                                                 HasId<Station> start, HasId<Station> end, JourneyRequest request) {
        return calculator.calculateRoute(txn, stationRepository.getStationById(start.getId()),
                stationRepository.getStationById(end.getId()), request);
    }

    public static Set<Journey> validateAtLeastNJourney(RouteCalculator theCalculator, StationRepository stationRepository, int maxToReturn,
                                                       Transaction transaction,
                                                       HasId<Station> start, HasId<Station>  destination, TramTime time, LocalDate date, int maxChanges,
                                                       int maxJourneyDuration) {
        TramServiceDate queryDate = new TramServiceDate(date);
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(date), time, false, maxChanges, maxJourneyDuration);

        Stream<Journey> journeyStream = RouteCalculatorTest.calculateRoute(theCalculator, stationRepository, transaction,
                start, destination, journeyRequest);

        Set<Journey> journeys = journeyStream.limit(maxToReturn).collect(Collectors.toSet());
        journeyStream.close();

        String message = "from " + start.getId() + " to " + destination.getId() + " at " + time + " on " + queryDate;
        assertTrue(journeys.size() > 0, "Unable to find journey " + message);
        journeys.forEach(journey -> assertFalse(journey.getStages().isEmpty(), message + " missing stages for journey" + journey));
        journeys.forEach(RouteCalculatorTest::checkStages);
        return journeys;
    }

    @Deprecated
    public static Set<Journey> validateAtLeastNJourney(RouteCalculator theCalculator, StationRepository stationRepository, int maxToReturn, Transaction transaction,
                                                       Station start, Station destination, TramTime time, LocalDate date, int maxChanges,
                                                       int maxJourneyDuration) {
        TramServiceDate queryDate = new TramServiceDate(date);
        Stream<Journey> journeyStream = theCalculator.calculateRoute(transaction,
                stationRepository.getStationById(start.getId()),
                stationRepository.getStationById(destination.getId()), new JourneyRequest(new TramServiceDate(date), time,
                false, maxChanges, maxJourneyDuration));

        Set<Journey> journeys = journeyStream.limit(maxToReturn).collect(Collectors.toSet());
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

    private List<TramTime> checkRangeOfTimes(TramStations start, TramStations dest) {

        // TODO Lockdown TEMPORARY 23 Changed to 21
        // TODO lockdown services after 6.10
        int minsOffset = 10;
        List<TramTime> missing = new LinkedList<>();
        int latestHour = 21;
        for (int hour = 6; hour < latestHour; hour++) {
            for (int minutes = minsOffset; minutes < 59; minutes=minutes+5) {
                TramTime time = TramTime.of(hour, minutes);
                JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(when), time, false, 3,
                        config.getMaxJourneyDuration());
                Stream<Journey> journeys = RouteCalculatorTest.calculateRoute(calculator, stationRepository,txn,
                        start, dest, journeyRequest);
                if (journeys.limit(1).findFirst().isEmpty()) {
                    missing.add(time);
                }
                journeys.close();
            }
            minsOffset = 0;
        }
        return missing;
    }

    private Set<Journey> calculateRoutes(TramStations start, TramStations destination, TramTime queryTime, TramServiceDate today) {
        Stream<Journey> journeyStream = calculator.calculateRoute(txn,
                TestStation.real(stationRepository,start), TestStation.real(stationRepository,destination),
                new JourneyRequest(today, queryTime, false, 3, config.getMaxJourneyDuration()));
        Set<Journey> journeySet = journeyStream.collect(Collectors.toSet());
        journeyStream.close();
        return journeySet;
    }


}