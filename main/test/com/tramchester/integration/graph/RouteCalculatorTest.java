package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.TestConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import com.tramchester.repository.TransportData;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.NotInTransactionException;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.TestConfig.avoidChristmasDate;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class RouteCalculatorTest {

    public static final int TXN_TIMEOUT = 180;
    private static Dependencies dependencies;
    private static TramchesterConfig testConfig;
    private static GraphDatabaseService database;

    private RouteCalculator calculator;
    private LocalDate nextTuesday = TestConfig.nextTuesday(0);
    private static boolean edgePerTrip;
    private Transaction tx;
    private Map<Long, Transaction> threadToTxnMap;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
        edgePerTrip = testConfig.getEdgePerTrip();
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
//        threadToTxnMap.values().forEach(Transaction::close);
        threadToTxnMap.clear();
    }

    @Test
    public void shouldHaveFailingTestForEdgePerTrip() {
        assertFalse(new IntegrationTramTestConfig().getEdgePerTrip());
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
        List<TramTime> queryTimes = Collections.singletonList(TramTime.of(10, 15));
        TramServiceDate today = new TramServiceDate(nextTuesday);
        Stream<RawJourney> results = calculator.calculateRoute(Stations.Altrincham.getId(), Stations.Deansgate.getId(),
                queryTimes, today, RouteCalculator.MAX_NUM_GRAPH_PATHS);
        results.forEach(journey -> {
            assertEquals(1, journey.getStages().size()); // should be one stage only
            journey.getStages().stream().
                    map(raw -> (RawVehicleStage) raw).
                    map(RawVehicleStage::getCost).
                    forEach(cost -> assertTrue(cost>0));
            Optional<Integer> total = journey.getStages().stream().
                    map(raw -> (RawVehicleStage) raw).
                    map(RawVehicleStage::getCost).
                    reduce(Integer::sum);
            assertTrue(total.isPresent());
            assertTrue(total.get()>20);
        });
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
    public void shouldHaveLongJourneyAcrossFromInterchange() {
        TramTime am8 = TramTime.of(8, 0);
        String rochdaleRail = "9400ZZMARRS";
        String monsall = "9400ZZMAMON";
        validateAtLeastOneJourney(monsall, rochdaleRail, am8, nextTuesday);
        validateAtLeastOneJourney(Stations.PiccadillyGardens, Stations.Rochdale, am8, nextTuesday);
    }

    @Test
    public void shouldHaveSimpleManyStopJourneyStartAtInterchange() {
        checkRouteNextNDays(Stations.Cornbrook, Stations.Bury, nextTuesday, TramTime.of(9,0), 1);
    }

    @Test
    public void testJourneyFromAltyToAirport() {
        List<TramTime> queryTimes = Collections.singletonList(TramTime.of(11, 43));
        TramServiceDate today = new TramServiceDate(LocalDate.now());

        Stream<RawJourney> stream = calculator.calculateRoute(Stations.Altrincham.getId(), Stations.ManAirport.getId(),
                queryTimes, today, RouteCalculator.MAX_NUM_GRAPH_PATHS);
        Set<RawJourney> results = stream.collect(Collectors.toSet());

        assertTrue(results.size()>0);    // results is iterator
        for (RawJourney result : results) {
            List<RawStage> stages = result.getStages();
            assertEquals(2,stages.size());
            RawVehicleStage firstStage = (RawVehicleStage) stages.get(0);
            assertEquals(Stations.Altrincham, firstStage.getFirstStation());
            assertEquals(Stations.TraffordBar, firstStage.getLastStation());
            assertEquals(TransportMode.Tram, firstStage.getMode());
            assertEquals(7, firstStage.getPassedStops());

            RawVehicleStage finalStage = (RawVehicleStage) stages.get(stages.size()-1);
            //assertEquals(Stations.TraffordBar, secondStage.getFirstStation()); // THIS CAN CHANGE
            assertEquals(Stations.ManAirport, finalStage.getLastStation());
            assertEquals(TransportMode.Tram, finalStage.getMode());
        }
    }

    @Test
    public void shouldHandleCrossingMidnight() {
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.ManAirport, TramTime.of(23, 15), nextTuesday);
    }

    @Test
    public void shouldHaveHeatonParkToBurtonRoad() {
        validateAtLeastOneJourney("9400ZZMAHEA", "9400ZZMABNR", TramTime.of(6, 5),  nextTuesday);
    }

    @Test
    public void shouldFindRouteEachStationToEveryOtherStream() {

        TransportData data = dependencies.get(TransportData.class);

        Set<Station> allStations = data.getStations();

        // pairs of stations to check
        Set<Pair<String, String>> combinations = allStations.stream().map(start -> allStations.stream().
                map(dest -> Pair.of(start, dest))).
                flatMap(Function.identity()).
                filter(pair -> !matches(pair, Stations.Interchanges)).
                filter(pair -> !matches(pair, Stations.EndOfTheLine)).
                map(pair -> Pair.of(pair.getLeft().getId(), pair.getRight().getId())).
                collect(Collectors.toSet());

        List<TramTime> queryTimes = Collections.singletonList(TramTime.of(6, 5));
        ConcurrentMap<Pair<String, String>, Optional<RawJourney>> results = validateAllHaveAtLeastOneJourney(nextTuesday, combinations, queryTimes);

        // now find longest journey
        Optional<Integer> maxNumberStops = results.values().stream().
                filter(Optional::isPresent).
                map(Optional::get).
                map(journey -> journey.getStages().stream().
                        map(RawStage::getPassedStops).
                        reduce(Integer::sum)).
                filter(Optional::isPresent).
                map(Optional::get).
                max(Integer::compare);

        assertTrue(maxNumberStops.isPresent());
        assertEquals(39, maxNumberStops.get().intValue());
    }

    @Test
    public void shouldFindEndOfLinesToEndOfLinesTuesday() {
        Set<Pair<String, String>> combinations = createJourneyPairs(Stations.EndOfTheLine, Stations.EndOfTheLine);

        List<TramTime> times = Arrays.asList(TramTime.of(9, 0));
        validateAllHaveAtLeastOneJourney(nextTuesday, combinations, times);

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

        for(int day = 0; day < 7; day++) {
            List<TramTime> times = Arrays.asList(TramTime.of(9,0));
            validateAllHaveAtLeastOneJourney(nextTuesday.plusDays(day), combinations, times);
        }
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
        assumeTrue(edgePerTrip);

        Set<List<RawStage>> stages = new HashSet<>();

        List<TramTime> queryTimes = new LinkedList<>();
        queryTimes.add(TramTime.of(11,45));
        Stream<RawJourney> stream = calculator.calculateRoute(Stations.Bury.getId(), Stations.Altrincham.getId(), queryTimes,
                new TramServiceDate(nextTuesday), 100);
        Set<RawJourney> journeys = stream.collect(Collectors.toSet());

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
        assertEquals(0,checkRangeOfTimes(Stations.Cornbrook, Stations.Eccles));
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
        List<TramTime> times = Arrays.asList(time);

        for(int day = 0; day< numDays; day++) {
            LocalDate testDate = avoidChristmasDate(date.plusDays(day));
            validateAllHaveAtLeastOneJourney(testDate, combinations, times);
        }
    }

    private void validateAtLeastOneJourney(Location start, Location dest, TramTime time, LocalDate date) {
        validateAtLeastOneJourney(start.getId(), dest.getId(), time, date);
    }

    private void validateAtLeastOneJourney(String startId, String destId, TramTime time, LocalDate date) {
        validateAtLeastOneJourney(calculator, startId, destId, time, date, testConfig.getEdgePerTrip());
    }

    public static void validateAtLeastOneJourney(RouteCalculator theCalculator, String startId, String destId,
                                                 TramTime time, LocalDate date, boolean edgePerTrip) {
        TramServiceDate queryDate = new TramServiceDate(date);
        Set<RawJourney> journeys = theCalculator.calculateRoute(startId, destId, Collections.singletonList(time),
                new TramServiceDate(date), RouteCalculator.MAX_NUM_GRAPH_PATHS).
                collect(Collectors.toSet());

        String message = String.format("from %s to %s at %s on %s", startId, destId, time, queryDate);
        assertTrue("Unable to find journey " + message, journeys.size() > 0);
        journeys.forEach(journey -> assertFalse(message + " missing stages for journey" + journey, journey.getStages().isEmpty()));
        if (edgePerTrip) {
            journeys.forEach(RouteCalculatorTest::checkStages);
        }
    }

    private Set<Pair<String, String>> createJourneyPairs(List<Location> starts, List<Location> ends) {
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

    private static void checkStages(RawJourney journey) {
        List<RawStage> stages = journey.getStages();
        TramTime earliestAtNextStage = null;
        for (RawStage stage : stages) {
            RawVehicleStage transportStage = (RawVehicleStage) stage;
            if (stage != null) {
                if (earliestAtNextStage!=null) {
                    assertFalse(transportStage.toString() + " arrived before " + earliestAtNextStage,
                            transportStage.getDepartTime().isBefore(earliestAtNextStage));
                }
                earliestAtNextStage = transportStage.getDepartTime().plusMinutes(transportStage.getCost());
            }
        }
    }

    private int checkRangeOfTimes(Location start, Location dest) {

        List<TramTime> missing = new LinkedList<>();
        for (int hour = 6; hour < 23; hour++) {
            for (int minutes = 0; minutes < 59; minutes=minutes+5) {
                TramTime time = TramTime.of(hour, minutes);
                Stream<RawJourney> journeys = calculator.calculateRoute(start.getId(), dest.getId(),
                        Collections.singletonList(time), new TramServiceDate(nextTuesday), RouteCalculator.MAX_NUM_GRAPH_PATHS);
                if (!journeys.findFirst().isPresent()) {
                    missing.add(time);
                }
            }

        }
        return missing.size();
    }

    private ConcurrentMap<Pair<String, String>, Optional<RawJourney>> validateAllHaveAtLeastOneJourney(
            LocalDate queryDate, Set<Pair<String, String>> combinations, List<TramTime> queryTimes) {

        // check each pair, collect results into (station,station)->result
        ConcurrentMap<Pair<String, String>, Optional<RawJourney>> results =
                combinations.parallelStream().
                        map(this::checkForTx).
                        map(journey -> Pair.of(journey,
                                calculator.calculateRoute(journey.getLeft(), journey.getRight(), queryTimes,
                                        new TramServiceDate(queryDate), RouteCalculator.MAX_NUM_GRAPH_PATHS).limit(1).
                                        findFirst())).
                        collect(Collectors.toConcurrentMap(Pair::getLeft, Pair::getRight));

        // check all results present, collect failures into a list
        List<Pair<String, String>> failed = results.entrySet().stream().
                filter(journey -> !journey.getValue().isPresent()).
                map(Map.Entry::getKey).
                map(pair -> Pair.of(pair.getLeft(), pair.getRight())).
                collect(Collectors.toList());
        assertEquals(failed.toString(), 0L, failed.size());
        return results;
    }

    private boolean matches(Pair<Station, Station> locationPair, List<Location> locations) {
        return locations.contains(locationPair.getLeft()) && locations.contains(locationPair.getRight());
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


}