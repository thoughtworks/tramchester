package com.tramchester.integration.mappers;


import com.tramchester.Dependencies;
import com.tramchester.TestConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import com.tramchester.integration.resources.JourneyPlannerHelper;
import com.tramchester.mappers.TramJourneyResponseMapper;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JourneyResponseMapperForTramTest extends JourneyResponseMapperTest {
    private final LocalDate when = TestConfig.nextTuesday(0);
    private LocalTime sevenAM;
    private LocalTime eightAM;

    private static Dependencies dependencies;
    private TramJourneyResponseMapper mapper;
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
        stages = new LinkedList<>();
        sevenAM = LocalTime.of(7, 0);
        eightAM = LocalTime.of(8, 0);
    }

    @Test
    public void shouldEnsureTripsAreOrderByEarliestFirst() throws TramchesterException {
        LocalTime time = LocalTime.of(15,30);

        RawVehicleStage vicToRoch = getRawVehicleStage(Stations.Victoria, Stations.Rochdale, "routeText", time, 42, when);
        stages.add(vicToRoch);

        Optional<Journey> result = mapper.createJourney(new RawJourney(stages, time), 30);

        assertTrue(result.isPresent());
        Journey journey = result.get();
        TransportStage stage = journey.getStages().get(0);
        // for this service trips later in the list actually depart earlier, so this would fail
        assertTrue(stage.getFirstDepartureTime().asLocalTime().isBefore(LocalTime.of(16,0)));
    }

    @Test
    public void shouldEnsureTripsAreOrderByEarliestFirstSpanningMidnightService() throws TramchesterException {
        LocalTime pm1044  = LocalTime.of(22,44);

        RawVehicleStage rawStage = getRawVehicleStage(Stations.ManAirport, Stations.Cornbrook, "routename",
                pm1044, 42, when);

        stages.add(rawStage);
        Optional<Journey> result = mapper.createJourney(new RawJourney(stages,pm1044), 60);

        assertTrue(result.isPresent());
        Journey journey = result.get();
        assertFalse(journey.getStages().isEmpty());
        TransportStage stage = journey.getStages().get(0);
        // for this service trips later in the list actually depart earlier, so this would fail
        assertTrue(stage.getFirstDepartureTime().asLocalTime().isBefore(LocalTime.of(22,55)));
    }

    @Test
    public void shouldMapSimpleJourney() throws TramchesterException {
        LocalTime am7 = LocalTime.of(7,0); //7 * 60;

        RawVehicleStage altToCorn = getRawVehicleStage(Stations.Altrincham, Stations.Cornbrook, "route name", am7, 42, when);

        stages.add(altToCorn);
        Optional<Journey> result = mapper.createJourney(new RawJourney(stages,am7), 30);

        assertTrue(result.isPresent());
        Journey journey = result.get();
        assertEquals(1, journey.getStages().size());
        TransportStage stage = journey.getStages().get(0);
        assertEquals(Stations.Altrincham.getId(),stage.getFirstStation().getId());
        assertEquals(Stations.Cornbrook.getId(),stage.getLastStation().getId());
        assertTrue(stage.getDuration()>0);
        assertTrue(stage.getFirstDepartureTime().asLocalTime().isAfter(sevenAM));
        assertTrue(stage.getFirstDepartureTime().asLocalTime().isBefore(eightAM));
        assertTrue(stage.getExpectedArrivalTime().asLocalTime().isAfter(sevenAM));
        assertTrue(stage.getExpectedArrivalTime().asLocalTime().isBefore(eightAM));
        assertTrue(stage.getBoardingPlatform().isPresent());
        assertEquals(Stations.Altrincham.getId()+"1", stage.getBoardingPlatform().get().getId());
    }

    @Test
    public void shouldMapTwoStageJourney() throws TramchesterException {
        LocalTime am10 = LocalTime.of(10,0);
        Location begin = Stations.Altrincham;
        Location middle = Stations.Cornbrook;
        Location end = Stations.ManAirport;

        RawVehicleStage rawStageA = getRawVehicleStage(begin, middle, "route text", am10, 42, when);
        RawVehicleStage rawStageB = getRawVehicleStage(middle, end, "route2 text", am10.plusMinutes(42), 20, when);
        stages.add(rawStageA);
        stages.add(rawStageB);

        Optional<Journey> result = mapper.createJourney(new RawJourney(stages, am10), 30);

        assertTrue(result.isPresent());
        Journey journey = result.get();

        assertEquals(2, journey.getStages().size());

        TransportStage stage1 = journey.getStages().get(0);
        assertEquals(begin.getId(),stage1.getFirstStation().getId());
        assertEquals(middle.getId(),stage1.getLastStation().getId());
        assertTrue(stage1.getBoardingPlatform().isPresent());
        assertEquals(begin.getId()+"1", stage1.getBoardingPlatform().get().getId());

        TransportStage stage2 = journey.getStages().get(1);
        assertEquals(middle.getId(),stage2.getFirstStation().getId());
        assertEquals(end.getId(),stage2.getLastStation().getId());
        assertTrue(stage2.getBoardingPlatform().isPresent());
        assertEquals(middle.getId()+"1", stage2.getBoardingPlatform().get().getId());

        assertEquals("Change tram at", stage2.getPrompt());
    }

    @Test
    public void shouldMapWalkingStageJourney() {
        LocalTime pm10 = LocalTime.of(22,0);

        RawWalkingStage walkingStage = new RawWalkingStage(Stations.Deansgate, Stations.MarketStreet, 10);
        stages.add(walkingStage);

        Optional<Journey> result = mapper.createJourney(new RawJourney(stages,pm10), 30);

        assertTrue(result.isPresent());
        Journey journey = result.get();
        assertEquals(1, journey.getStages().size());

        TransportStage stage = journey.getStages().get(0);
        assertEquals(Stations.Deansgate.getId(),stage.getFirstStation().getId());
        assertEquals(Stations.MarketStreet.getId(),stage.getLastStation().getId());

        assertEquals("Walk to",stage.getPrompt());
    }

    @Test
    public void shouldMapThreeStageJourneyWithWalk() throws TramchesterException {
        LocalTime am10 = LocalTime.of(10,0);
        Location begin = Stations.Altrincham;
        Location middleA = Stations.Deansgate;
        Location middleB = Stations.MarketStreet;
        Location end = Stations.Bury;

        RawVehicleStage rawStageA = getRawVehicleStage(begin, middleA, "route text", am10, 42, when);
        int walkCost = 10;
        RawWalkingStage walkingStage = new RawWalkingStage(middleA, middleB, walkCost);
        RawVehicleStage finalStage = getRawVehicleStage(middleB, end, "route3 text", am10, 42, when);

        stages.add(rawStageA);
        stages.add(walkingStage);
        stages.add(finalStage);

        Optional<Journey> result = mapper.createJourney(new RawJourney(stages,am10), 30);

        assertTrue(result.isPresent());
        Journey journey = result.get();
        assertEquals(3, journey.getStages().size());

        TransportStage stage1 = journey.getStages().get(0);
        assertEquals("Board tram at",stage1.getPrompt());
        assertTrue(stage1.getBoardingPlatform().isPresent());
        assertEquals(begin.getId()+"1", stage1.getBoardingPlatform().get().getId());

        TransportStage stage2 = journey.getStages().get(1);
        assertEquals(middleB.getId(),stage2.getActionStation().getId());
        assertEquals(middleB.getId(),stage2.getLastStation().getId());
        assertEquals("Walk to",stage2.getPrompt());
        assertEquals(walkCost, stage2.getDuration());
        assertFalse(stage2.getBoardingPlatform().isPresent());

        TransportStage stage3 = journey.getStages().get(2);
        assertEquals("Board tram at",stage3.getPrompt());
        assertEquals(middleB.getId(),stage3.getFirstStation().getId());
        assertEquals(end.getId(),stage3.getLastStation().getId());
        assertTrue(stage3.getBoardingPlatform().isPresent());
        assertEquals(middleB.getId()+"1", stage3.getBoardingPlatform().get().getId());

        TramTime arrivalTime = stage3.getExpectedArrivalTime();
        assertTrue(arrivalTime.asLocalTime().isAfter(LocalTime.of(10,10)));

    }

    @Test
    public void shouldMapEndOfDayJourneyCorrectly() throws TramchesterException {
        LocalTime startTime = LocalTime.of(22,50);
        Location start = Stations.Altrincham;
        Location middle = Stations.TraffordBar;
        Location finish = Stations.ManAirport;

        RawVehicleStage rawStageA = getRawVehicleStage(start, middle, "route text", startTime, 18, when);
        RawVehicleStage rawStageB = getRawVehicleStage(middle, finish, "route2 text", startTime.plusMinutes(18), 42, when);

        stages.add(rawStageA);
        stages.add(rawStageB);

        Optional<Journey> result = mapper.createJourney(new RawJourney(stages,startTime), 30);

        assertTrue(result.isPresent());
    }

    private RawVehicleStage getRawVehicleStage(Location start, Location finish, String routeName, LocalTime startTime,
                                               int cost, LocalDate when) throws TramchesterException {

        String svcId = findServiceId(start.getId(), finish.getId(), when, startTime);
        RawVehicleStage rawVehicleStage = new RawVehicleStage(start, routeName, TransportMode.Tram, "cssClass");
        rawVehicleStage.setCost(cost);
        rawVehicleStage.setLastStation(finish);
        rawVehicleStage.setServiceId(svcId);
        rawVehicleStage.setPlatform(new Platform(start.getId()+"1", "platform name"));
        return rawVehicleStage;
    }

}
