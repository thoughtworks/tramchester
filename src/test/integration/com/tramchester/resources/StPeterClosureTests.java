package com.tramchester.resources;

import com.tramchester.Dependencies;
import com.tramchester.IntegrationTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.Journey;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.graph.UnknownStationException;
import com.tramchester.representations.JourneyPlanRepresentation;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;

public class StPeterClosureTests extends JourneyPlannerHelper {
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
        for (int i = 0; i < 60; i++) {
            String time = String.format("08:%02d:00", i);
            validateAtLeastOneJourney(Stations.VeloPark, Stations.HoltTown, time, DaysOfWeek.Monday, today);
        }
    }


}
