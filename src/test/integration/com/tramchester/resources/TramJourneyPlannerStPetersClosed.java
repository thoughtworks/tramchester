package com.tramchester.resources;

import com.tramchester.Dependencies;
import com.tramchester.IntegrationTramTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.exceptions.TramchesterException;
import org.joda.time.LocalDate;
import org.junit.*;
import org.junit.rules.Timeout;

import java.io.IOException;


public class TramJourneyPlannerStPetersClosed extends JourneyPlannerHelper {
    private static Dependencies dependencies;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10 * 60);
    private LocalDate when;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @Before
    public void beforeEachTestRuns() {
        when = LocalDate.now().plusWeeks(2);
        planner = dependencies.get(JourneyPlannerResource.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Ignore("Work in progress")
    @Test
    public void shouldFindEndOfLinesToEndOfLinesEast() throws TramchesterException {
        for (String start : Stations.EndOfTheLineEast) {
            for (String dest : Stations.EndOfTheLineEast) {
                checkRouteForAllDays(start, dest, 10 * 60, when);
            }
        }
    }

    @Ignore("Work in progress")
    @Test
    public void shouldFindEndOfLinesToEndOfLinesWest() throws TramchesterException {
        for (String start : Stations.EndOfTheLineWest) {
            for (String dest : Stations.EndOfTheLineWest) {
                checkRouteForAllDays(start, dest, 10 * 60, when);
            }
        }
    }
}
