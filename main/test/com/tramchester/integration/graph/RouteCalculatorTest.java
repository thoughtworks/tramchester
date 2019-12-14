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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class RouteCalculatorTest {

    private static Dependencies dependencies;
    private static TramchesterConfig testConfig;

    private RouteCalculator calculator;
    private LocalDate nextTuesday = TestConfig.nextTuesday(0);
    private static boolean edgePerTrip;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
        edgePerTrip = testConfig.getEdgePerTrip();
    }

    @Before
    public void beforeEachTestRuns() {
        calculator = dependencies.get(RouteCalculator.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
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
    public void shouldHaveReasonableJourneyAltyToDeansgate() {
        List<TramTime> queryTimes = Collections.singletonList(TramTime.of(10, 15));
        TramServiceDate today = new TramServiceDate(nextTuesday);
        Set<RawJourney> results = calculator.calculateRoute(Stations.Altrincham.getId(), Stations.Deansgate.getId(),
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

        Set<RawJourney> results = calculator.calculateRoute(Stations.Altrincham.getId(), Stations.ManAirport.getId(),
                queryTimes, today, RouteCalculator.MAX_NUM_GRAPH_PATHS);

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

    @Test(timeout=60000)
    public void shouldFindRouteEachStationToEveryOtherStream() {

        TramServiceDate queryDate = new TramServiceDate(nextTuesday);
        TransportData data = dependencies.get(TransportData.class);

        Set<Station> allStations = data.getStations();
        List<Pair<Location,Location>> combinations = new LinkedList<>();

        for (Location start : allStations) {
            for (Location end : allStations) {
                if (!start.equals(end)) {
                    combinations.add(Pair.of(start,end));
                }
            }
        }

        List<TramTime> queryTimes = Collections.singletonList(TramTime.of(6, 5));

        Map<Pair<Location, Location>, Set<RawJourney>> allJourneys = combinations.parallelStream().
                map(stations -> Pair.of(stations, calc(stations, queryTimes, queryDate))).
                        collect(Collectors.toConcurrentMap(Pair::getLeft, Pair::getRight));

        List<Pair<String, String>> failed = allJourneys.entrySet().
                stream().
                filter(journey -> journey.getValue().size() == 0).
                map(Map.Entry::getKey).
                map(pair -> Pair.of(pair.getLeft().getName(), pair.getRight().getName())).
                collect(Collectors.toList());
        //long failed = allJourneys.values().stream().filter(rawJourneys -> rawJourneys.size() == 0).count();
        assertEquals(failed.toString(), 0L, failed.size());

        Set<Integer> passedStops = allJourneys.values().stream().
                flatMap(Collection::stream).
                map(journey ->
                        journey.getStages().stream().
                                map(RawStage::getPassedStops).
                                reduce(Integer::sum)).
                    filter(Optional::isPresent).
                    map(Optional::get).
                    collect(Collectors.toSet());

        Integer[] results = new Integer[passedStops.size()];
        passedStops.toArray(results);
        int longest = results[results.length - 1];
        assertEquals(40,longest);

    }

    private Set<RawJourney> calc(Pair<Location, Location> pair, List<TramTime> queryTimes, TramServiceDate queryDate) {
        return calculator.calculateRoute(pair.getLeft().getId(), pair.getRight().getId(), queryTimes, queryDate, RouteCalculator.MAX_NUM_GRAPH_PATHS);
    }

    @Test
    public void shouldFindEndOfLinesToEndOfLinesTuesday() {
        for (Location start : Stations.EndOfTheLine) {
            for (Location dest : Stations.EndOfTheLine) {
                if (!dest.equals(start)) {
                    validateAtLeastOneJourney(start, dest, TramTime.of(9, 0), nextTuesday);
                }
            }
        }
    }

    @Test
    public void shouldReproIssueWithMediaCityTrams() {
        TramTime time = TramTime.of(12, 0);

        validateAtLeastOneJourney(Stations.StPetersSquare, Stations.MediaCityUK, time, nextTuesday);
        validateAtLeastOneJourney(Stations.ExchangeSquare, Stations.MediaCityUK, time, nextTuesday);
    }

    @Test
    public void shouldFindEndOfLinesToEndOfLines() {
        for (Location start : Stations.EndOfTheLine) {
            for (Location dest : Stations.EndOfTheLine) {
                checkRouteNextNDays(start, dest, nextTuesday, TramTime.of(9,0), 7);
            }
        }
    }

    @Test
    public void shouldCheckCornbrookToStPetersSquareOnSundayMorning() {
        TramTime time = TramTime.of(11, 0);
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.StPetersSquare, time, nextTuesday.plusDays(5));
    }

    public void shouldFindInterchangesToInterchanges() {
        for (Location start :  Stations.Interchanges) {
            for (Location dest : Stations.Interchanges) {
                checkRouteNextNDays(start, dest, nextTuesday, TramTime.of(9,0), 7);
            }
        }
    }

    @Test
    public void shouldNotGenerateDuplicateJourneys() {
        assumeTrue(edgePerTrip);

        Set<List<RawStage>> stages = new HashSet<>();

        List<TramTime> queryTimes = new LinkedList<>();
        queryTimes.add(TramTime.of(11,45));
        Set<RawJourney> journeys = calculator.calculateRoute(Stations.Bury.getId(), Stations.Altrincham.getId(), queryTimes,
                new TramServiceDate(nextTuesday), 100);
        assertTrue(journeys.size()>0);

        journeys.forEach(journey -> {
            assertFalse(stages.toString(), stages.contains(journey.getStages()));
            stages.add(journey.getStages());
        });

    }

    @Test
    public void shouldFindEndOfLinesToInterchanges() {
        for (Location start : Stations.EndOfTheLine) {
            for (Location dest : Stations.Interchanges) {
                checkRouteNextNDays(start, dest, nextTuesday, TramTime.of(9,0), 7);
            }
        }
    }

    @Test
    public void shouldFindInterchangesToEndOfLines() {
        for (Location start : Stations.Interchanges ) {
            for (Location dest : Stations.EndOfTheLine) {
                checkRouteNextNDays(start,dest, nextTuesday, TramTime.of(8,0), 7);
            }
        }
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
    
    private int checkRangeOfTimes(Location start, Location dest) {
        List<TramTime> missing = new LinkedList<>();
        for (int hour = 6; hour < 23; hour++) {
            for (int minutes = 0; minutes < 59; minutes=minutes+5) {
                TramTime time = TramTime.of(hour, minutes);
                Set<RawJourney> journeys = calculator.calculateRoute(start.getId(), dest.getId(),
                        Collections.singletonList(time), new TramServiceDate(nextTuesday), RouteCalculator.MAX_NUM_GRAPH_PATHS);
                if (journeys.size()==0) {
                    missing.add(time);
                }
            }

        }
        return missing.size();
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
                validateAtLeastOneJourney(start, dest, time, date.plusDays(day));
            }
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
                new TramServiceDate(date), RouteCalculator.MAX_NUM_GRAPH_PATHS);

        String message = String.format("from %s to %s at %s on %s", startId, destId, time, queryDate);
        assertTrue("Unable to find journey " + message, journeys.size() > 0);
        journeys.forEach(journey -> assertFalse(message + " missing stages for journey" + journey, journey.getStages().isEmpty()));
        if (edgePerTrip) {
            journeys.forEach(RouteCalculatorTest::checkStages);
        }
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

}