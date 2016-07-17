package com.tramchester.resources;


import com.tramchester.ClosureTest;
import com.tramchester.Dependencies;
import com.tramchester.IntegrationTramTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.JourneyPlanRepresentation;
import org.joda.time.LocalDate;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.Timeout;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Set;

import static org.joda.time.DateTimeConstants.MONDAY;
import static org.joda.time.DateTimeConstants.SUNDAY;
import static org.junit.Assert.assertEquals;

public class TramJourneyPlannerTest extends JourneyPlannerHelper {
    private static Dependencies dependencies;
    private LocalDate when;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(5*60);

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @Before
    public void beforeEachTestRuns() {
        LocalDate now = LocalDate.now();
        int offset = now.getDayOfWeek()-MONDAY;
        when =  now.plusDays(offset);
        planner = dependencies.get(JourneyPlannerResource.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    @Category(ClosureTest.class)
    @Ignore("Summer 2016 closure")
    public void shouldFindEndOfLinesToEndOfLines() throws TramchesterException {
        for (Location start : Stations.EndOfTheLine) {
            for (Location dest : Stations.EndOfTheLine) {
                checkRouteNext7Days(start, dest, when, 10*60);
            }
        }
    }

    @Test
    @Category(ClosureTest.class)
    @Ignore("Summer 2016 closure")
    public void shouldFindInterchangesToInterchanges() throws TramchesterException {
        for (Location start :  Stations.getInterchanges()) {
            for (Location dest : Stations.getInterchanges()) {
                checkRouteNext7Days(start, dest, when, 9*60);
            }
        }
    }

    @Test
    @Category(ClosureTest.class)
    @Ignore("Summer 2016 closure")
    public void shouldFindEndOfLinesToInterchanges() throws TramchesterException {
        for (Location start : Stations.EndOfTheLine) {
            for (Location dest : Stations.getInterchanges()) {
                checkRouteNext7Days(start, dest, when, 9*60);
            }
        }
    }

    @Test
    @Ignore("summer closures")
    @Category(ClosureTest.class)
    public void shouldFindInterchangesToEndOfLines() throws TramchesterException {
        for (Location start : Stations.getInterchanges() ) {
            for (Location dest : Stations.EndOfTheLine) {
                checkRouteNext7Days(start,dest, when, 10*60);
            }
        }
    }

    @Test
    @Ignore("Summary 2016 closures")
    public void shouldReproduceIssueWithMissingRoutes() throws TramchesterException {
        validateAtLeastOneJourney(Stations.TraffordBar, Stations.ExchangeSquare, 10*60, when);
    }

    @Test
    public void testAltyToManAirportHasRealisticTranferAtCornbrook() throws TramchesterException {
        int offsetToSunday = SUNDAY- when.getDayOfWeek();
        LocalDate nextSunday = when.plusDays(offsetToSunday);

        JourneyPlanRepresentation results = planner.createJourneyPlan(Stations.Altrincham.getId(),
                Stations.ManAirport.getId(), new TramServiceDate(nextSunday), (11*60)+43);
        Set<Journey> journeys = results.getJourneys();

        assertEquals(1, journeys.size());
        checkDepartsAfterPreviousArrival("Altrincham to airport at 11:43 sunday", journeys);
    }

    @Test
    public void shouldFindRouteVeloToEtihad() throws TramchesterException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.Etihad, 8*60, when);
    }

    @Test
    public void shouldFindRouteVicToShawAndCrompton() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Victoria, Stations.ShawAndCrompton, (23*60)+44, when);
    }

    @Test
    @Ignore("Summary 2016 closures")
    public void shouldFindRoutePiccadilyGardensToCornbrook() throws TramchesterException {
        validateAtLeastOneJourney(Stations.PiccadillyGardens, Stations.Cornbrook, 23*60, when);
    }

    @Test
    public void shouldFindRouteCornbrookToManAirport() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.ManAirport, (23*60)+20, when);
    }

    @Test
    @Ignore("Summary 2016 closures")
    public void shouldFindRouteDeansgateToVictoria() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Deansgate, Stations.Victoria, (23*60)+41, when);
    }

    @Test
    public void shouldFindRouteVeloToHoltTownAt8AM() throws TramchesterException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.HoltTown, 8*60, when);
    }

    @Test
    public void shouldFindRouteVeloToHoltTownAt8RangeOfTimes() throws TramchesterException {
        for(int i=0; i<60; i++) {
            int time = (8*60)+i;
            validateAtLeastOneJourney(Stations.VeloPark, Stations.HoltTown, time, when);
        }
    }

    @Test
    @Ignore("Summary 2016 closures")
    public void shouldFindRouteVeloToEndOfLines() throws TramchesterException {
        int offsetToMonday = MONDAY- when.getDayOfWeek();
        LocalDate nextMonday = when.plusDays(offsetToMonday);

        for (Location dest : Stations.EndOfTheLine) {
            validateAtLeastOneJourney(Stations.VeloPark, dest, 8*60, nextMonday);
        }
    }

    @Test
    @Ignore("Summary 2016 closures")
    public void shouldFindRouteVeloInterchanges() throws TramchesterException {
        for (Location dest : Stations.getInterchanges()) {
            validateAtLeastOneJourney(Stations.VeloPark, dest, 8*60, when);
        }
    }

    @Test
    @Ignore("Summary 2016 closures")
    public void shouldFindRouteVeloToDeansgate() throws TramchesterException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.Deansgate, 8*60, when);
    }

    @Test
    @Ignore("Summary 2016 closures")
    public void shouldFindRouteVeloToBroadway() throws TramchesterException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.Broadway, 8*60, when);
    }

    @Test
    @Ignore("Summary 2016 closures")
    public void shouldFindRouteVeloToPomona() throws TramchesterException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.Pomona, 8*60, when);
    }

    @Test
    public void shouldFindEndOfDayTwoStageJourney() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Altrincham, Stations.ManAirport, 23*60, when);
    }

    @Test
    @Ignore("Summary 2016 closures")
    public void shouldFindEndOfDayThreeStageJourney() throws TramchesterException {
        validateAtLeastOneJourney(Stations.Altrincham, Stations.ShawAndCrompton, (22*60)+45, when);
    }

    @Test
    public void shouldOnlyReturnFullJourneysForEndOfDaysJourney() throws TramchesterException {
        JourneyPlanRepresentation results = validateAtLeastOneJourney(Stations.Deansgate,
                Stations.ManAirport, (23*60)+10, when);
        Journey journey = results.getJourneys().stream().findFirst().get();

        assertEquals("number of times for stage one", 5, journey.getStages().get(0).getNumberOfServiceTimes());
        assertEquals("number of times for stage two", 5, journey.getStages().get(1).getNumberOfServiceTimes());
        assertEquals("available times", 5, journey.getNumberOfTimes());
    }

    @Test
    public void shouldInvokeQuickestRouteDirectly() throws TramchesterException {
        Response result = planner.quickestRoute(Stations.Altrincham.getId(), Stations.Deansgate.getId(), "23:00:00",
                when.toString("YYYY-MM-dd"));
        assertEquals(200, result.getStatus());
    }

}
