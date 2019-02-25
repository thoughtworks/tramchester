package com.tramchester.integration.mappers;

import com.tramchester.Dependencies;
import com.tramchester.TestConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.factory.JourneyDTOFactory;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.integration.BusTest;
import com.tramchester.integration.IntegrationBusTestConfig;
import com.tramchester.livedata.LiveDataEnricher;
import com.tramchester.mappers.HeadsignMapper;
import com.tramchester.mappers.JourneysMapper;
import com.tramchester.repository.LiveDataRepository;
import org.junit.*;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static junit.framework.TestCase.assertEquals;

public class JourneyResponseMapperForBusTest extends JourneyResponseMapperTest {

    private static Dependencies dependencies;
    private JourneysMapper mapper;
    private Set<RawJourney> journeys;
    private List<RawStage> stages;

    private Location stockportBusStation = new Station("1800STBS001", "stockportArea", "Bus station", new LatLong(1.5, 1.5), false);
    private Location stockportBridgefieldStreet = new Station("1800SG18471", "stockportArea", "Bridgefield Street",
            new LatLong(1.5, 1.5), false);
    private List<String> notes;
    private LiveDataRepository liveDataRepository;

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
        notes = new LinkedList<>();
        mapper = dependencies.get(JourneysMapper.class);
        routeCalculator = dependencies.get(RouteCalculator.class);
        liveDataRepository = dependencies.get(LiveDataRepository.class);
        journeys = new HashSet<>();
        stages = new LinkedList<>();
    }

    @Test
    @Category({BusTest.class})
    @Ignore("Work in progress")
    public void shouldMapStockportCircularJourney() throws TramchesterException {
//        LocalDate now = LocalDate.now();
////        DayOfWeek offset = now.getDayOfWeek().minus(MONDAY);
////
        LocalDate when = TestConfig.nextTuesday(0);
        String svcId = findServiceId(stockportBusStation.getId(), stockportBridgefieldStreet.getId(), when,
                LocalTime.of(9,42));
        //String svcId = "Serv002953"; // use above when timetable changes to find new svc id

        JourneyPlanRepresentation result = getJourneyPlanRepresentation(stockportBusStation, stockportBridgefieldStreet,
                svcId, 42, LocalTime.of(9,42), new TramServiceDate(when));

        assertEquals(1,result.getJourneys().size());
    }

    private JourneyPlanRepresentation getJourneyPlanRepresentation(Location begin, Location end, String svcId,
                                                                   int cost, LocalTime minutesFromMidnight, TramServiceDate queryDate) {

        RawVehicleStage busStage = new RawVehicleStage(begin, "route text", TransportMode.Bus, "cssClass");
        busStage.setServiceId(svcId);
        busStage.setLastStation(end,1 );
        busStage.setCost(cost);

        stages.add(busStage);
        journeys.add(new RawJourney(stages, minutesFromMidnight));

        LiveDataEnricher liveDataEnricher = new LiveDataEnricher(liveDataRepository, queryDate,
                TramTime.of(minutesFromMidnight));
        StageDTOFactory stageFactory = new StageDTOFactory(liveDataEnricher);
        HeadsignMapper headsignMapper = new HeadsignMapper();
        JourneyDTOFactory factory = new JourneyDTOFactory(stageFactory, headsignMapper);
        SortedSet<JourneyDTO> mapped = mapper.map(factory, journeys, 30);
        return new JourneyPlanRepresentation(mapped, notes);
    }
}
