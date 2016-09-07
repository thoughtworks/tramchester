package com.tramchester.resources;

import com.tramchester.Dependencies;
import com.tramchester.IntegrationTramTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.Location;
import com.tramchester.domain.exceptions.TramchesterException;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.*;
import org.junit.rules.TestName;
import org.junit.rules.Timeout;
import org.slf4j.MDC;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class TramJourneyPlannerSummer2016Closures extends JourneyPlannerHelper {
    public static final int AM10 = 10 * 60;
    private static Dependencies dependencies;
    private LocalDate date;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(200); // TODO should not have to set this so high

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @Before
    public void beforeEachTestRuns() {
        // for logging
        MDC.put("test", testName.getMethodName());

        date = new LocalDate(2016,7,15); // closure starts on the 27th
        //date = LocalDate.now();
        planner = dependencies.get(JourneyPlannerResource.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }


    @Test
    public void shouldFindWalkingRouteInCentre() throws TramchesterException {
        checkRouteNext7Days(Stations.Deansgate, Stations.MarketStreet, date, AM10);
    }

    @Test
    public void shouldCrossCityWithAWalk() throws TramchesterException {
        checkRouteNext7Days(Stations.Altrincham, Stations.Bury, date, AM10);
    }

}
