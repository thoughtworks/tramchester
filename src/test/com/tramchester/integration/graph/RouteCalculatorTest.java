package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.TestConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import com.tramchester.integration.resources.JourneyPlannerHelper;
import com.tramchester.repository.TransportData;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.Assert.*;

public class RouteCalculatorTest {

    private static Dependencies dependencies;

    private RouteCalculator calculator;
    private LocalDate when = TestConfig.nextTuesday(0);

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
                minutes, new TramServiceDate(when));
        assertTrue(results.size()>0);
    }

    @Test
    public void shouldHaveSimpleOneStopJourney() {
        checkRouteNextNDays(Stations.Deansgate, Stations.Cornbrook, when, LocalTime.of(9,0), 1);
    }

    @Test
    public void shouldHaveSimpleManyStopSameLineJourney() {
        checkRouteNextNDays(Stations.Altrincham, Stations.Cornbrook, when, LocalTime.of(9,0), 1);
    }

    @Test
    public void shouldHaveSimpleManyStopJourney() {
        checkRouteNextNDays(Stations.Altrincham, Stations.Bury, when, LocalTime.of(9,0), 1);
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
                queryTimes, new TramServiceDate(when));

        assertTrue(results.size()>0);
    }

    @Test
    public void shouldFindRouteEachStationToEveryOtherStream() {
        TramServiceDate queryDate = new TramServiceDate(when);
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

        Boolean result = combinations.parallelStream().
                map(pair -> calc(pair, Collections.singletonList(LocalTime.of(12, 0)), queryDate)).
                map(journeys -> journeys.size() > 0).
                reduce(true, (a, b) -> a && b);
        assertTrue(result);

    }

    private Set<RawJourney> calc(Pair<Location, Location> pair, List<LocalTime> queryTimes, TramServiceDate queryDate) {
        return calculator.calculateRoute(pair.getLeft().getId(), pair.getRight().getId(), queryTimes, queryDate);
    }

    @Test
    public void shouldFindEndOfLinesToEndOfLines() {
        for (Location start : Stations.EndOfTheLine) {
            for (Location dest : Stations.EndOfTheLine) {
                checkRouteNextNDays(start, dest, when, LocalTime.of(9,0), 7);
            }
        }
    }

    @Test
    public void shouldFindInterchangesToInterchanges() {
        for (Location start :  Stations.Interchanges) {
            for (Location dest : Stations.Interchanges) {
                checkRouteNextNDays(start, dest, when, LocalTime.of(9,0), 7);
            }
        }
    }

    @Test
    public void shouldFindEndOfLinesToInterchanges() {
        for (Location start : Stations.EndOfTheLine) {
            for (Location dest : Stations.Interchanges) {
                checkRouteNextNDays(start, dest, when, LocalTime.of(9,0), 7);
            }
        }
    }

    @Test
    public void shouldFindInterchangesToEndOfLines() {
        for (Location start : Stations.Interchanges ) {
            for (Location dest : Stations.EndOfTheLine) {
                checkRouteNextNDays(start,dest, when, LocalTime.of(8,0), 7);
            }
        }
    }

    @Test
    public void reproduceIssueWithImmediateDepartOffABoardedTram() {
        checkRouteNextNDays(Stations.Deansgate, Stations.Ashton, when, LocalTime.of(8,0), 7);
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
            validateAtLeastOneJourney(Stations.VeloPark, Stations.HoltTown, time, when);
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