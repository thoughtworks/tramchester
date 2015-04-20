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
import org.neo4j.unsafe.impl.batchimport.stats.Stat;

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
        validateJourney(Stations.VeloPark, Stations.PeelHall, "12:00:00", DaysOfWeek.Monday, 1);
    }

    @Test
    public void shouldFindRouteWhenTramsDontCallAtStopOftenSaturday() {
        validateJourney(Stations.VeloPark, Stations.MediaCityUK, "08:00:00", DaysOfWeek.Saturday, 1);
    }

    @Test
    public void shouldFindRouteVeloToEtihad() {
        validateJourney(Stations.VeloPark, Stations.Etihad, "08:00:00", DaysOfWeek.Monday, 1);
    }

    @Test
    public void shouldFindRouteVeloToHoltTownAt8AM() {
        validateJourney(Stations.VeloPark, "9400ZZMAHTN", "08:00:00", DaysOfWeek.Monday, 1);
    }

    @Test
    public void shouldFindRouteVeloToHoltTownAt8RangeOfTimes() {
        for(int i=0; i<60; i++) {
            String time = String.format("08:%02d:00", i);
            validateJourney(Stations.VeloPark, "9400ZZMAHTN", time, DaysOfWeek.Monday, 1);
        }
    }

    @Test
    public void shouldFindRouteVeloToPiccGardens() {
        validateJourney(Stations.VeloPark, "9400ZZMAPGD", "08:00:00", DaysOfWeek.Monday, 1);
    }

    @Test
    public void shouldFindRouteVeloToStPetersSquare() {
        validateJourney(Stations.VeloPark, "9400ZZMASTP", "08:00:00", DaysOfWeek.Monday, 1);
    }

    @Test
    public void shouldFindRouteVeloToCornbrook() {
        validateJourney(Stations.VeloPark, Stations.Cornbrook, "08:00:00", DaysOfWeek.Monday, 1);
    }

    @Test
    public void shouldFindRouteVeloToPomona() {
        validateJourney(Stations.VeloPark, "9400ZZMAPOM", "08:00:00", DaysOfWeek.Monday, 1);
    }

    @Test
    public void shouldFindRouteVeloToHarbourCity() {
        validateJourney(Stations.VeloPark, "9400ZZMAHCY", "08:00:00", DaysOfWeek.Monday, 1);
    }

    @Test
    public void shouldFindRouteHarbourCityToMediaCityAtInterchangeTime() {
        validateJourney("9400ZZMAHCY", Stations.MediaCityUK, "08:33:00", DaysOfWeek.Monday, 1);
    }

    @Test
    public void shouldFindRouteVeloToEccles() {
        validateJourney(Stations.VeloPark, "9400ZZMAECC" , "08:00:00", DaysOfWeek.Monday, 1);
    }

    @Test
    public void shouldFindRouteVeloToTraffordBarChangeIsRequired() {
        validateJourney(Stations.VeloPark, Stations.TraffordBar, "08:00:00", DaysOfWeek.Monday, 2);
    }

    @Test
    public void shouldFindRouteVeloToMediaCityChangeIsRequired() {
        validateJourney(Stations.VeloPark, Stations.MediaCityUK, "08:00:00", DaysOfWeek.Monday, 1);
    }

    @Test
    public void shouldFindRouteCornbrookToMediaCityAtInterchangeTimeForVelo() {
        validateJourney(Stations.Cornbrook, Stations.MediaCityUK , "08:20:00", DaysOfWeek.Monday, 1);
    }

    private void validateJourney(String start, String end, String time, DaysOfWeek dayOfWeek, int expected) {
        JourneyPlanRepresentation results = planner.createJourneyPlan(start, end, time, dayOfWeek);
        Set<Journey> journeys = results.getJourneys();

        assertEquals(expected, journeys.size());
        checkDepartsAfterPreviousArrival(journeys);
    }

//    @Test
//    public void testEachStationToEveryOther() {
//        TransportData data = dependencies.get(TransportData.class);
//        List<Station> allStations = data.getStations();
//        for(Station start : allStations) {
//            for(Station end: allStations) {
//                String startCode = start.getId();
//                String endCode = end.getId();
//                if (!startCode.equals(endCode)) {
//                    JourneyPlanRepresentation results = planner.createJourneyPlan(startCode, endCode, "12:00:00", DaysOfWeek.Monday);
//                    assertTrue(results.getJourneys().size() > 0);
//                }
//            }
//        }
//    }

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
