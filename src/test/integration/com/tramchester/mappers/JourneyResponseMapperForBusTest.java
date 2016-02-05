package com.tramchester.mappers;

import com.tramchester.BusTest;
import com.tramchester.Dependencies;
import com.tramchester.IntegrationBusTestConfig;
import com.tramchester.domain.*;
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
    private Set<RawJourney> journeys;
    private List<RawStage> stages;

    private Station stockportBusStation = new Station("1800STBS001", "stockportArea", "Bus station", 1.5, 1.5, false);
    private Station stockportBridgefieldStreet = new Station("1800SG15811", "stockportArea", "Bridgefield Street",
            1.5, 1.5, false);

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
        mapper = dependencies.get(GenericJourneyResponseMapper.class);
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

    private JourneyPlanRepresentation getJourneyPlanRepresentation(Station begin, Station end, String svcId,
                                                                   int minutesFromMidnight) throws TramchesterException {

        int elapsedTime = 8*60;
        RawTravelStage busStage = new RawTravelStage(begin, "route text", TransportMode.Bus, "cssClass", elapsedTime);
        busStage.setServiceId(svcId);
        busStage.setLastStation(end);

        stages.add(busStage);
        journeys.add(new RawJourney(stages));

        return mapper.map(journeys, new TimeWindow(minutesFromMidnight, 30));
    }
}
