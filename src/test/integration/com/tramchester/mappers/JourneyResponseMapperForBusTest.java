package com.tramchester.mappers;

import com.tramchester.BusTest;
import com.tramchester.Dependencies;
import com.tramchester.IntegrationBusTestConfig;
import com.tramchester.domain.RawJourney;
import com.tramchester.domain.RawStage;
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
    private TramJourneyResponseMapper mapper;
    private Set<RawJourney> journeys;
    private List<RawStage> stages;

    private String stockportBusStation = "1800STBS001";
    private String stockportBridgefieldStreet = "1800SG15811";

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
        mapper = dependencies.get(TramJourneyResponseMapper.class);
        routeCalculator = dependencies.get(RouteCalculator.class);
        journeys = new HashSet<>();
        stages = new LinkedList<>();
    }

    @Test
    @Category({BusTest.class})
    public void shouldMapStockportCircularJourney() throws TramchesterException {
        //String svcId = findServiceId(stockportBusStation, stockportBridgefieldStreet, minutesFromMidnight);
        String svcId = "Serv002953"; // use above when timetable changes to find new svc id

        JourneyPlanRepresentation result = getJourneyPlanRepresentation(stockportBusStation, stockportBridgefieldStreet,
                svcId, 571);

        assertEquals(1,result.getJourneys().size());
    }

    private JourneyPlanRepresentation getJourneyPlanRepresentation(String begin, String end, String svcId, int minutesFromMidnight) throws TramchesterException {

        RawStage busStage = new RawStage(begin, "route text", "tram", "cssClass");
        busStage.setServiceId(svcId);
        busStage.setLastStation(end);

        stages.add(busStage);
        journeys.add(new RawJourney(stages,1));

        return mapper.map(journeys, minutesFromMidnight, 1);
    }
}
