package com.tramchester.mappers;


import com.tramchester.Dependencies;
import com.tramchester.IntegrationTramTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.*;
import com.tramchester.graph.RouteCalculator;
import org.joda.time.LocalDate;
import org.junit.*;

import java.io.IOException;
import java.time.LocalTime;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.joda.time.DateTimeConstants.MONDAY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JourneyResponseMapperForTramTest extends JourneyResponseMapperTest {
    private LocalTime sevenAM = LocalTime.of(7, 0);
    private LocalTime eightAM = LocalTime.of(8, 0);

    private static Dependencies dependencies;
    private TramJourneyResponseMapper mapper;
    private Set<RawJourney> journeys;
    private List<TransportStage> stages;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
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
    public void shouldEnsureTripsAreOrderByEarliestFirst() throws TramchesterException {
        int time = 930;
        int elapsedTime = time +1;

        RawVehicleStage vicToRoch = getRawVehicleStage(Stations.Victoria, Stations.Rochdale, "routeText", time);

        stages.add(vicToRoch);
        journeys.add(new RawJourney(stages));
        JourneyPlanRepresentation result = mapper.map(journeys, new TimeWindow(930, 30));

        Journey journey = result.getJourneys().stream().findFirst().get();
        PresentableStage stage = journey.getStages().get(0);
        // for this service trips later in the list actually depart earlier, so this would fail
        assertTrue(stage.getFirstDepartureTime().isBefore(LocalTime.of(16,00)));
    }

    @Test
    public void shouldEnsureTripsAreOrderByEarliestFirstSpanningMidnightService() throws TramchesterException {
        int pm1044  = (22*60)+44;
        int elapsedTime = pm1044 +1 ;

        RawVehicleStage rawStage = getRawVehicleStage(Stations.ManAirport, Stations.Cornbrook, "routename",
                pm1044);

        stages.add(rawStage);
        journeys.add(new RawJourney(stages));
        JourneyPlanRepresentation result = mapper.map(journeys, new TimeWindow(elapsedTime, 60));

        Journey journey = result.getJourneys().stream().findFirst().get();
        assertFalse(journey.getStages().isEmpty());
        PresentableStage stage = journey.getStages().get(0);
        // for this service trips later in the list actually depart earlier, so this would fail
        assertTrue(stage.getFirstDepartureTime().isBefore(LocalTime.of(22,55)));
    }

    @Test
    public void shouldMapSimpleJourney() throws TramchesterException {
        int am7 = 7 * 60;
        int elapsedTime = am7 +1;

        RawVehicleStage altToCorn = getRawVehicleStage(Stations.Altrincham, Stations.Cornbrook, "route name", am7);

        stages.add(altToCorn);
        journeys.add(new RawJourney(stages));
        JourneyPlanRepresentation result = mapper.map(journeys, new TimeWindow(am7, 30));

        assertEquals(1,result.getJourneys().size());
        Journey journey = result.getJourneys().stream().findFirst().get();
        assertEquals(1, journey.getStages().size());
        PresentableStage stage = journey.getStages().get(0);
        assertEquals(Stations.Altrincham,stage.getFirstStation());
        assertEquals(Stations.Cornbrook,stage.getLastStation());
        assertTrue(stage.getDuration()>0);
        assertTrue(stage.getFirstDepartureTime().isAfter(sevenAM));
        assertTrue(stage.getFirstDepartureTime().isBefore(eightAM));
        assertTrue(stage.getExpectedArrivalTime().isAfter(sevenAM));
        assertTrue(stage.getExpectedArrivalTime().isBefore(eightAM));

        assertTrue(stage.getNumberOfServiceTimes()>1);
    }

    @Test
    public void shouldMapTwoStageJourney() throws TramchesterException {
        int pm10 = 10 * 60;
        Location begin = Stations.Altrincham;
        Location middle = Stations.Cornbrook;
        Location end = Stations.ManAirport;

        int elapsedTime = pm10 + 1;

        RawVehicleStage rawStageA = getRawVehicleStage(begin, middle, "route text", pm10);
        RawVehicleStage rawStageB = getRawVehicleStage(middle, end, "route2 text", pm10);

        stages.add(rawStageA);
        stages.add(rawStageB);
        journeys.add(new RawJourney(stages));

        JourneyPlanRepresentation result = mapper.map(journeys, new TimeWindow(pm10, 30));
        assertEquals(1,result.getJourneys().size());
        Journey journey = result.getJourneys().stream().findFirst().get();
        assertEquals(2, journey.getStages().size());

        PresentableStage stage2 = journey.getStages().get(1);
        assertEquals(middle,stage2.getFirstStation());
        assertEquals(end,stage2.getLastStation());

        assertTrue(stage2.getNumberOfServiceTimes()>1);
        assertEquals("Change tram at",stage2.getPrompt());
    }

    @Test
    public void shouldMapWalkingStageJourney() throws TramchesterException {
        int pm10 = 22 * 60;

        RawWalkingStage walkingStage = new RawWalkingStage(Stations.Deansgate, Stations.MarketStreet, 10);

        stages.add(walkingStage);
        journeys.add(new RawJourney(stages));

        JourneyPlanRepresentation result = mapper.map(journeys, new TimeWindow(pm10, 30));

        assertEquals(1,result.getJourneys().size());
        Journey journey = result.getJourneys().stream().findFirst().get();
        assertEquals(1, journey.getStages().size());

        PresentableStage stage = journey.getStages().get(0);
        assertEquals(Stations.Deansgate,stage.getFirstStation());
        assertEquals(Stations.MarketStreet,stage.getLastStation());

        assertEquals(1, stage.getNumberOfServiceTimes());
        assertEquals("Walk to",stage.getPrompt());
    }

    @Test
    public void shouldMapThreeStageJourneyWithWalk() throws TramchesterException {
        int am10 = 10 * 60;
        Location begin = Stations.Altrincham;
        Location middleA = Stations.Deansgate;
        Location middleB = Stations.MarketStreet;
        Location end = Stations.Bury;

        RawVehicleStage rawStageA = getRawVehicleStage(begin, middleA, "route text", am10);
        int walkCost = 10;
        RawWalkingStage walkingStage = new RawWalkingStage(middleA, middleB, walkCost);
        RawVehicleStage finalStage = getRawVehicleStage(middleB, end, "route3 text", am10);

        stages.add(rawStageA);
        stages.add(walkingStage);
        stages.add(finalStage);
        journeys.add(new RawJourney(stages));

        JourneyPlanRepresentation result = mapper.map(journeys, new TimeWindow(am10, 30));
        assertEquals(1,result.getJourneys().size());
        Journey journey = result.getJourneys().stream().findFirst().get();
        assertEquals(3, journey.getStages().size());
        LocalTime arrivalTime = journey.getExpectedArrivalTime();
        assertTrue(arrivalTime.isAfter(LocalTime.of(10,10)));

        PresentableStage stage1 = journey.getStages().get(0);
        assertEquals("Board tram at",stage1.getPrompt());

        PresentableStage stage2 = journey.getStages().get(1);
        assertEquals(middleB,stage2.getActionStation());
        assertEquals(middleB,stage2.getLastStation());
        assertEquals("Walk to",stage2.getPrompt());
        assertEquals(walkCost, stage2.getDuration());

        PresentableStage stage3 = journey.getStages().get(2);
        assertEquals("Board tram at",stage3.getPrompt());
        assertEquals(middleB,stage3.getFirstStation());
        assertEquals(end,stage3.getLastStation());

    }

    @Test
    public void shouldMapEndOfDayJourneyCorrectly() throws TramchesterException {
        int pm23 = (23 * 60)+25;
        Location start = Stations.Altrincham;
        Location middle = Stations.Cornbrook;

        RawVehicleStage rawStageA = getRawVehicleStage(start, middle, "route text", pm23);
        RawVehicleStage rawStageB = getRawVehicleStage(middle, Stations.ManAirport, "rouet2 text", pm23);

        stages.add(rawStageA);
        stages.add(rawStageB);
        journeys.add(new RawJourney(stages));

        JourneyPlanRepresentation result = mapper.map(journeys, new TimeWindow(pm23, 30));

        Journey journey = result.getJourneys().stream().findFirst().get();
        assertTrue(journey.getNumberOfTimes()>0);
    }

    private RawVehicleStage getRawVehicleStage(Location start, Location finish, String routeName, int startTime) throws TramchesterException {
        LocalDate now = LocalDate.now();
        int offset = now.getDayOfWeek()-MONDAY;
        LocalDate when = now.plusDays(offset);
        String svcId = findServiceId(start.getId(), finish.getId(), when, startTime);
        RawVehicleStage rawVehicleStage = new RawVehicleStage(start, routeName, TransportMode.Tram, "cssClass");
        rawVehicleStage.setLastStation(finish);
        rawVehicleStage.setServiceId(svcId);
        return rawVehicleStage;
    }

}
