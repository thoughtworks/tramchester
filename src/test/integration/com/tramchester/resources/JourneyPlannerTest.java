package com.tramchester.resources;


import com.tramchester.Dependencies;
import com.tramchester.IntegrationTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.Journey;
import com.tramchester.domain.Stage;
import com.tramchester.representations.JourneyPlanRepresentation;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class JourneyPlannerTest {
    private JourneyPlannerResource planner;
    private static Dependencies dependencies;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTestConfig());
    }

    @Before
    public void beforeEachTestRuns() {
        planner = dependencies.get(JourneyPlannerResource.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void testAltyToManAirport() throws Exception {
        JourneyPlanRepresentation results = planner.createJourneyPlan(Stations.Altrincham, Stations.ManAirport, "11:43:00", DaysOfWeek.Sunday);
        Set<Journey> journeys = results.getJourneys();

        assertEquals(1, journeys.size());
        checkDepartsAfterPreviousArrival(journeys);
    }

    private void checkDepartsAfterPreviousArrival(Set<Journey> journeys) {
        for(Journey journey: journeys) {
            DateTime previousArrive = null;
            for(Stage stage : journey.getStages()) {
                if (previousArrive!=null) {
                    String message = String.format("Check arrive at %s and leave at %s", previousArrive, stage.getFirstDepartureTime());
                    assertTrue(message, stage.getFirstDepartureTime().isAfter(previousArrive));
                }
                previousArrive = stage.getExpectedArrivalTime();
            }
        }
    }


}
