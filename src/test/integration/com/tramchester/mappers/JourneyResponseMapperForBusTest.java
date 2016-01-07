package com.tramchester.mappers;

import com.tramchester.BusTest;
import com.tramchester.Dependencies;
import com.tramchester.IntegrationBusTestConfig;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.Stage;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.JourneyPlanRepresentation;
import com.tramchester.graph.RouteCalculator;
import org.junit.*;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;

public class JourneyResponseMapperForBusTest extends JourneyResponseMapperTest {

    private static Dependencies dependencies;
    private JourneyResponseMapper mapper;
    private Set<Journey> journeys;
    private List<Stage> stages;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationBusTestConfig());
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }


    @Before
    public void beforeEachTestRuns() {
        mapper = dependencies.get(JourneyResponseMapper.class);
        routeCalculator = dependencies.get(RouteCalculator.class);
        journeys = new HashSet<>();
        stages = new LinkedList<>();
    }

    @Test
    @Category({BusTest.class})
    public void shouldMapStockportCircularJourneyReproduceIssue() throws TramchesterException {

        String stockportBusStation = "1800STBS001";
        String stockportBridgefieldStreet = "1800SG15811";

        int minutesFromMidnight = 571;
        //String svcId = findServiceId(stockportBusStation, stockportBridgefieldStreet, minutesFromMidnight);
        String svcId = "Serv002953"; // use above when timetable changes to find new svc id

        Stage busStage = new Stage(stockportBusStation, "route text", "tram", "cssClass");
        busStage.setServiceId(svcId);
        busStage.setLastStation(stockportBridgefieldStreet);

        stages.add(busStage);
        journeys.add(new Journey(stages));

        JourneyPlanRepresentation result = mapper.map(journeys, minutesFromMidnight, 1);

        assertEquals(1,result.getJourneys().size());
    }
}
