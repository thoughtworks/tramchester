package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.TestConfig;
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
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class RouteCalculatorTest {

    private static Dependencies dependencies;

    private RouteCalculator calculator;
    private LocalDate nextTuesday = TestConfig.nextTuesday(0);

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
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
        List<LocalTime> minutes = Collections.singletonList(LocalTime.of(8, 0));
        Set<RawJourney> results = calculator.calculateRoute(Stations.Altrincham.getId(), Stations.Cornbrook.getId(),
                minutes, new TramServiceDate(nextTuesday));
        assertTrue(results.size()>0);
    }

    @Test
    public void shouldHaveSimpleOneStopJourney() {
        checkRouteNextNDays(Stations.Deansgate, Stations.Cornbrook, nextTuesday, LocalTime.of(9,0), 1);
    }

    @Test
    public void shouldHaveSimpleManyStopSameLineJourney() {
        checkRouteNextNDays(Stations.Altrincham, Stations.Cornbrook, nextTuesday, LocalTime.of(9,0), 1);
    }

    @Test
    public void shouldHaveSimpleManyStopJourneyViaInterchange() {
        checkRouteNextNDays(Stations.Altrincham, Stations.Bury, nextTuesday, LocalTime.of(9,0), 1);
    }

    // over max wait, catch failure to accumulate journey times correctly
    @Test
    public void shouldHaveSimpleButLongJoruneySameRoute() {
        checkRouteNextNDays(Stations.ManAirport, Stations.TraffordBar, nextTuesday, LocalTime.of(9,0), 1);
    }

    @Test
    public void shouldHaveSimpleManyStopJourneyStartAtInterchange() {
        checkRouteNextNDays(Stations.Cornbrook, Stations.Bury, nextTuesday, LocalTime.of(9,0), 1);
    }

    @Test
    public void testJourneyFromAltyToAirport() {
        List<LocalTime> queryTimes = Collections.singletonList(LocalTime.of(11, 43));
        TramServiceDate today = new TramServiceDate(LocalDate.now());

        Set<RawJourney> results = calculator.calculateRoute(Stations.Altrincham.getId(), Stations.ManAirport.getId(),
                queryTimes, today);

        assertTrue(results.size()>0);    // results is iterator
        for (RawJourney result : results) {
            List<RawStage> stages = result.getStages();
            assertTrue(stages.size()>=2);
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

        List<LocalTime> queryTimes = Collections.singletonList(LocalTime.of(23, 15));
        Set<RawJourney> results = calculator.calculateRoute(Stations.Cornbrook.getId(), Stations.ManAirport.getId(),
                queryTimes, new TramServiceDate(nextTuesday));

        assertTrue(results.size()>0);
    }

    @Test
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

        List<LocalTime> queryTimes = Collections.singletonList(LocalTime.of(12, 0));

        Map<Pair<Location, Location>, Set<RawJourney>> allJourneys = combinations.parallelStream().
                map(stations -> Pair.of(stations, calc(stations, queryTimes, queryDate))).
                        collect(Collectors.toMap(pair -> pair.getLeft(), pair -> pair.getRight()));

        long failed = allJourneys.values().stream().filter(rawJourneys -> rawJourneys.size() == 0).count();
        assertEquals(0L, failed);

        Set<Integer> passedStops = allJourneys.values().stream().
                flatMap(set -> set.stream()).
                map(journey ->
                        journey.getStages().stream().
                                map(stage -> stage.getPassedStops()).
                                reduce((a, b) -> a + b)).
                filter(sum -> sum.isPresent()).
                map(sum -> sum.get())
                .collect(Collectors.toSet());

        Integer[] results = new Integer[passedStops.size()];
        passedStops.toArray(results);
        int longest = results[results.length - 1];
        assertEquals(40,longest);

    }

    private Set<RawJourney> calc(Pair<Location, Location> pair, List<LocalTime> queryTimes, TramServiceDate queryDate) {
        return calculator.calculateRoute(pair.getLeft().getId(), pair.getRight().getId(), queryTimes, queryDate);
    }

    @Test
    public void shouldFindEndOfLinesToEndOfLines() {
        for (Location start : Stations.EndOfTheLine) {
            for (Location dest : Stations.EndOfTheLine) {
                checkRouteNextNDays(start, dest, nextTuesday, LocalTime.of(9,0), 7);
            }
        }
    }

    @Test
    public void shouldFindInterchangesToInterchanges() {
        for (Location start :  Stations.Interchanges) {
            for (Location dest : Stations.Interchanges) {
                checkRouteNextNDays(start, dest, nextTuesday, LocalTime.of(9,0), 7);
            }
        }
    }

    @Test
    public void shouldFindEndOfLinesToInterchanges() {
        for (Location start : Stations.EndOfTheLine) {
            for (Location dest : Stations.Interchanges) {
                checkRouteNextNDays(start, dest, nextTuesday, LocalTime.of(9,0), 7);
            }
        }
    }

    @Test
    public void shouldFindInterchangesToEndOfLines() {
        for (Location start : Stations.Interchanges ) {
            for (Location dest : Stations.EndOfTheLine) {
                checkRouteNextNDays(start,dest, nextTuesday, LocalTime.of(8,0), 7);
            }
        }
    }

    @Test
    public void shouldHaveInAndAroundCornbrookToEccles8amTuesday() {
        LocalDate nextTuesday = TestConfig.nextTuesday(0);
        // catches issue with services, only some of which go to media city, while others direct to broadway
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.Broadway, LocalTime.of(8,00), nextTuesday);
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.Eccles, LocalTime.of(8,00), nextTuesday);

        validateAtLeastOneJourney(Stations.Cornbrook, Stations.Broadway, LocalTime.of(9,00), nextTuesday);
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.Eccles, LocalTime.of(9,00), nextTuesday);
    }

    @Test
    public void shouldReproIssueWithJourneysToEccles() {
        validateAtLeastOneJourney(Stations.Bury, Stations.Broadway, LocalTime.of(9,00), nextTuesday);
        validateAtLeastOneJourney(Stations.Bury, Stations.Eccles, LocalTime.of(9,00), nextTuesday);
    }

    @Test
    public void reproduceIssueEdgePerTrip() {
        validateAtLeastOneJourney(Stations.StPetersSquare, Stations.Pomona, LocalTime.of(19,51), nextTuesday);
    }

    @Test
    public void shouldReproIssueWithStPetersToBeyondEcclesAt8AM() {
        assertEquals(0,checkRangeOfTimes(Stations.Cornbrook, Stations.Eccles));
    }

    private int checkRangeOfTimes(Location start, Location dest) {
        List<LocalTime> missing = new LinkedList<>();
        for (int hour = 6; hour < 23; hour++) {
            for (int minutes = 0; minutes < 59; minutes++) {
                LocalTime time = LocalTime.of(hour, minutes);
                Set<RawJourney> journeys = calculator.calculateRoute(start.getId(), dest.getId(),
                        Collections.singletonList(time), new TramServiceDate(nextTuesday));
                if (journeys.size()==0) {
                    missing.add(time);
                }
            }

        }
        return missing.size();
    }

    @Test
    public void reproduceIssueWithImmediateDepartOffABoardedTram() {
        checkRouteNextNDays(Stations.Deansgate, Stations.Ashton, nextTuesday, LocalTime.of(8,0), 7);
    }

    @Test
    public void reproduceIssueWithTramsSunday() {
        checkRouteNextNDays(Stations.StPetersSquare, Stations.Deansgate, TestConfig.nextSunday(), LocalTime.of(9,0), 1);
    }

    @Test
    public void shouldReproduceIssueCornbrookToAshtonSatursdays() {
        LocalDate date = TestConfig.nextSaturday();
        checkRouteNextNDays(Stations.Cornbrook, Stations.Ashton, date, LocalTime.of(9,0), 7);
    }

    @Test
    public void shouldFindRouteVeloToHoltTownAt8RangeOfTimes() {
        for(int i=0; i<60; i++) {
            LocalTime time = LocalTime.of(8,i);
            validateAtLeastOneJourney(Stations.VeloPark, Stations.HoltTown, time, nextTuesday);
        }
    }

    protected void checkRouteNextNDays(Location start, Location dest, LocalDate date, LocalTime time, int numDays) {
        if (!dest.equals(start)) {
            for(int day = 0; day< numDays; day++) {
                validateAtLeastOneJourney(start, dest, time, date.plusDays(day));
            }
        }
    }

    private void validateAtLeastOneJourney(Location start, Location dest, LocalTime minsPastMid, LocalDate date) {
        TramServiceDate queryDate = new TramServiceDate(date);
        Set<RawJourney> journeys = calculator.calculateRoute(start.getId(), dest.getId(), Collections.singletonList(minsPastMid),
                new TramServiceDate(date));

        String message = String.format("from %s to %s at %s on %s", start, dest, minsPastMid, queryDate);
        assertTrue("Unable to find journey " + message, journeys.size() > 0);
        journeys.forEach(journey -> assertFalse("Missing stages for journey"+journey,journey.getStages().isEmpty()));
    }


}