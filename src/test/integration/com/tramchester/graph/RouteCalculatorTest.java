package com.tramchester.graph;

import com.tramchester.Dependencies;
import com.tramchester.IntegrationTramTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.repository.TransportData;
import com.tramchester.resources.JourneyPlannerHelper;
import com.tramchester.services.DateTimeService;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.LocalDate;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RouteCalculatorTest {

    private static Dependencies dependencies;

    private RouteCalculator calculator;
    private DateTimeService dateTimeService;
    private LocalDate when = JourneyPlannerHelper.nextMonday();

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @Before
    public void beforeEachTestRuns() {
        calculator = dependencies.get(RouteCalculator.class);
        dateTimeService = dependencies.get(DateTimeService.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void shouldHaveSimpleJourney() throws TramchesterException {
        List<Integer> minutes = Arrays.asList(new Integer[]{8*60});
        Set<RawJourney> results = calculator.calculateRoute(Stations.Altrincham.getId(), Stations.Cornbrook.getId(),
                minutes, new TramServiceDate(when));
        assertTrue(results.size()>0);
    }

    @Test
    public void testJourneyFromAltyToAirport() throws TramchesterException {
        int minutesFromMidnight = dateTimeService.getMinutesFromMidnight("11:43:00");
        List<Integer> minutes = Arrays.asList(new Integer[]{minutesFromMidnight});
        TramServiceDate today = new TramServiceDate(LocalDate.now());

        Set<RawJourney> results = calculator.calculateRoute(Stations.Altrincham.getId(), Stations.ManAirport.getId(),
                minutes, today);

        assertTrue(results.size()>0);    // results is iterator
        for (RawJourney result : results) {
            List<RawStage> stages = result.getStages();
            assertEquals(2, stages.size());
            RawVehicleStage firstStage = (RawVehicleStage) stages.get(0);
            assertEquals(Stations.Altrincham, firstStage.getFirstStation());
            assertEquals(Stations.TraffordBar, firstStage.getLastStation());
            assertEquals(TransportMode.Tram, firstStage.getMode());
            RawVehicleStage secondStage = (RawVehicleStage) stages.get(1);
            assertEquals(Stations.TraffordBar, secondStage.getFirstStation());
            assertEquals(Stations.ManAirport, secondStage.getLastStation());
            assertEquals(TransportMode.Tram, secondStage.getMode());
        }
    }

    @Test
    public void shouldFindRouteEachStationToEveryOtherStream() throws TramchesterException {
        TramServiceDate queryDate = new TramServiceDate(when);
        TransportData data = dependencies.get(TransportData.class);
        int time = 12 * 60;

        List<Integer> queryTimes = formQueryTimes(time);

        List<Station> allStations = data.getStations();
        List<Pair<Location,Location>> combinations = new LinkedList<>();

        for (Location start : allStations) {
            for (Location end : allStations) {
                if (!start.equals(end)) {
                    combinations.add(Pair.of(start,end));
                }
            }
        }

        Boolean result = combinations.parallelStream().
                map(pair -> calc(pair, queryTimes, queryDate)).
                map(journeys -> journeys.size() > 0).
                reduce(true, (a, b) -> a && b);
        assertTrue(result);

    }

    private Set<RawJourney> calc(Pair<Location, Location> pair, List<Integer> queryTimes, TramServiceDate queryDate) {
        try {
            return calculator.calculateRoute(pair.getLeft().getId(), pair.getRight().getId(), queryTimes, queryDate);
        } catch (TramchesterException e) {
            return new HashSet<>();
        }
    }

    @Test
    public void shouldFindEndOfLinesToEndOfLines() throws TramchesterException {
        for (Location start : Stations.EndOfTheLine) {
            for (Location dest : Stations.EndOfTheLine) {
                checkRouteNext7Days(start, dest, when, 9*60);
            }
        }
    }

    @Test
    public void shouldFindInterchangesToInterchanges() throws TramchesterException {
        for (Location start :  Stations.getInterchanges()) {
            for (Location dest : Stations.getInterchanges()) {
                checkRouteNext7Days(start, dest, when, 9*60);
            }
        }
    }

    @Test
    public void shouldFindEndOfLinesToInterchanges() throws TramchesterException {
        for (Location start : Stations.EndOfTheLine) {
            for (Location dest : Stations.getInterchanges()) {
                checkRouteNext7Days(start, dest, when, 9*60);
            }
        }
    }

    @Test
    public void shouldFindInterchangesToEndOfLines() throws TramchesterException {
        for (Location start : Stations.getInterchanges() ) {
            for (Location dest : Stations.EndOfTheLine) {
                checkRouteNext7Days(start,dest, when, 10*60);
            }
        }
    }

    @Test
    public void shouldFindRouteVeloToHoltTownAt8RangeOfTimes() throws TramchesterException {
        for(int i=0; i<60; i++) {
            int time = (8*60)+i;
            validateAtLeastOneJourney(Stations.VeloPark, Stations.HoltTown, time, when);
        }
    }

    protected void checkRouteNext7Days(Location start, Location dest, LocalDate date, int time) throws TramchesterException {
        if (!dest.equals(start)) {
            for(int day=0; day<7; day++) {
                validateAtLeastOneJourney(start, dest, time, date.plusDays(day));
            }
        }
    }

    private void validateAtLeastOneJourney(Location start, Location dest, int minsPastMid, LocalDate date) throws TramchesterException {
        TramServiceDate queryDate = new TramServiceDate(date);
        Set<RawJourney> journeys = calculator.calculateRoute(start.getId(), dest.getId(), formQueryTimes(minsPastMid),
                new TramServiceDate(date));

        String message = String.format("from %s to %s at %s on %s", start, dest, minsPastMid, queryDate);
        assertTrue("Unable to find journey " + message, journeys.size() > 0);
        journeys.forEach(journey -> assertFalse("Missing stages for journey"+journey,journey.getStages().isEmpty()));
    }

    private List<Integer> formQueryTimes(int time) {
        List<Integer> queryTimes = new ArrayList<>();
        queryTimes.add(time);
        return queryTimes;
    }

}