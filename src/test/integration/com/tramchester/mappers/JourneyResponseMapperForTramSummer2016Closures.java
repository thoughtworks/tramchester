package com.tramchester.mappers;

import com.tramchester.ClosureTest;
import com.tramchester.Dependencies;
import com.tramchester.IntegrationTramTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.Interchanges;
import com.tramchester.domain.Location;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.resources.JourneyPlannerHelper;
import com.tramchester.resources.JourneyPlannerResource;
import org.joda.time.LocalDate;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.Timeout;

import java.io.IOException;

public class JourneyResponseMapperForTramSummer2016Closures extends JourneyPlannerHelper {
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
        when = new LocalDate(2016, 06, 27);
        planner = dependencies.get(JourneyPlannerResource.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    @Category(ClosureTest.class)
    public void shouldFindEndsOfLineToEndOfLineEast() throws TramchesterException {
        for (Location start : Stations.EndOfTheLineEast ) {
            for (Location dest : Stations.EndOfTheLineEast) {
                checkRouteNext7Days(start,dest, when, 10*60);
            }
        }
    }

    @Test
    @Category(ClosureTest.class)
    public void shouldFindInterchangesToEndOfLineEast() throws TramchesterException {
        for (Location start : Stations.EndOfTheLineEast ) {
            for (Location dest : Stations.getEastInterchanges()) {
                checkRouteNext7Days(start,dest, when, 10*60);
            }
        }
    }

    @Test
    @Category(ClosureTest.class)
    public void shouldFindEndsOfLinesToEndOfLineWestAndNoEcclesTrams() throws TramchesterException {
        for (Location start : Stations.EndOfTheLineWest ) {
            for (Location dest : Stations.EndOfTheLineWest) {
                if (!Stations.Eccles.equals(start) && !Stations.Eccles.equals(dest))
                checkRouteNext7Days(start,dest, when, 10*60);
            }
        }
    }

    @Test
    @Category(ClosureTest.class)
    public void shouldFindInterchangesToEndOfLineWest() throws TramchesterException {
        for (Location start : Stations.EndOfTheLineWest ) {
            for (Location dest : Stations.getWestInterchanges()) {
                if (!Stations.Eccles.equals(start) && !Stations.HarbourCity.equals(dest))
                    checkRouteNext7Days(start,dest, when, 10*60);
            }
        }
    }

    @Test
    @Category(ClosureTest.class)
    public void shouldFindWestInterchangesToWestInterchanges() throws TramchesterException {
        for (Location start : Stations.getWestInterchanges()) {
            for (Location dest : Stations.getWestInterchanges()) {
                if (!Stations.HarbourCity.equals(start) && !Stations.HarbourCity.equals(dest)) {
                    checkRouteNext7Days(start, dest, when, 9 * 60);
                }
            }
        }
    }

    @Test
    @Category(ClosureTest.class)
    public void testExchangeSquareToAshton() throws TramchesterException {
        checkRouteNext7Days(Stations.ExchangeSquare, Stations.Ashton, new LocalDate(2016,6,27), 9*60);
    }

    @Test
    @Category(ClosureTest.class)
    public void shouldFindEastInterchangesToEastInterchanges() throws TramchesterException {
        for (Location start : Stations.getEastInterchanges()) {
            for (Location dest : Stations.getEastInterchanges()) {
                checkRouteNext7Days(start, dest, when, 9*60);
            }
        }
    }
}
