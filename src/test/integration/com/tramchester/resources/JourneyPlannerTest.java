package com.tramchester.resources;


import com.tramchester.Dependencies;
import com.tramchester.IntegrationTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.*;
import com.tramchester.representations.JourneyPlanRepresentation;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
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
    public void testAltyToManAirportHasRealisticTranferAtCornbrook() throws Exception {
        JourneyPlanRepresentation results = planner.createJourneyPlan(Stations.Altrincham, Stations.ManAirport, "11:43:00", DaysOfWeek.Sunday);
        Set<Journey> journeys = results.getJourneys();

        assertEquals(1, journeys.size());
        checkDepartsAfterPreviousArrival(journeys);
    }

    // related helpful cypher query:
    // match (start { id: "9400ZZMAVPKMET:MET4:O:"} )-[route]->dest return route
    @Test
    public void testVeloParkToPeelHallPeformanceIssue() {
        JourneyPlanRepresentation results = planner.createJourneyPlan(Stations.VeloPark, Stations.PeelHall, "12:00:00", DaysOfWeek.Monday);
        Set<Journey> journeys = results.getJourneys();

        assertEquals(2, journeys.size());
        checkDepartsAfterPreviousArrival(journeys);
    }

    @Test
    public void testEachStationToEveryOther() {
        TransportData data = dependencies.get(TransportData.class);
        List<Station> allStations = data.getStations();
        for(Station start : allStations) {
            for(Station end: allStations) {
                String startCode = start.getId();
                String endCode = end.getId();
                if (!startCode.equals(endCode)) {
                    JourneyPlanRepresentation results = planner.createJourneyPlan(startCode, endCode, "12:00:00", DaysOfWeek.Monday);
                    assertTrue(results.getJourneys().size() > 0);
                }
            }
        }
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
