package com.tramchester.resources;


import com.tramchester.Dependencies;
import com.tramchester.IntegrationTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.*;
import com.tramchester.graph.TransportGraphBuilder;
import com.tramchester.graph.UnknownStationException;
import com.tramchester.representations.JourneyPlanRepresentation;
import org.joda.time.DateTime;
import org.junit.*;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore("Ignored because of St Peter Square closure")
public class JourneyPlannerTest extends  JourneyPlannerHelper {
    private static Dependencies dependencies;
    private TramServiceDate today;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTestConfig());
    }

    @Before
    public void beforeEachTestRuns() {
        today = new TramServiceDate(DateTime.now());
        planner = dependencies.get(JourneyPlannerResource.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void shouldFindEndOfLinesToEndOfLines() throws UnknownStationException {
        for (String start : Stations.EndOfTheLine) {
            for (String dest : Stations.EndOfTheLine) {
                if (!dest.equals(start)) {
                    validateAtLeastOneJourney(start, dest, "08:00:00", DaysOfWeek.Monday, today);
                }
            }
        }
    }

    @Test
    public void shouldFindInterchangesToInterchanges() throws UnknownStationException {
        for (String start : TransportGraphBuilder.interchanges) {
            for (String dest : TransportGraphBuilder.interchanges) {
                if (!dest.equals(start)) {
                    validateAtLeastOneJourney(start, dest, "08:00:00", DaysOfWeek.Monday, today);
                }
            }
        }
    }

    @Test
    public void shouldFindEndOfLinesToInterchanges() throws UnknownStationException {
        for (String start : Stations.EndOfTheLine) {
            for (String dest : TransportGraphBuilder.interchanges) {
                if (!dest.equals(start)) {
                    validateAtLeastOneJourney(start, dest, "08:00:00", DaysOfWeek.Monday, today);
                }
            }
        }
    }

    @Test
    public void shouldFindInterchangesToEndOfLines() throws UnknownStationException {
        for (String start : TransportGraphBuilder.interchanges ) {
            for (String dest : Stations.EndOfTheLine) {
                if (!dest.equals(start)) {
                    validateAtLeastOneJourney(start, dest, "08:00:00", DaysOfWeek.Monday, today);
                }
            }
        }
    }

    @Test
    public void testAltyToManAirportHasRealisticTranferAtCornbrook() throws Exception {
        JourneyPlanRepresentation results = planner.createJourneyPlan(Stations.Altrincham, Stations.ManAirport, "11:43:00", DaysOfWeek.Sunday, today);
        Set<Journey> journeys = results.getJourneys();

        assertEquals(1, journeys.size());
        checkDepartsAfterPreviousArrival("Altrincham to airport at 11:43 sunday", journeys);
    }

    @Test
    public void shouldFindRouteVeloToEtihad() throws UnknownStationException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.Etihad, "08:00:00", DaysOfWeek.Monday, today);
    }

    @Test
    public void shouldFindRouteVeloToHoltTownAt8AM() throws UnknownStationException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.HoltTown, "08:00:00", DaysOfWeek.Monday, today);
    }

    @Test
    public void shouldFindRouteVeloToHoltTownAt8RangeOfTimes() throws UnknownStationException {
        for(int i=0; i<60; i++) {
            String time = String.format("08:%02d:00", i);
            validateAtLeastOneJourney(Stations.VeloPark, Stations.HoltTown, time, DaysOfWeek.Monday, today);
        }
    }

    @Test
    public void shouldFindRouteVeloToEndOfLines() throws UnknownStationException {
        for (String dest : Stations.EndOfTheLine) {
            validateAtLeastOneJourney(Stations.VeloPark, dest, "08:00:00", DaysOfWeek.Monday, today);
        }
    }

    @Test
    public void shouldFindRouteVeloInterchanges() throws UnknownStationException {
        for (String dest : TransportGraphBuilder.interchanges) {
            validateAtLeastOneJourney(Stations.VeloPark, dest, "08:00:00", DaysOfWeek.Monday, today);
        }
    }

    @Test
    public void shouldFindRouteVeloToDeansgate() throws UnknownStationException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.Deansgate, "08:00:00", DaysOfWeek.Monday, today);
    }

    @Test
    public void shouldFindRouteVeloToBroadway() throws UnknownStationException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.Broadway, "08:00:00", DaysOfWeek.Monday, today);
    }

    @Test
    public void shouldFindRouteVeloToPomona() throws UnknownStationException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.Pomona, "08:00:00", DaysOfWeek.Monday, today);
    }

    @Test
    public void shouldFindRouteEachStationToEveryOther() throws UnknownStationException {
        TransportData data = dependencies.get(TransportData.class);
        List<Station> allStations = data.getStations();
        for(Station start : allStations) {
            for(Station end: allStations) {
                String startCode = start.getId();
                String endCode = end.getId();
                if (!startCode.equals(endCode)) {
                    JourneyPlanRepresentation results = planner.createJourneyPlan(startCode, endCode, "12:00:00", DaysOfWeek.Monday, today);
                    assertTrue(results.getJourneys().size() > 0);
                }
            }
        }
    }


}
