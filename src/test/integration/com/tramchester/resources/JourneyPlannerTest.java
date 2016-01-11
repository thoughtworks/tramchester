package com.tramchester.resources;


import com.tramchester.BusTest;
import com.tramchester.Dependencies;
import com.tramchester.IntegrationBusTestConfig;
import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.services.SpatialService;
import org.joda.time.LocalDate;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.Timeout;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.List;

public class JourneyPlannerTest extends JourneyPlannerHelper {
    private static Dependencies dependencies;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10*60);
    private TramServiceDate today;
    private SpatialService spatialService;
    private GraphDatabaseService graphDatabaseService;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationBusTestConfig());
    }

    @Before
    public void beforeEachTestRuns() {
        today = new TramServiceDate(LocalDate.now());
        planner = dependencies.get(JourneyPlannerResource.class);
        spatialService = dependencies.get(SpatialService.class);
        graphDatabaseService = dependencies.get(GraphDatabaseService.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    @Category({BusTest.class})
    public void spikeMultipleToMultiple() throws TramchesterException {
        List<Node> nearEastDids;
        List<Node> nearPiccGardens;
        try (Transaction tx = graphDatabaseService.beginTx()) {
            nearEastDids = spatialService.getNearestStationsTo(53.4092, -2.2218, 5);
            nearPiccGardens = spatialService.getNearestStationsTo(53.4803, -2.2370, 10);
            tx.success();
        }

        planner.createJourneyPlan(nearEastDids, nearPiccGardens, "09:00:00", DaysOfWeek.Monday, today);
    }

    @Test
    @Category({BusTest.class})
    public void reproduceIssueWithRoute() throws TramchesterException {
        planner.createJourneyPlan("1800SB34231", "1800SB01681", "09:00:00", DaysOfWeek.Monday, today);
    }

}
