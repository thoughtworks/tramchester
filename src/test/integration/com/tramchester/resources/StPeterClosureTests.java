package com.tramchester.resources;

import com.tramchester.Dependencies;
import com.tramchester.IntegrationTramTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.exceptions.UnknownStationException;
import org.joda.time.LocalDate;
import org.junit.*;

import static org.junit.Assert.fail;

@Ignore
public class StPeterClosureTests extends JourneyPlannerHelper {
    private static Dependencies dependencies;
    private TramServiceDate today;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
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
    @Ignore("St peters square has been deleted from the list of metrolink stations by tfgm")
    public void shouldNotFindRouteToStPetersSquare() throws Exception {
        try {
            planner.createJourneyPlan(Stations.Altrincham.getId(), Stations.StPetersSquare,
                    DaysOfWeek.Monday, today, (11*60)+43);
            fail("should have throw");
        }
        catch (UnknownStationException expected) {
            // expected
        }
    }



}
