package com.tramchester.unit.mappers;


import com.tramchester.domain.*;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.*;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.factory.JourneyDTOFactory;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.StationLocations;
import com.tramchester.mappers.TramJourneyToDTOMapper;
import com.tramchester.testSupport.BusStations;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.unit.graph.TransportDataForTest;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TramJourneyToDTOMapperTest extends EasyMockSupport {
    private static TransportDataForTest transportData;
    private final LocalDate when = TestEnv.testDay();

    private TramJourneyToDTOMapper mapper;
    private List<TransportStage> stages;
    private TramServiceDate tramServiceDate;
    private JourneyDTOFactory journeyFactory;
    private StageDTOFactory stageFactory;
    private ProvidesNotes providesNotes;
    private List<Note> notes;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        StationLocations stationLocations = new StationLocations(new CoordinateTransforms());
        transportData = new TransportDataForTest(stationLocations);
        transportData.start();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        journeyFactory = createMock(JourneyDTOFactory.class);
        stageFactory = createMock(StageDTOFactory.class);
        providesNotes = createMock(ProvidesNotes.class);

        notes = Collections.singletonList(new StationNote(Note.NoteType.Live, "someText", Stations.StPetersSquare));

        mapper = new TramJourneyToDTOMapper(journeyFactory, stageFactory, providesNotes);
        stages = new LinkedList<>();
        tramServiceDate = new TramServiceDate(when);
    }

    private Route createRoute(String name) {
        return new Route("routeId", "shortName", name, TestEnv.MetAgency(), TransportMode.Tram);
    }

    @Test
    void shouldMapWalkingStageJourneyFromMyLocation() {
        TramTime pm10 = TramTime.of(22,0);

        boolean towardsMyLocation = false;
        WalkingStage walkingStage = new WalkingStage(Stations.Deansgate, Stations.MarketStreet, 10, pm10, towardsMyLocation);
        stages.add(walkingStage);

        StageDTO stageDTOA = new StageDTO();
        EasyMock.expect(stageFactory.build(walkingStage, TravelAction.WalkTo, pm10, tramServiceDate)).andReturn(stageDTOA);

        JourneyDTO journeyDTO = new JourneyDTO();
        EasyMock.expect(journeyFactory.build(Collections.singletonList(stageDTOA), pm10, notes)).andReturn(journeyDTO);

        Journey journey = new Journey(stages, pm10);
        EasyMock.expect(providesNotes.createNotesForJourney(journey, tramServiceDate)).andReturn(notes);

        replayAll();
        JourneyDTO result = mapper.createJourneyDTO(journey, tramServiceDate);
        verifyAll();

        assertEquals(journeyDTO, result);
    }

    @Test
    void shouldMapWalkingStageJourneyToMyLocation() {
        TramTime pm10 = TramTime.of(22,0);

        boolean towardsMyLocation = true;
        WalkingStage walkingStage = new WalkingStage(Stations.Deansgate, Stations.MarketStreet, 10, pm10, towardsMyLocation);
        stages.add(walkingStage);

        StageDTO stageDTOA = new StageDTO();
        EasyMock.expect(stageFactory.build(walkingStage, TravelAction.WalkFrom, pm10, tramServiceDate)).andReturn(stageDTOA);

        JourneyDTO journeyDTO = new JourneyDTO();
        EasyMock.expect(journeyFactory.build(Collections.singletonList(stageDTOA), pm10, notes)).andReturn(journeyDTO);

        Journey journey = new Journey(stages, pm10);
        EasyMock.expect(providesNotes.createNotesForJourney(journey, tramServiceDate)).andReturn(notes);

        replayAll();
        JourneyDTO result = mapper.createJourneyDTO(journey, tramServiceDate);
        verifyAll();

        assertEquals(journeyDTO, result);
    }

    @Test
    void shouldMapJoruneyWithConnectingStage() {
        TramTime time = TramTime.of(15,45);
        ConnectingStage connectingStage = new ConnectingStage(BusStations.AltrinchamInterchange, Stations.Altrincham, 1, time);
        VehicleStage tramStage = getRawVehicleStage(Stations.Altrincham, Stations.Shudehill, createRoute("route"), time.plusMinutes(1), 35, 9);

        stages.add(connectingStage);
        stages.add(tramStage);

        StageDTO stageDTOA = new StageDTO();
        StageDTO stageDTOB = new StageDTO();
        List<StageDTO> dtoStages = Arrays.asList(stageDTOA, stageDTOB);

        EasyMock.expect(stageFactory.build(connectingStage, TravelAction.ConnectTo, time, tramServiceDate)).andReturn(stageDTOA);

        // TODO Should be board
        EasyMock.expect(stageFactory.build(tramStage, TravelAction.Change, time, tramServiceDate)).andReturn(stageDTOB);

        JourneyDTO journeyDTO = new JourneyDTO();
        EasyMock.expect(journeyFactory.build(dtoStages,time, notes)).andReturn(journeyDTO);

        Journey journey = new Journey(stages, time);
        EasyMock.expect(providesNotes.createNotesForJourney(journey, tramServiceDate)).andReturn(notes);

        replayAll();
        JourneyDTO result = mapper.createJourneyDTO(journey, tramServiceDate);
        verifyAll();

        assertEquals(journeyDTO, result);
    }

    @Test
    void shouldMapThreeStageJourneyWithWalk() {
        TramTime am10 = TramTime.of(10,0);
        Location begin = Stations.Altrincham;
        Location middleA = Stations.Deansgate;
        Location middleB = Stations.MarketStreet;
        Location end = Stations.Bury;

        VehicleStage rawStageA = getRawVehicleStage(begin, middleA, createRoute("route text"), am10, 42, 8);
        int walkCost = 10;
        WalkingStage walkingStage = new WalkingStage(middleA, middleB, walkCost, am10, false);
        VehicleStage finalStage = getRawVehicleStage(middleB, end, createRoute("route3 text"), am10, 42, 9);

        stages.add(rawStageA);
        stages.add(walkingStage);
        stages.add(finalStage);

        StageDTO stageDTOA = new StageDTO();
        StageDTO stageDTOB = new StageDTO();
        StageDTO stageDTOC = new StageDTO();

        List<StageDTO> dtoStages = Arrays.asList(stageDTOA, stageDTOB, stageDTOC);

        EasyMock.expect(stageFactory.build(rawStageA, TravelAction.Board, am10, tramServiceDate)).andReturn(stageDTOA);
        EasyMock.expect(stageFactory.build(walkingStage, TravelAction.WalkTo, am10, tramServiceDate)).andReturn(stageDTOB);
        // TODO Ought to be Board?
        EasyMock.expect(stageFactory.build(finalStage, TravelAction.Change, am10, tramServiceDate)).andReturn(stageDTOC);

        JourneyDTO journeyDTO = new JourneyDTO();
        EasyMock.expect(journeyFactory.build(dtoStages, am10, notes)).andReturn(journeyDTO);

        Journey journey = new Journey(stages, am10);
        EasyMock.expect(providesNotes.createNotesForJourney(journey, tramServiceDate)).andReturn(notes);

        replayAll();
        JourneyDTO result = mapper.createJourneyDTO(journey, tramServiceDate);
        verifyAll();

        assertEquals(journeyDTO, result);
    }

    @Test
    void shouldMap2StageJoruneyWithChange() {
        TramTime startTime = TramTime.of(22,50);
        Location start = transportData.getFirst();
        Location middle = transportData.getSecond();
        Location finish = transportData.getInterchange();

        VehicleStage rawStageA = getRawVehicleStage(start, middle, createRoute("route text"), startTime,
                18, 8);
        VehicleStage rawStageB = getRawVehicleStage(middle, finish, createRoute("route2 text"), startTime.plusMinutes(18),
                42, 9);

        stages.add(rawStageA);
        stages.add(rawStageB);

        StageDTO stageDTOA = new StageDTO();
        StageDTO stageDTOB = new StageDTO();
        List<StageDTO> dtoStages = Arrays.asList(stageDTOA, stageDTOB);

        EasyMock.expect(stageFactory.build(rawStageA, TravelAction.Board, startTime, tramServiceDate)).andReturn(stageDTOA);
        EasyMock.expect(stageFactory.build(rawStageB, TravelAction.Change, startTime, tramServiceDate)).andReturn(stageDTOB);

        JourneyDTO journeyDTO = new JourneyDTO();
        EasyMock.expect(journeyFactory.build(dtoStages, startTime, notes)).andReturn(journeyDTO);

        Journey journey = new Journey(stages, startTime);
        EasyMock.expect(providesNotes.createNotesForJourney(journey, tramServiceDate)).andReturn(notes);

        replayAll();
        JourneyDTO result = mapper.createJourneyDTO(journey, tramServiceDate);
        verifyAll();

        assertEquals(journeyDTO, result);
    }

    private VehicleStage getRawVehicleStage(Location start, Location finish, Route route, TramTime startTime,
                                            int cost, int passedStops) {

        Trip validTrip = transportData.getTrip(TransportDataForTest.TRIP_A_ID);

        VehicleStage vehicleStage = new VehicleStage(start, route, TransportMode.Tram, "cssClass", validTrip,
                startTime.plusMinutes(1), finish, passedStops);

        vehicleStage.setCost(cost);
        vehicleStage.setPlatform(new Platform(start.getId() + "1", "platform name"));

        return vehicleStage;

    }

}
