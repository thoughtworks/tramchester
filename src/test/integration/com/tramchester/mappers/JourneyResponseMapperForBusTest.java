package com.tramchester.mappers;

import com.tramchester.Dependencies;
import com.tramchester.IntegrationBusTestConfig;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.Stage;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.JourneyPlanRepresentation;
import org.junit.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;

public class JourneyResponseMapperForBusTest {

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
        journeys = new HashSet<>();
        stages = new LinkedList<>();
    }

    @Test
    @Ignore("work in progress")
    public void shouldMapStockportCircularJourneyReproduceIssue() throws TramchesterException {
        String stockportBusStation = "1800STBS001";
        String stockportBridgefieldStreet = "1800SG15811";

        Stage busStage = new Stage(stockportBusStation, "route text", "tram", "cssClass");
        busStage.setServiceId("Serv001653");
        busStage.setLastStation(stockportBridgefieldStreet);

        stages.add(busStage);
        journeys.add(new Journey(stages));

        JourneyPlanRepresentation result = mapper.map(journeys, 571, 1);

        assertEquals(1,result.getJourneys().size());

    }
}
