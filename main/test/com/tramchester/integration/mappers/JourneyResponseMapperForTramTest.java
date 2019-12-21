package com.tramchester.integration.mappers;


import com.tramchester.Dependencies;
import com.tramchester.TestConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import com.tramchester.mappers.SingleJourneyMapper;
import com.tramchester.repository.TransportDataFromFiles;
import org.junit.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JourneyResponseMapperForTramTest extends JourneyResponseMapperTest {
    private static boolean edgePerTrip;
    private static TransportDataFromFiles transportData;
    private static GraphDatabaseService database;
    private final LocalDate when = TestConfig.nextTuesday(0);
    private LocalTime sevenAM;
    private LocalTime eightAM;

    private static Dependencies dependencies;
    private SingleJourneyMapper mapper;
    private List<RawStage> stages;
    private Transaction tx;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
        transportData = dependencies.get(TransportDataFromFiles.class);
        edgePerTrip = testConfig.getEdgePerTrip();
        database = dependencies.get(GraphDatabaseService.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Before
    public void beforeEachTestRuns() {
        tx = database.beginTx(180, TimeUnit.SECONDS);

        mapper = dependencies.get(SingleJourneyMapper.class);
        routeCalculator = dependencies.get(RouteCalculator.class);
        stages = new LinkedList<>();
        sevenAM = LocalTime.of(7, 0);
        eightAM = LocalTime.of(8, 0);
    }

    @After
    public void onceAfterEachTestRuns() {
        tx.close();
    }

    @Test
    public void shouldEnsureTripsAreOrderByEarliestFirst() {
        TramTime time = TramTime.of(15,30);

        RawVehicleStage vicToRoch = getRawVehicleStage(Stations.Victoria, Stations.Rochdale, "routeText", time, 42, when, 16);
        stages.add(vicToRoch);

        Optional<Journey> result = mapper.createJourney(new RawJourney(stages, time), 30);

        assertTrue(result.isPresent());
        Journey journey = result.get();
        TransportStage stage = journey.getStages().get(0);
        // for this service trips later in the list actually depart earlier, so this would fail
        assertTrue(stage.getFirstDepartureTime().asLocalTime().isBefore(LocalTime.of(16,0)));
    }

    @Test
    public void shouldEnsureTripsAreOrderByEarliestFirstSpanningMidnightService() {
        TramTime pm1044  = TramTime.of(22,44);

        RawVehicleStage rawStage = getRawVehicleStage(Stations.ManAirport, Stations.Cornbrook, "routename",
                pm1044, 42, when, 14);

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
    public void shouldMapSimpleJourney() {
        TramTime am7 = TramTime.of(7,0);

        RawVehicleStage altToCorn = getRawVehicleStage(Stations.Altrincham, Stations.Cornbrook, "route name", am7, 42, when, 8);

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
        assertEquals(8, stage.getPassedStops());
        assertEquals(Stations.Altrincham.getId()+"1", stage.getBoardingPlatform().get().getId());
    }

    @Test
    public void shouldMapTwoStageJourney() {
        TramTime am10 = TramTime.of(10,0);
        Location begin = Stations.Altrincham;
        Location middle = Stations.Cornbrook;
        Location end = Stations.ManAirport;

        RawVehicleStage rawStageA = getRawVehicleStage(begin, middle, "route text", am10, 42, when, 8);
        RawVehicleStage rawStageB = getRawVehicleStage(middle, end, "route2 text", am10.plusMinutes(42), 20, when, 8);
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

    }

    @Test
    public void shouldMapWalkingStageJourney() {
        TramTime pm10 = TramTime.of(22,0);

        RawWalkingStage walkingStage = new RawWalkingStage(Stations.Deansgate, Stations.MarketStreet, 10);
        stages.add(walkingStage);

        Optional<Journey> result = mapper.createJourney(new RawJourney(stages,pm10), 30);

        assertTrue(result.isPresent());
        Journey journey = result.get();
        assertEquals(1, journey.getStages().size());

        TransportStage stage = journey.getStages().get(0);
        assertEquals(Stations.Deansgate.getId(),stage.getFirstStation().getId());
        assertEquals(Stations.MarketStreet.getId(),stage.getLastStation().getId());

    }

    @Test
    public void shouldMapThreeStageJourneyWithWalk() {
        TramTime am10 = TramTime.of(10,0);
        Location begin = Stations.Altrincham;
        Location middleA = Stations.Deansgate;
        Location middleB = Stations.MarketStreet;
        Location end = Stations.Bury;

        RawVehicleStage rawStageA = getRawVehicleStage(begin, middleA, "route text", am10, 42, when, 8);
        int walkCost = 10;
        RawWalkingStage walkingStage = new RawWalkingStage(middleA, middleB, walkCost);
        RawVehicleStage finalStage = getRawVehicleStage(middleB, end, "route3 text", am10, 42, when, 9);

        stages.add(rawStageA);
        stages.add(walkingStage);
        stages.add(finalStage);

        Optional<Journey> result = mapper.createJourney(new RawJourney(stages,am10), 30);

        assertTrue(result.isPresent());
        Journey journey = result.get();
        assertEquals(3, journey.getStages().size());

        TransportStage stage1 = journey.getStages().get(0);
        assertTrue(stage1.getBoardingPlatform().isPresent());
        assertEquals(begin.getId()+"1", stage1.getBoardingPlatform().get().getId());

        TransportStage stage2 = journey.getStages().get(1);
        assertEquals(middleB.getId(),stage2.getActionStation().getId());
        assertEquals(middleB.getId(),stage2.getLastStation().getId());
        assertEquals(walkCost, stage2.getDuration());
        assertFalse(stage2.getBoardingPlatform().isPresent());

        TransportStage stage3 = journey.getStages().get(2);
        assertEquals(middleB.getId(),stage3.getFirstStation().getId());
        assertEquals(end.getId(),stage3.getLastStation().getId());
        assertTrue(stage3.getBoardingPlatform().isPresent());
        assertEquals(middleB.getId()+"1", stage3.getBoardingPlatform().get().getId());

        TramTime arrivalTime = stage3.getExpectedArrivalTime();
        assertTrue(arrivalTime.asLocalTime().isAfter(LocalTime.of(10,10)));

    }

    @Test
    public void shouldMapEndOfDayJourneyCorrectly() {
        TramTime startTime = TramTime.of(22,50);
        Location start = Stations.Altrincham;
        Location middle = Stations.TraffordBar;
        Location finish = Stations.ManAirport;

        RawVehicleStage rawStageA = getRawVehicleStage(start, middle, "route text", startTime, 18, when, 8);
        RawVehicleStage rawStageB = getRawVehicleStage(middle, finish, "route2 text", startTime.plusMinutes(18), 42, when, 9);

        stages.add(rawStageA);
        stages.add(rawStageB);

        Optional<Journey> result = mapper.createJourney(new RawJourney(stages,startTime), 30);

        assertTrue(result.isPresent());
    }

    private RawVehicleStage getRawVehicleStage(Location start, Location finish, String routeName, TramTime startTime,
                                               int cost, LocalDate when, int passedStops) {

        RawVehicleStage rawVehicleStage = new RawVehicleStage(start, routeName, TransportMode.Tram, "cssClass");
        rawVehicleStage.setCost(cost);
        rawVehicleStage.setLastStation(finish, passedStops);
        rawVehicleStage.setPlatform(new Platform(start.getId() + "1", "platform name"));

        if (!edgePerTrip) {
            String svcId = findServiceId(start.getId(), finish.getId(), when, startTime);

            rawVehicleStage.setServiceId(svcId);
        } else {

            Trip validTrip = transportData.getTripsFor(start.getId()).iterator().next();
            rawVehicleStage.setServiceId(validTrip.getServiceId());
            rawVehicleStage.setTripId(validTrip.getTripId());
            rawVehicleStage.setDepartTime(startTime.plusMinutes(1));

        }
        return rawVehicleStage;

    }

}
