package com.tramchester.resources;


import com.tramchester.Dependencies;
import com.tramchester.Stations;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.Stage;
import com.tramchester.representations.JourneyPlanRepresentation;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class JourneyPlannerTest {
    private static JourneyPlannerResource planner;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        Dependencies dependencies = new Dependencies();
        dependencies.initialise(new TestConfig());

        planner = dependencies.get(JourneyPlannerResource.class);
    }

    @Test
    public void testAltyToManAirport() throws Exception {
        JourneyPlanRepresentation results = planner.createJourneyPlan(Stations.Altrincham, Stations.ManAirport, "11:43:00");
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

    private static class TestConfig extends TramchesterConfig {

        @Override
        public boolean isRebuildGraph() {
            return false;
        }

        @Override
        public boolean isPullData() {
            return false;
        }
    }
}
