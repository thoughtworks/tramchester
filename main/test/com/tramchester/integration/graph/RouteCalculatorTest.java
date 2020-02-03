package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.TestConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.NotInTransactionException;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.TestConfig.avoidChristmasDate;
import static org.junit.Assert.*;

public class RouteCalculatorTest {

    // TODO this needs to be > time for whole test fixture, see note below in @After
    public static final int TXN_TIMEOUT = 5*60;

    private static Dependencies dependencies;
    private static TramchesterConfig testConfig;
    private static GraphDatabaseService database;

    private RouteCalculator calculator;
    private LocalDate nextTuesday = TestConfig.nextTuesday(0);
    private Transaction tx;
    private Map<Long, Transaction> threadToTxnMap;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
        database = dependencies.get(GraphDatabaseService.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Before
    public void beforeEachTestRuns() {
        tx = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        calculator = dependencies.get(RouteCalculator.class);
        threadToTxnMap = new HashMap<>();
    }

    @After
    public void afterEachTestRuns() {
        tx.close();
        // can't close transactions on other threads because neo4j uses a threadlocal to cache the transaction
//        threadToTxnMap.values().forEach(Transaction::close);
        threadToTxnMap.clear();
    }

    @Test
    public void shouldHaveSimpleJourney() {
        validateAtLeastOneJourney(Stations.Altrincham, Stations.Cornbrook, TramTime.of(8, 0), nextTuesday);
    }

    @Test
    public void shouldReproIssueWithChangesVeloToTraffordBar() {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.TraffordBar, TramTime.of(8,0), nextTuesday);
    }

    @Test
    public void shouldHaveSimpleOneStopJourney() {
        checkRouteNextNDays(Stations.TraffordBar, Stations.Altrincham, nextTuesday, TramTime.of(9,0), 1);
    }

    @Test
    public void shouldHaveSimpleManyStopSameLineJourney() {
        checkRouteNextNDays(Stations.Altrincham, Stations.Cornbrook, nextTuesday, TramTime.of(9,0), 1);
    }

    @Test
    public void shouldHaveSimpleManyStopJourneyViaInterchange() {
        checkRouteNextNDays(Stations.Altrincham, Stations.Bury, nextTuesday, TramTime.of(9,0), 1);
    }

    @Test
    public void shouldFindInterchangesToEndOfLines() {
        Set<Pair<String, String>> combinations = createJourneyPairs(Stations.Interchanges,Stations.EndOfTheLine );
        checkRouteNextNDays(combinations, nextTuesday, TramTime.of(8,0), 7);
    }

    @Test
    public void shouldHaveReasonableJourneyAltyToDeansgate() {
        TramServiceDate today = new TramServiceDate(nextTuesday);
        Stream<Journey> results = calculator.calculateRoute(Stations.Altrincham.getId(), Stations.Deansgate.getId(),
                TramTime.of(10, 15), today);
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

    @Test
    public void shouldUseAllRoutesCorrectlWhenMultipleRoutesServDestination() {
        TramServiceDate today = new TramServiceDate(nextTuesday);

        Location start = Stations.Altrincham;

        Set<Journey> servedByBothRoutes = calculateRoutes(start, Stations.Deansgate, TramTime.of(10, 15), today);
        Set<Journey> altyToPiccGardens = calculateRoutes(start, Stations.PiccadillyGardens, TramTime.of(10, 15), today);
        Set<Journey> altyToMarketStreet = calculateRoutes(start, Stations.MarketStreet, TramTime.of(10, 15), today);

        assertEquals(altyToPiccGardens.size()+altyToMarketStreet.size(), servedByBothRoutes.size());
    }

    // over max wait, catch failure to accumulate journey times correctly
    @Test
    public void shouldHaveSimpleButLongJoruneySameRoute() {
        checkRouteNextNDays(Stations.ManAirport, Stations.TraffordBar, nextTuesday, TramTime.of(9,0), 1);
    }

    @Test
    public void shouldHaveLongJourneyAcross() {
        validateAtLeastOneJourney(Stations.Altrincham, Stations.Rochdale, TramTime.of(9,0), nextTuesday);
    }

    @Test
    public void shouldHaveReasonableLongJourneyAcrossFromInterchange() {
        TramTime am8 = TramTime.of(8, 0);

        Set<Journey> journeys = calculateRoutes(Stations.Monsall, Stations.RochdaleRail, am8, new TramServiceDate(nextTuesday));

        assertFalse(journeys.isEmpty());
        journeys.forEach(journey -> {
            // direct, or change at shaw
            assertTrue(journey.getStages().size()<=2);
        });
    }

    @Test
    public void shouldHaveSimpleManyStopJourneyStartAtInterchange() {
        checkRouteNextNDays(Stations.Cornbrook, Stations.Bury, nextTuesday, TramTime.of(9,0), 1);
    }

    @Test
    public void testJourneyFromAltyToAirport() {
        TramServiceDate today = new TramServiceDate(LocalDate.now());

        Stream<Journey> stream = calculator.calculateRoute(Stations.Altrincham.getId(), Stations.ManAirport.getId(),
                TramTime.of(11, 43), today);
        Set<Journey> results = stream.collect(Collectors.toSet());

        assertTrue(results.size()>0);    // results is iterator
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

    @Test
    public void shouldHandleCrossingMidnightWithChange() {
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.ManAirport, TramTime.of(23,20), nextTuesday);
    }

    @Test
    public void shouldHandleCrossingMidnightDirect() {
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.StPetersSquare, TramTime.of(23,55), nextTuesday);
        validateAtLeastOneJourney(Stations.Altrincham, Stations.TraffordBar, TramTime.of(23,51), nextTuesday);
    }

    @Test
    public void shouldHandleAfterMidnightDirect() {
        validateAtLeastOneJourney(Stations.Altrincham, Stations.NavigationRoad, TramTime.of(0,0), nextTuesday);
    }

    @Test
    public void shouldHaveHeatonParkToBurtonRoad() {
        validateAtLeastOneJourney("9400ZZMAHEA", "9400ZZMABNR", TramTime.of(6, 5),  nextTuesday);
    }

    @Test
    public void shouldReproIssueRochInterchangeToBury() {
        validateAtLeastOneJourney(Stations.Rochdale.getId(), Stations.Bury.getId(), TramTime.of(9, 0), nextTuesday );
    }

    @Test
    public void shouldReproIssueWithMediaCityTrams() {
        TramTime time = TramTime.of(12, 0);

        validateAtLeastOneJourney(Stations.StPetersSquare, Stations.MediaCityUK, time, nextTuesday);
        validateAtLeastOneJourney(Stations.ExchangeSquare, Stations.MediaCityUK, time, nextTuesday);
    }

    @Test
    public void shouldFindEndOfLinesToEndOfLines() {
        Set<Pair<String, String>> combinations = createJourneyPairs(Stations.EndOfTheLine, Stations.EndOfTheLine);
        checkRouteNextNDays(combinations, nextTuesday, TramTime.of(9,0), 7);
    }

    @Test
    @Ignore
    public void shouldFindEndOfLinesToEndOfLinesFindLongestDuration() {
        Set<Pair<String, String>> combinations = createJourneyPairs(Stations.EndOfTheLine, Stations.EndOfTheLine);

        List<Journey> allResults = new ArrayList<>();

        Map<Pair<String, String>, Optional<Journey>> results = validateAllHaveAtLeastOneJourney(nextTuesday,
                combinations, TramTime.of(9,0));
        results.forEach((route, journey) -> journey.ifPresent(allResults::add));

        double longest = allResults.stream().map(Journey::getTotalCost).max(Double::compare).get();
        assertEquals(testConfig.getMaxJourneyDuration(), longest, 0.001);
    }

    @Test
    public void shouldCheckCornbrookToStPetersSquareOnSundayMorning() {
        TramTime time = TramTime.of(11, 0);
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.StPetersSquare, time, nextTuesday.plusDays(5));
    }

    @Test
    public void shouldFindInterchangesToInterchanges() {
        Set<Pair<String, String>> combinations = createJourneyPairs(Stations.Interchanges, Stations.Interchanges);
        checkRouteNextNDays(combinations, nextTuesday, TramTime.of(9,0), 7);
    }

    @Test
    public void shouldFindEndOfLinesToInterchanges() {
        Set<Pair<String, String>> combinations = createJourneyPairs(Stations.EndOfTheLine, Stations.Interchanges);
        checkRouteNextNDays(combinations, nextTuesday, TramTime.of(9,0), 7);
    }

    @Test
    public void shouldNotGenerateDuplicateJourneys() {

        Set<List<TransportStage>> stages = new HashSet<>();

        Stream<Journey> stream = calculator.calculateRoute(Stations.Bury.getId(), Stations.Altrincham.getId(), TramTime.of(11,45),
                new TramServiceDate(nextTuesday));
        Set<Journey> journeys = stream.collect(Collectors.toSet());

        assertTrue(journeys.size()>0);

        journeys.forEach(journey -> {
            assertFalse(stages.toString(), stages.contains(journey.getStages()));
            stages.add(journey.getStages());
        });

    }

    @Test
    public void shouldHaveInAndAroundCornbrookToEccles8amTuesday() {
        LocalDate nextTuesday = TestConfig.nextTuesday(0);
        // catches issue with services, only some of which go to media city, while others direct to broadway
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.Broadway, TramTime.of(8,0), nextTuesday);
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.Eccles, TramTime.of(8,0), nextTuesday);

        validateAtLeastOneJourney(Stations.Cornbrook, Stations.Broadway, TramTime.of(9,0), nextTuesday);
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.Eccles, TramTime.of(9,0), nextTuesday);
    }

    @Test
    public void shouldReproIssueWithJourneysToEccles() {
        validateAtLeastOneJourney(Stations.Bury, Stations.Broadway, TramTime.of(9,0), nextTuesday);
        validateAtLeastOneJourney(Stations.Bury, Stations.Eccles, TramTime.of(9,0), nextTuesday);
    }

    @Test
    public void reproduceIssueEdgePerTrip() {
        // see also RouteCalculatorSubGraphTest
        validateAtLeastOneJourney(Stations.PiccadillyGardens, Stations.Pomona, TramTime.of(19,48), nextTuesday);
        validateAtLeastOneJourney(Stations.StPetersSquare, Stations.Pomona, TramTime.of(19,51), nextTuesday);
        validateAtLeastOneJourney(Stations.StPetersSquare, Stations.Pomona, TramTime.of(19,56), nextTuesday);
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.Eccles, TramTime.of(6,1), nextTuesday);
    }

    @Test
    public void shouldReproIssueWithStPetersToBeyondEcclesAt8AM() {
        assertEquals(0, checkRangeOfTimes(Stations.Cornbrook, Stations.Eccles));
    }

    @Test
    public void reproduceIssueWithImmediateDepartOffABoardedTram() {
        checkRouteNextNDays(Stations.Deansgate, Stations.Ashton, nextTuesday, TramTime.of(8,0), 7);
    }

    @Test
    public void reproduceIssueWithTramsSundayStPetersToDeansgate() {
        validateAtLeastOneJourney(Stations.StPetersSquare, Stations.Deansgate, TramTime.of(9,0), TestConfig.nextSunday());
    }

    @Test
    public void reproduceIssueWithTramsSundayAshtonToEccles() {
        validateAtLeastOneJourney(Stations.Ashton, Stations.Eccles, TramTime.of(9,0), TestConfig.nextSunday());
    }

    @Test
    public void reproduceIssueWithTramsSundayToFromEcclesAndCornbrook() {
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.Eccles, TramTime.of(9,0), TestConfig.nextSunday());
        validateAtLeastOneJourney(Stations.Eccles, Stations.Cornbrook, TramTime.of(9,0), TestConfig.nextSunday());
    }

    @Test
    public void shouldReproduceIssueCornbrookToAshtonSatursdays() {
        LocalDate date = TestConfig.nextSaturday();
        checkRouteNextNDays(Stations.Cornbrook, Stations.Ashton, date, TramTime.of(9,0), 7);
    }

    @Test
    public void shouldFindRouteVeloToHoltTownAt8RangeOfTimes() {
        for(int i=0; i<60; i++) {
            TramTime time = TramTime.of(8,i);
            validateAtLeastOneJourney(Stations.VeloPark, Stations.HoltTown, time, nextTuesday);
        }
    }

    @Test
    public void reproIssueRochdaleToEccles() {
        validateAtLeastOneJourney(Stations.Rochdale, Stations.Eccles, TramTime.of(9,0), nextTuesday);
    }

    private void checkRouteNextNDays(Location start, Location dest, LocalDate date, TramTime time, int numDays) {
        if (!dest.equals(start)) {
            for(int day = 0; day< numDays; day++) {
                LocalDate testDate = avoidChristmasDate(date.plusDays(day));
                validateAtLeastOneJourney(start, dest, time, testDate);
            }
        }
    }

    private void checkRouteNextNDays(Set<Pair<String,String>> combinations, LocalDate date, TramTime time, int numDays) {

        for(int day = 0; day< numDays; day++) {
            LocalDate testDate = avoidChristmasDate(date.plusDays(day));
            validateAllHaveAtLeastOneJourney(testDate, combinations, time);
        }
    }

    private void validateAtLeastOneJourney(Location start, Location dest, TramTime time, LocalDate date) {
        validateAtLeastOneJourney(start.getId(), dest.getId(), time, date);
    }

    private void validateAtLeastOneJourney(String startId, String destId, TramTime time, LocalDate date) {
        validateAtLeastOneJourney(calculator, startId, destId, time, date);
    }

    public static void validateAtLeastOneJourney(RouteCalculator theCalculator, String startId, String destId,
                                                 TramTime time, LocalDate date) {
        TramServiceDate queryDate = new TramServiceDate(date);
        Set<Journey> journeys = theCalculator.calculateRoute(startId, destId, time,
                new TramServiceDate(date)).
                limit(1).collect(Collectors.toSet());

        String message = String.format("from %s to %s at %s on %s", startId, destId, time, queryDate);
        assertTrue("Unable to find journey " + message, journeys.size() > 0);
        journeys.forEach(journey -> assertFalse(message + " missing stages for journey" + journey, journey.getStages().isEmpty()));
        journeys.forEach(RouteCalculatorTest::checkStages);
    }

    private Set<Pair<String, String>> createJourneyPairs(List<Station> starts, List<Station> ends) {
        Set<Pair<String,String>> combinations = new HashSet<>();
        for (Location start : starts) {
            for (Location dest : ends) {
                if (!dest.equals(start)) {
                    combinations.add(Pair.of(start.getId(), dest.getId()));
                }
            }
        }
        return combinations;
    }

    private static void checkStages(Journey journey) {
        List<TransportStage> stages = journey.getStages();
        TramTime earliestAtNextStage = null;
        for (TransportStage stage : stages) {
            if (earliestAtNextStage!=null) {
                assertFalse(stage.toString() + " arrived before " + earliestAtNextStage,
                        stage.getFirstDepartureTime().isBefore(earliestAtNextStage));
            }
            earliestAtNextStage = stage.getFirstDepartureTime().plusMinutes(stage.getDuration());
        }
    }

    private int checkRangeOfTimes(Location start, Location dest) {

        List<TramTime> missing = new LinkedList<>();
        for (int hour = 6; hour < 23; hour++) {
            for (int minutes = 0; minutes < 59; minutes=minutes+5) {
                TramTime time = TramTime.of(hour, minutes);
                Stream<Journey> journeys = calculator.calculateRoute(start.getId(), dest.getId(),
                        time, new TramServiceDate(nextTuesday));
                if (!journeys.limit(1).findFirst().isPresent()) {
                    missing.add(time);
                }
            }

        }
        return missing.size();
    }

    private Map<Pair<String, String>, Optional<Journey>> validateAllHaveAtLeastOneJourney(
            LocalDate queryDate, Set<Pair<String, String>> combinations, TramTime queryTime) {

        // check each pair, collect results into (station,station)->result
        Map<Pair<String, String>, Optional<Journey>> results =
                combinations.parallelStream().
                        map(this::checkForTx).
                        map(journey -> Pair.of(journey,
                                calculator.calculateRoute(journey.getLeft(), journey.getRight(), queryTime,
                                        new TramServiceDate(queryDate)).limit(1).
                                        findAny())).
                        collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

        // check all results present, collect failures into a list
        List<Pair<String, String>> failed = results.entrySet().stream().
                filter(journey -> !journey.getValue().isPresent()).
                map(Map.Entry::getKey).
                map(pair -> Pair.of(pair.getLeft(), pair.getRight())).
                collect(Collectors.toList());
        assertEquals(failed.toString(), 0L, failed.size());
        return results;
    }

    private Pair<String, String>  checkForTx(Pair<String, String> journey) {
        long id = Thread.currentThread().getId();
        if (threadToTxnMap.containsKey(id)) {
            return journey;
        }

        try {
            database.getNodeById(1);
        }
        catch (NotInTransactionException noTxnForThisThread) {
            Transaction txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
            threadToTxnMap.put(id, txn);
        }
        catch(Exception uncaught) {
            throw uncaught;
        }
        return journey;
    }

    private Set<Journey> calculateRoutes(Location start, Location destination, TramTime queryTime, TramServiceDate today) {
        return calculator.calculateRoute(start.getId(), destination.getId(), queryTime, today).collect(Collectors.toSet());
    }

}