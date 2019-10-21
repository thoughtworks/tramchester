package com.tramchester.integration.resources;


import com.tramchester.Dependencies;
import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.integration.BusTest;
import com.tramchester.integration.IntegrationBusTestConfig;
import com.tramchester.integration.Stations;
import com.tramchester.resources.JourneyPlannerResource;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.SortedSet;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class JourneyPlannerTest extends JourneyPlannerHelper {
    private static Dependencies dependencies;
    private TramServiceDate today;
    private CreateQueryTimes createQueryTimes;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10*60);

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationBusTestConfig());
    }

    @Before
    public void beforeEachTestRuns() {
        today = new TramServiceDate(LocalDate.now());
        planner = dependencies.get(JourneyPlannerResource.class);
        createQueryTimes = dependencies.get(CreateQueryTimes.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    protected JourneyPlanRepresentation getJourneyPlan(Location start, Location end, LocalTime queryTime, TramServiceDate queryDate) {
        List<LocalTime> queryTimes = createQueryTimes.generate(queryTime);
        return planner.createJourneyPlan(start.getId(), end.getId(), queryDate, queryTimes , TramTime.of(queryTime));
    }

    @Test
    @Category({BusTest.class})
    @Ignore("experimental")
    public void shouldFindRoutesForLatLongToStationId() throws TramchesterException {
        LatLong startLocation = new LatLong(53.4092, -2.2218);

        String startId = formId(startLocation);

        // todo currently finds far too many start points

        JourneyPlanRepresentation plan = planner.createJourneyPlan(startId,
                Stations.PiccadillyGardens.getId(),
                today, createQueryTimes.generate(LocalTime.of(9,0)), TramTime.create(9,0));
        SortedSet<JourneyDTO> journeys = plan.getJourneys();
        assertTrue(journeys.size()>=1);
        JourneyDTO journey = journeys.first();
        List<StageDTO> stages = journey.getStages();
        stages.forEach(stage ->
                assertEquals(TransportMode.Bus, stage.getMode())
        );
    }

    private static String formId(LatLong startLocation) {
        return String.format("lat=%f&lon=%f", startLocation.getLat(), startLocation.getLon());
    }

    @Test
    @Category({BusTest.class})
    @Ignore("experimental")
    public void reproduceIssueWithRoute() throws TramchesterException {
        planner.createJourneyPlan("1800SB34231", "1800SB01681", today,
                createQueryTimes.generate(LocalTime.of(9,0)), TramTime.create(9,0));
    }

}
