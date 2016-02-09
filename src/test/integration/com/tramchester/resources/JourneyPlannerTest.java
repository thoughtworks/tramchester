package com.tramchester.resources;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.BusTest;
import com.tramchester.Dependencies;
import com.tramchester.IntegrationBusTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.StageWithTiming;
import org.joda.time.LocalDate;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.util.List;
import java.util.SortedSet;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class JourneyPlannerTest extends JourneyPlannerHelper {
    private static Dependencies dependencies;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10*60);
    private TramServiceDate today;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationBusTestConfig());
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
    @Category({BusTest.class})
    public void shouldFindRoutesForLatLongToStationId() throws TramchesterException, IOException {
        LatLong startLocation = new LatLong(53.4092, -2.2218);

        String startId = formId(startLocation);

        JourneyPlanRepresentation plan = planner.createJourneyPlan(startId,
                Stations.PiccadilyGardens.getId(),
                DaysOfWeek.Monday, today, 9*60);
        SortedSet<Journey> journeys = plan.getJourneys();
        assertTrue(journeys.size()>=1);
        Journey journey = journeys.first();
        List<StageWithTiming> stages = journey.getStages();
        stages.forEach(stage ->
                assertEquals(TransportMode.Bus, stage.getMode())
        );
    }

    public static String formId(LatLong startLocation) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(startLocation);
    }

    @Test
    @Category({BusTest.class})
    public void reproduceIssueWithRoute() throws TramchesterException, IOException {
        planner.createJourneyPlan("1800SB34231", "1800SB01681", DaysOfWeek.Monday, today, 9*60);
    }

}
