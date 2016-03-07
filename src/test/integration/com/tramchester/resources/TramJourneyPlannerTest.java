package com.tramchester.resources;


import com.tramchester.Dependencies;
import com.tramchester.IntegrationTramTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.JourneyPlanRepresentation;
import com.tramchester.repository.TransportData;
import org.joda.time.LocalDate;
import org.junit.*;
import org.junit.rules.Timeout;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TramJourneyPlannerTest extends  JourneyPlannerHelper {
    private static Dependencies dependencies;
    private TramServiceDate today;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10*60);

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @Before
    public void beforeEachTestRuns() {
        today = new TramServiceDate(LocalDate.now());
        planner = dependencies.get(JourneyPlannerResource.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void shouldFindEndOfLinesToEndOfLinesToday() throws TramchesterException {
        for (String start : Stations.EndOfTheLine) {
            for (String dest : Stations.EndOfTheLine) {
                checkRouteForAllDays(start, dest, 10*60, today);
            }
        }
    }

    @Test
    public void shouldFindEndOfLinesToEndOfLinesFuture() throws TramchesterException {
        for (String start : Stations.EndOfTheLine) {
            for (String dest : Stations.EndOfTheLine) {
                TramServiceDate twoWeeksHence = new TramServiceDate(LocalDate.now().plusWeeks(2));
                checkRouteForAllDays(start, dest, 10*60, twoWeeksHence);
            }
        }
    }

    @Test
    public void shouldFindInterchangesToInterchanges() throws TramchesterException {
        for (String start : Interchanges.stations()) {
            for (String dest : Interchanges.stations()) {
                checkRouteForAllDays(start, dest, 9*60, today);
            }
        }
    }

    @Test
    public void shouldFindEndOfLinesToInterchanges() throws TramchesterException {
        for (String start : Stations.EndOfTheLine) {
            for (String dest : Interchanges.stations()) {
                checkRouteForAllDays(start, dest, 9*60, today);
            }
        }
    }

    @Test
    public void shouldFindInterchangesToEndOfLines() throws TramchesterException {
        for (String start : Interchanges.stations() ) {
            for (String dest : Stations.EndOfTheLine) {
                checkRouteForAllDays(start,dest, 10*60, today);
            }
        }
    }

    @Test
    public void shouldReproduceIssueWithMissingRoutes() throws TramchesterException {
        validateAtLeastOneJourney(Stations.TraffordBar.getName(), Stations.ExchangeSquare.getId(), 10*60, DaysOfWeek.Saturday, today);
    }

    @Test
    public void testAltyToManAirportHasRealisticTranferAtCornbrook() throws TramchesterException {
        JourneyPlanRepresentation results = planner.createJourneyPlan(Stations.Altrincham.getId(),
                Stations.ManAirport.getId(), DaysOfWeek.Sunday, today, (11*60)+43);
        Set<Journey> journeys = results.getJourneys();

        assertEquals(1, journeys.size());
        checkDepartsAfterPreviousArrival("Altrincham to airport at 11:43 sunday", journeys);
    }

    @Test
    public void shouldFindRouteVeloToEtihad() throws TramchesterException {
        validateAtLeastOneJourney(Stations.VeloPark.getId(), Stations.Etihad, 8*60, DaysOfWeek.Monday, today);
    }

    @Test
    public void shouldFindRouteVicToShawAndCrompton() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Victoria.getId(), Stations.ShawAndCrompton, (23*60)+44, DaysOfWeek.Monday, today);
    }

    @Test
    public void shouldFindRoutePiccadilyGardensToCornbrook() throws TramchesterException {
        validateAtLeastOneJourney(Stations.PiccadilyGardens.getId(), Stations.Cornbrook.getId(), 23*60, DaysOfWeek.Monday, today);
    }

    @Test
    public void shouldFindRouteCornbrookToManAirport() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Cornbrook.getId(), Stations.ManAirport.getId(), (23*60)+20, DaysOfWeek.Monday, today);
    }

    @Test
    public void shouldFindRouteDeansgateToVictoria() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Deansgate.getId(), Stations.Victoria.getId(), (23*60)+41, DaysOfWeek.Monday, today);
    }

    @Test
    public void shouldFindRouteVeloToHoltTownAt8AM() throws TramchesterException {
        validateAtLeastOneJourney(Stations.VeloPark.getId(), Stations.HoltTown, 8*60, DaysOfWeek.Monday, today);
    }

    @Test
    public void shouldFindRouteVeloToHoltTownAt8RangeOfTimes() throws TramchesterException {
        for(int i=0; i<60; i++) {
            int time = (8*60)+i;
            validateAtLeastOneJourney(Stations.VeloPark.getId(), Stations.HoltTown, time, DaysOfWeek.Monday, today);
        }
    }

    @Test
    public void shouldFindRouteVeloToEndOfLines() throws TramchesterException {
        for (String dest : Stations.EndOfTheLine) {
            validateAtLeastOneJourney(Stations.VeloPark.getId(), dest, 8*60, DaysOfWeek.Monday, today);
        }
    }

    @Test
    public void shouldFindRouteVeloInterchanges() throws TramchesterException {
        for (String dest : Interchanges.stations()) {
            validateAtLeastOneJourney(Stations.VeloPark.getId(), dest, 8*60, DaysOfWeek.Monday, today);
        }
    }

    @Test
    public void shouldFindRouteVeloToDeansgate() throws TramchesterException {
        validateAtLeastOneJourney(Stations.VeloPark.getId(), Stations.Deansgate.getId(), 8*60, DaysOfWeek.Monday, today);
    }

    @Test
    public void shouldFindRouteVeloToBroadway() throws TramchesterException {
        validateAtLeastOneJourney(Stations.VeloPark.getId(), Stations.Broadway, 8*60, DaysOfWeek.Monday, today);
    }

    @Test
    public void shouldFindRouteVeloToPomona() throws TramchesterException {
        validateAtLeastOneJourney(Stations.VeloPark.getId(), Stations.Pomona, 8*60, DaysOfWeek.Monday, today);
    }

    @Test
    public void shouldFindEndOfDayTwoStageJourney() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Altrincham.getId(), Stations.Victoria.getId(), 23*60, DaysOfWeek.Wednesday,
                today);
    }

    @Test
    public void shouldFindEndOfDayThreeStageJourney() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Altrincham.getId(), Stations.ShawAndCrompton, (22*60)+45, DaysOfWeek.Wednesday,
                today);
    }



    @Test
    public void shouldOnlyReturnFullJourneysForEndOfDaysJourney() throws TramchesterException {
        JourneyPlanRepresentation results = validateAtLeastOneJourney(Stations.PiccadilyGardens.getId(),
                Stations.ManAirport.getId(), (23*60)+10, DaysOfWeek.Monday, today);
        Journey journey = results.getJourneys().stream().findFirst().get();

        assertEquals("number of times for stage one", 3, journey.getStages().get(0).getNumberOfServiceTimes());
        assertEquals("number of times for stage two", 1, journey.getStages().get(1).getNumberOfServiceTimes());
        assertEquals("available times", 1, journey.getNumberOfTimes());
    }

    @Test
    public void shouldInvokeQuickestRouteDirectly() throws TramchesterException {
        Response result = planner.quickestRoute(Stations.Altrincham.getId(), Stations.Piccadily.getId(), "23:00:00");
        assertEquals(200, result.getStatus());
    }

    @Test
    public void shouldFindRouteEachStationToEveryOther() throws TramchesterException {
        TransportData data = dependencies.get(TransportData.class);
        List<Station> allStations = data.getStations();
        for(Location start : allStations) {
            for(Location end: allStations) {
                String startCode = start.getId();
                String endCode = end.getId();
                if (!startCode.equals(endCode)) {
                    JourneyPlanRepresentation results = planner.createJourneyPlan(startCode, endCode,
                            DaysOfWeek.Monday, today,12*60);
                    assertTrue(results.getJourneys().size() > 0);
                }
            }
        }
    }

    private void checkRouteForAllDays(String start, String dest, int time, TramServiceDate queryDate) throws TramchesterException {
        if (!dest.equals(start)) {
            for(DaysOfWeek day : DaysOfWeek.values()) {
                validateAtLeastOneJourney(start, dest, time, day, queryDate);
            }
        }
    }
}
