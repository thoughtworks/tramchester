package com.tramchester.resources;

import com.tramchester.Dependencies;
import com.tramchester.IntegrationTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.graph.UnknownStationException;
import org.joda.time.DateTime;
import org.junit.*;

import static org.junit.Assert.fail;

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
    @Ignore("St peters square has been deleted from the list of metrolink statiosn")
    public void shouldNotFindRouteToStPetersSquare() throws Exception {
        try {
            planner.createJourneyPlan(Stations.Altrincham, Stations.StPetersSquare, "11:43:00",
                    DaysOfWeek.Monday, today);
            fail("should have throw");
        }
        catch (UnknownStationException expected) {
            // expected
        }
    }



}
