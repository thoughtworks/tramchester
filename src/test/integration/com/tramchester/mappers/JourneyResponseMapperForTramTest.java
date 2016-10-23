package com.tramchester.mappers;


import com.tramchester.Dependencies;
import com.tramchester.IntegrationTramTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.resources.JourneyPlannerHelper;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JourneyResponseMapperForTramTest extends JourneyResponseMapperTest {
    private LocalTime sevenAM = new LocalTime(7, 0);
    private LocalTime eightAM = new LocalTime(8, 0);

    private static Dependencies dependencies;
    private TramJourneyResponseMapper mapper;
    private Set<RawJourney> journeys;
    private List<RawStage> stages;

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

        RawVehicleStage vicToRoch = getRawVehicleStage(Stations.Victoria, Stations.Rochdale, "routeText", time, 42);

        stages.add(vicToRoch);
        journeys.add(new RawJourney(stages, time));
        JourneyPlanRepresentation result = mapper.map(journeys, 30);

        JourneyDTO journey = result.getJourneys().stream().findFirst().get();
        StageDTO stage = journey.getStages().get(0);
        // for this service trips later in the list actually depart earlier, so this would fail
        assertTrue(stage.getFirstDepartureTime().isBefore(new LocalTime(16,00)));
    }

    @Test
    public void shouldEnsureTripsAreOrderByEarliestFirstSpanningMidnightService() throws TramchesterException {
        int pm1044  = (22*60)+44;

        RawVehicleStage rawStage = getRawVehicleStage(Stations.ManAirport, Stations.Cornbrook, "routename",
                pm1044, 42);

        stages.add(rawStage);
        journeys.add(new RawJourney(stages,pm1044));
        JourneyPlanRepresentation result = mapper.map(journeys, 60);

        JourneyDTO journey = result.getJourneys().stream().findFirst().get();
        assertFalse(journey.getStages().isEmpty());
        StageDTO stage = journey.getStages().get(0);
        // for this service trips later in the list actually depart earlier, so this would fail
        assertTrue(stage.getFirstDepartureTime().isBefore(new LocalTime(22,55)));
    }

    @Test
    public void shouldMapSimpleJourney() throws TramchesterException {
        int am7 = 7 * 60;

        RawVehicleStage altToCorn = getRawVehicleStage(Stations.Altrincham, Stations.Cornbrook, "route name", am7, 42);

        stages.add(altToCorn);
        journeys.add(new RawJourney(stages,am7));
        JourneyPlanRepresentation result = mapper.map(journeys, 30);

        assertEquals(1,result.getJourneys().size());
        JourneyDTO journey = result.getJourneys().stream().findFirst().get();
        assertEquals(1, journey.getStages().size());
        StageDTO stage = journey.getStages().get(0);
        assertEquals(Stations.Altrincham.getId(),stage.getFirstStation().getId());
        assertEquals(Stations.Cornbrook.getId(),stage.getLastStation().getId());
        assertTrue(stage.getDuration()>0);
        assertTrue(stage.getFirstDepartureTime().isAfter(sevenAM));
        assertTrue(stage.getFirstDepartureTime().isBefore(eightAM));
        assertTrue(stage.getExpectedArrivalTime().isAfter(sevenAM));
        assertTrue(stage.getExpectedArrivalTime().isBefore(eightAM));
    }

    @Test
    public void shouldMapTwoStageJourney() throws TramchesterException {
        int pm10 = 10 * 60;
        Location begin = Stations.Altrincham;
        Location middle = Stations.Cornbrook;
        Location end = Stations.ManAirport;

        RawVehicleStage rawStageA = getRawVehicleStage(begin, middle, "route text", pm10, 42);
        RawVehicleStage rawStageB = getRawVehicleStage(middle, end, "route2 text", pm10+42, 20);

        stages.add(rawStageA);
        stages.add(rawStageB);
        journeys.add(new RawJourney(stages, pm10));

        JourneyPlanRepresentation result = mapper.map(journeys, 30);

        assertEquals(1,result.getJourneys().size());

        JourneyDTO journey = result.getJourneys().stream().findFirst().get();

        assertEquals(2, journey.getStages().size());

        StageDTO stage1 = journey.getStages().get(0);
        assertEquals(begin.getId(),stage1.getFirstStation().getId());
        assertEquals(middle.getId(),stage1.getLastStation().getId());

        StageDTO stage2 = journey.getStages().get(1);
        assertEquals(middle.getId(),stage2.getFirstStation().getId());
        assertEquals(end.getId(),stage2.getLastStation().getId());

        assertEquals("Change tram at",stage2.getPrompt());
    }

    @Test
    public void shouldMapWalkingStageJourney() throws TramchesterException {
        int pm10 = 22 * 60;

        RawWalkingStage walkingStage = new RawWalkingStage(Stations.Deansgate, Stations.MarketStreet, 10);

        stages.add(walkingStage);
        journeys.add(new RawJourney(stages,pm10));

        JourneyPlanRepresentation result = mapper.map(journeys, 30);

        assertEquals(1,result.getJourneys().size());
        JourneyDTO journey = result.getJourneys().stream().findFirst().get();
        assertEquals(1, journey.getStages().size());

        StageDTO stage = journey.getStages().get(0);
        assertEquals(Stations.Deansgate.getId(),stage.getFirstStation().getId());
        assertEquals(Stations.MarketStreet.getId(),stage.getLastStation().getId());

        assertEquals("Walk to",stage.getPrompt());
    }

    @Test
    public void shouldMapThreeStageJourneyWithWalk() throws TramchesterException {
        int am10 = 10 * 60;
        Location begin = Stations.Altrincham;
        Location middleA = Stations.Deansgate;
        Location middleB = Stations.MarketStreet;
        Location end = Stations.Bury;

        RawVehicleStage rawStageA = getRawVehicleStage(begin, middleA, "route text", am10, 42);
        int walkCost = 10;
        RawWalkingStage walkingStage = new RawWalkingStage(middleA, middleB, walkCost);
        RawVehicleStage finalStage = getRawVehicleStage(middleB, end, "route3 text", am10, 42);

        stages.add(rawStageA);
        stages.add(walkingStage);
        stages.add(finalStage);
        journeys.add(new RawJourney(stages,am10));

        JourneyPlanRepresentation result = mapper.map(journeys, 30);
        assertEquals(1,result.getJourneys().size());
        JourneyDTO journey = result.getJourneys().stream().findFirst().get();
        assertEquals(3, journey.getStages().size());
        LocalTime arrivalTime = journey.getExpectedArrivalTime();
        assertTrue(arrivalTime.isAfter(new LocalTime(10,10)));

        StageDTO stage1 = journey.getStages().get(0);
        assertEquals("Board tram at",stage1.getPrompt());

        StageDTO stage2 = journey.getStages().get(1);
        assertEquals(middleB.getId(),stage2.getActionStation().getId());
        assertEquals(middleB.getId(),stage2.getLastStation().getId());
        assertEquals("Walk to",stage2.getPrompt());
        assertEquals(walkCost, stage2.getDuration());

        StageDTO stage3 = journey.getStages().get(2);
        assertEquals("Board tram at",stage3.getPrompt());
        assertEquals(middleB.getId(),stage3.getFirstStation().getId());
        assertEquals(end.getId(),stage3.getLastStation().getId());

    }

    @Test
    public void shouldMapEndOfDayJourneyCorrectly() throws TramchesterException {
        int startTime = (22 * 60)+55;
        Location start = Stations.Altrincham;
        Location middle = Stations.Cornbrook;

        RawVehicleStage rawStageA = getRawVehicleStage(start, middle, "route text", startTime, 22);
        RawVehicleStage rawStageB = getRawVehicleStage(middle, Stations.ManAirport, "rouet2 text", startTime+20, 42);

        stages.add(rawStageA);
        stages.add(rawStageB);
        journeys.add(new RawJourney(stages,startTime));

        JourneyPlanRepresentation result = mapper.map(journeys, 30);

        assertTrue(result.getJourneys().size()>0);
    }

    private RawVehicleStage getRawVehicleStage(Location start, Location finish, String routeName, int startTime, int cost) throws TramchesterException {

        LocalDate when = JourneyPlannerHelper.nextMonday();
        String svcId = findServiceId(start.getId(), finish.getId(), when, startTime);
        RawVehicleStage rawVehicleStage = new RawVehicleStage(start, routeName, TransportMode.Tram, "cssClass");
        rawVehicleStage.setCost(cost);
        rawVehicleStage.setLastStation(finish);
        rawVehicleStage.setServiceId(svcId);
        return rawVehicleStage;
    }

}
