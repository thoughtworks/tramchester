package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.places.Location;
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
import org.apache.commons.lang3.tuple.Pair;
import org.junit.*;
import org.neo4j.graphdb.NotInTransactionException;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.Stations.HeatonPark;
import static com.tramchester.testSupport.TestEnv.avoidChristmasDate;
import static org.junit.Assert.*;

public class RouteCalculatorTest {

    // TODO this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static Dependencies dependencies;
    private static TramchesterConfig testConfig;
    private static GraphDatabase database;

    private RouteCalculator calculator;
    private LocalDate nextTuesday = TestEnv.nextTuesday(0);
    private Transaction tx;
    private Map<Long, Transaction> threadToTxnMap;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
        database = dependencies.get(GraphDatabase.class);
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
        Set<Pair<Station, Station>> combinations = createJourneyPairs(Stations.Interchanges,Stations.EndOfTheLine );
        checkRouteNextNDays(combinations, nextTuesday, TramTime.of(8,0), 7);
    }

    @Test
    public void shouldHaveReasonableJourneyAltyToDeansgate() {
        TramServiceDate today = new TramServiceDate(nextTuesday);
        Stream<Journey> results = calculator.calculateRoute(Stations.Altrincham, Stations.Deansgate,
                new JourneyRequest(today, TramTime.of(10, 15), false));
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

    @Ignore("Failing due to temporarily less frequency service")
    @Test
    public void shouldUseAllRoutesCorrectlWhenMultipleRoutesServDestination() {
        TramServiceDate today = new TramServiceDate(nextTuesday);

        Station start = Stations.Altrincham;

        TramTime queryTime = TramTime.of(10, 21);
        Set<Journey> servedByBothRoutes = calculateRoutes(start, Stations.Deansgate, queryTime, today);

        Set<Journey> altyToPiccGardens = calculateRoutes(start, Stations.PiccadillyGardens, queryTime, today);
        Set<Journey> altyToMarketStreet = calculateRoutes(start, Stations.MarketStreet, queryTime, today);

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
    public void shouldLimitNumberChangesResultsInNoJourneys() {
        TramServiceDate today = new TramServiceDate(TestEnv.LocalNow().toLocalDate());

        Stream<Journey> stream = calculator.calculateRoute(Stations.Altrincham, Stations.ManAirport,
                new JourneyRequest(today, TramTime.of(11, 43), false, 0));
        Set<Journey> results = stream.collect(Collectors.toSet());
        stream.close();

        assertEquals(0, results.size());
    }

    @Test
    public void testJourneyFromAltyToAirport() {
        TramServiceDate today = new TramServiceDate(TestEnv.LocalNow().toLocalDate());

        Stream<Journey> stream = calculator.calculateRoute(Stations.Altrincham, Stations.ManAirport,
                new JourneyRequest(today, TramTime.of(11, 43), false));
        Set<Journey> results = stream.collect(Collectors.toSet());
        stream.close();

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

    @Ignore("Temporary: trams finish at 2300")
    @Test
    public void shouldHandleCrossingMidnightWithChange() {
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.ManAirport, TramTime.of(23,20), nextTuesday);
    }

    @Ignore("Temporary: trams finish at 2300")
    @Test
    public void shouldHandleCrossingMidnightDirect() {
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.StPetersSquare, TramTime.of(23,55), nextTuesday);
        validateAtLeastOneJourney(Stations.Altrincham, Stations.TraffordBar, TramTime.of(23,51), nextTuesday);
    }

    @Ignore("Temporary: trams finish at 2300")
    @Test
    public void shouldHandleAfterMidnightDirect() {
        validateAtLeastOneJourney(Stations.Altrincham, Stations.NavigationRoad, TramTime.of(0,0), nextTuesday);
    }

    @Test
    public void shouldHaveHeatonParkToBurtonRoad() {
        validateAtLeastOneJourney(HeatonPark, Stations.BurtonRoad, TramTime.of(7, 30),  nextTuesday);
    }

    @Test
    public void shouldReproIssueRochInterchangeToBury() {
        validateAtLeastOneJourney(Stations.Rochdale, Stations.Bury, TramTime.of(9, 0), nextTuesday );
    }

    @Test
    public void shouldReproIssueWithMediaCityTrams() {
        TramTime time = TramTime.of(12, 0);

        validateAtLeastOneJourney(Stations.StPetersSquare, Stations.MediaCityUK, time, nextTuesday);
        validateAtLeastOneJourney(Stations.ExchangeSquare, Stations.MediaCityUK, time, nextTuesday);
    }

    @Test
    public void shouldFindEndOfLinesToEndOfLines() {
        // todo: changed from 9 to 10.15 as airport to eccles fails for 10.15am
        Set<Pair<Station, Station>> combinations = createJourneyPairs(Stations.EndOfTheLine, Stations.EndOfTheLine);
        checkRouteNextNDays(combinations, nextTuesday, TramTime.of(10,15), 7);
    }

    @Test
    @Ignore
    public void shouldFindEndOfLinesToEndOfLinesFindLongestDuration() {
        Set<Pair<Station, Station>> combinations = createJourneyPairs(Stations.EndOfTheLine, Stations.EndOfTheLine);

        List<Journey> allResults = new ArrayList<>();

        Map<Pair<Station, Station>, JourneyOrNot> results = validateAllHaveAtLeastOneJourney(nextTuesday,
                combinations, TramTime.of(9,0));
        results.forEach((route, journey) -> journey.ifPresent(allResults::add));

        double longest = allResults.stream().map(RouteCalculatorTest::costOfJourney).max(Integer::compare).get();
        assertEquals(testConfig.getMaxJourneyDuration(), longest, 0.001);

    }

    public static int costOfJourney(Journey journey) {
        List<TransportStage> stages = journey.getStages();
        TramTime departs = stages.get(0).getFirstDepartureTime();
        TramTime arrive = stages.get(stages.size() - 1).getExpectedArrivalTime();

        return TramTime.diffenceAsMinutes(departs, arrive);
    }

    @Test
    public void shouldCheckCornbrookToStPetersSquareOnSundayMorning() {
        TramTime time = TramTime.of(11, 0);
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.StPetersSquare, time, nextTuesday.plusDays(5));
    }

    @Test
    public void shouldFindInterchangesToInterchanges() {
        Set<Pair<Station, Station>> combinations = createJourneyPairs(Stations.Interchanges, Stations.Interchanges);
        checkRouteNextNDays(combinations, nextTuesday, TramTime.of(9,0), 7);
    }

    @Test
    public void shouldFindEndOfLinesToInterchanges() {
        Set<Pair<Station, Station>> combinations = createJourneyPairs(Stations.EndOfTheLine, Stations.Interchanges);
        checkRouteNextNDays(combinations, nextTuesday, TramTime.of(9,0), 7);
    }

    @Test
    public void shouldNotGenerateDuplicateJourneys() {

        Set<List<TransportStage>> stages = new HashSet<>();

        Stream<Journey> stream = calculator.calculateRoute(Stations.Bury, Stations.Altrincham,
                new JourneyRequest(new TramServiceDate(nextTuesday), TramTime.of(11,45), false));
        Set<Journey> journeys = stream.collect(Collectors.toSet());
        stream.close();

        assertTrue(journeys.size()>0);

        journeys.forEach(journey -> {
            assertFalse(stages.toString(), stages.contains(journey.getStages()));
            stages.add(journey.getStages());
        });

    }

    @Test
    public void shouldHaveInAndAroundCornbrookToEccles8amTuesday() {
        LocalDate nextTuesday = TestEnv.nextTuesday(0);
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
        List<TramTime> missingTimes = checkRangeOfTimes(Stations.Cornbrook, Stations.Eccles);
        assertTrue(missingTimes.toString(), missingTimes.isEmpty());
    }

    @Test
    public void reproduceIssueWithImmediateDepartOffABoardedTram() {
        checkRouteNextNDays(Stations.Deansgate, Stations.Ashton, nextTuesday, TramTime.of(8,0), 7);
    }

    @Test
    public void reproduceIssueWithTramsSundayStPetersToDeansgate() {
        validateAtLeastOneJourney(Stations.StPetersSquare, Stations.Deansgate, TramTime.of(9,0), TestEnv.nextSunday());
    }

    @Test
    public void reproduceIssueWithTramsSundayAshtonToEccles() {
        validateAtLeastOneJourney(Stations.Ashton, Stations.Eccles, TramTime.of(9,0), TestEnv.nextSunday());
    }

    @Test
    public void reproduceIssueWithTramsSundayToFromEcclesAndCornbrook() {
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.Eccles, TramTime.of(9,0), TestEnv.nextSunday());
        validateAtLeastOneJourney(Stations.Eccles, Stations.Cornbrook, TramTime.of(9,0), TestEnv.nextSunday());
    }

    @Test
    public void shouldReproduceIssueCornbrookToAshtonSatursdays() {
        LocalDate date = TestEnv.nextSaturday();
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

    private void checkRouteNextNDays(Station start, Station dest, LocalDate date, TramTime time, int numDays) {
        if (!dest.equals(start)) {
            for(int day = 0; day< numDays; day++) {
                LocalDate testDate = avoidChristmasDate(date.plusDays(day));
                validateAtLeastOneJourney(start, dest, time, testDate);
            }
        }
    }

    private void checkRouteNextNDays(Set<Pair<Station,Station>> combinations, LocalDate date, TramTime time, int numDays) {

        for(int day = 0; day< numDays; day++) {
            LocalDate testDate = avoidChristmasDate(date.plusDays(day));
            validateAllHaveAtLeastOneJourney(testDate, combinations, time);
        }
    }

//    private void validateAtLeastOneJourney(Location start, Station dest, TramTime time, LocalDate date) {
//        validateAtLeastOneJourney(start, dest, time, date);
//    }

    private void validateAtLeastOneJourney(Station station, Station dest, TramTime time, LocalDate date) {
        validateAtLeastOneJourney(calculator, station, dest, time, date, 5);
    }

    public static Set<Journey> validateAtLeastOneJourney(RouteCalculator theCalculator, Station start, Station destination,
                                                         TramTime time, LocalDate date, int maxChanges) {
        TramServiceDate queryDate = new TramServiceDate(date);
        Stream<Journey> journeyStream = theCalculator.calculateRoute(start, destination, new JourneyRequest(new TramServiceDate(date), time,
                false, maxChanges));
        Set<Journey> journeys = journeyStream.limit(1).collect(Collectors.toSet());
        journeyStream.close();

        String message = "from " + start + " to " + destination + " at " + time + " on " + queryDate;
        assertTrue("Unable to find journey " + message, journeys.size() > 0);
        journeys.forEach(journey -> assertFalse(message + " missing stages for journey" + journey, journey.getStages().isEmpty()));
        journeys.forEach(RouteCalculatorTest::checkStages);
        return journeys;
    }

    // TODO to station, station
    private Set<Pair<Station, Station>> createJourneyPairs(List<Station> starts, List<Station> ends) {
        Set<Pair<Station, Station>> combinations = new HashSet<>();
        for (Station start : starts) {
            for (Station dest : ends) {
                if (!dest.equals(start)) {
                    combinations.add(Pair.of(start, dest));
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

    private List<TramTime> checkRangeOfTimes(Station start, Station dest) {

        // TODO TEMPORARY 23 Changed to 21
        List<TramTime> missing = new LinkedList<>();
        int latestHour = 21;
        for (int hour = 6; hour < latestHour; hour++) {
            for (int minutes = 0; minutes < 59; minutes=minutes+5) {
                TramTime time = TramTime.of(hour, minutes);
                Stream<Journey> journeys = calculator.calculateRoute(start, dest,
                        new JourneyRequest(new TramServiceDate(nextTuesday), time, false));
                if (journeys.limit(1).findFirst().isEmpty()) {
                    missing.add(time);
                }
                journeys.close();
            }

        }
        return missing;
    }

    private Map<Pair<Station, Station>, JourneyOrNot> validateAllHaveAtLeastOneJourney(
            LocalDate queryDate, Set<Pair<Station, Station>> combinations, TramTime queryTime) {

        // check each pair, collect results into (station,station)->result
        Map<Pair<Station, Station>, JourneyOrNot> results =
                combinations.parallelStream().
                        map(this::checkForTx).
                        map(requested -> {
                            Optional<Journey> optionalJourney = calculator.calculateRoute(requested.getLeft(), requested.getRight(),
                                    new JourneyRequest(new TramServiceDate(queryDate), queryTime, false)).limit(1).findAny();
                            JourneyOrNot journeyOrNot = new JourneyOrNot(requested, queryDate, queryTime, optionalJourney);
                            return Pair.of(requested, journeyOrNot); } ).
                        collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

        // check all results present, collect failures into a list
        List<JourneyOrNot> failed = results.entrySet().stream().
                filter(journeyOrNot -> journeyOrNot.getValue().missing()).
                map(Map.Entry::getValue).
                collect(Collectors.toList());
        assertTrue("missing routes: " + failed.toString(), failed.isEmpty());
        return results;
    }

    private  <A,B> Pair<A, B> checkForTx(Pair<A, B> journey) {
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

        return journey;
    }

    private Set<Journey> calculateRoutes(Station start, Station destination, TramTime queryTime, TramServiceDate today) {
        Stream<Journey> journeyStream = calculator.calculateRoute(start, destination, new JourneyRequest(today, queryTime, false));
        Set<Journey> journeySet = journeyStream.collect(Collectors.toSet());
        journeyStream.close();
        return journeySet;
    }

    private static class JourneyOrNot {
        private final Pair<Station, Station> requested;
        private final LocalDate queryDate;
        private final TramTime queryTime;
        private final Journey journey;

        public JourneyOrNot(Pair<Station, Station> requested, LocalDate queryDate, TramTime queryTime,
                            Optional<Journey> optionalJourney) {
            this.requested = requested;
            this.queryDate = queryDate;
            this.queryTime = queryTime;
            this.journey = optionalJourney.orElse(null);
        }

        public boolean missing() {
            return journey==null;
        }

        public void ifPresent(Consumer<Journey> action) {
            if (this.journey != null) {
                action.accept(this.journey);
            }

        }

        @Override
        public String toString() {
            return "JourneyOrNot{" +
                    " queryDate=" + queryDate +
                    ", queryTime=" + queryTime +
                    ", requested=" + requested +
                    '}';
        }
    }
}