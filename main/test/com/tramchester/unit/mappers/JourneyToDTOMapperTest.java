package com.tramchester.unit.mappers;


import com.tramchester.domain.*;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.domain.presentation.DTO.factory.JourneyDTOFactory;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.domain.presentation.*;
import com.tramchester.domain.reference.RouteDirection;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.mappers.JourneyToDTOMapper;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.reference.TramTransportDataForTestProvider;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static com.tramchester.testSupport.reference.BusStations.AltrinchamInterchange;
import static com.tramchester.testSupport.reference.TramStations.*;
import static com.tramchester.testSupport.reference.TramTransportDataForTestProvider.TestTransportData.TRIP_A_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JourneyToDTOMapperTest extends EasyMockSupport {
    private static TramTransportDataForTestProvider.TestTransportData transportData;
    private final LocalDate when = TestEnv.testDay();

    private JourneyToDTOMapper mapper;
    private List<TransportStage<?,?>> stages;
    private TramServiceDate tramServiceDate;
    private JourneyDTOFactory journeyFactory;
    private StageDTOFactory stageFactory;
    private ProvidesNotes providesNotes;
    private List<Note> notes;
    private MyLocation nearPiccGardensLocation;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        //StationLocations stationLocations = new StationLocations();
        ProvidesNow providesNow = new ProvidesLocalNow();
        transportData = new TramTransportDataForTestProvider(providesNow).getTestData();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        journeyFactory = createMock(JourneyDTOFactory.class);
        stageFactory = createMock(StageDTOFactory.class);
        providesNotes = createMock(ProvidesNotes.class);

        notes = Collections.singletonList(new StationNote(Note.NoteType.Live, "someText", of(StPetersSquare)));

        mapper = new JourneyToDTOMapper(journeyFactory, stageFactory, providesNotes);
        stages = new LinkedList<>();
        tramServiceDate = new TramServiceDate(when);
        nearPiccGardensLocation = new MyLocation("Manchester", TestEnv.nearPiccGardens);
    }

    private Route createRoute(String name) {
        return new Route("routeId", "shortName", name, TestEnv.MetAgency(), TransportMode.Tram, RouteDirection.Inbound);
    }

    @Test
    void shouldMapWalkingStageJourneyFromMyLocation() {
        TramTime pm10 = TramTime.of(22,0);

        WalkingToStationStage walkingStage = new WalkingToStationStage(nearPiccGardensLocation, of(MarketStreet), 10, pm10);
        stages.add(walkingStage);

        StageDTO stageDTOA = new StageDTO();
        EasyMock.expect(stageFactory.build(walkingStage, TravelAction.WalkTo, when)).andReturn(stageDTOA);

        JourneyDTO journeyDTO = new JourneyDTO();
        List<StationRefWithPosition> refWithPositions = Collections.singletonList(new StationRefWithPosition(of(Deansgate)));
        EasyMock.expect(journeyFactory.build(Collections.singletonList(stageDTOA), pm10, notes, refWithPositions, when)).andReturn(journeyDTO);

        Journey journey = new Journey(stages, pm10, Collections.singletonList(of(Deansgate)));
        EasyMock.expect(providesNotes.createNotesForJourney(journey, tramServiceDate)).andReturn(notes);

        replayAll();
        JourneyDTO result = mapper.createJourneyDTO(journey, tramServiceDate);
        verifyAll();

        assertEquals(journeyDTO, result);
    }

    @Test
    void shouldMapWalkingStageJourneyToMyLocation() {
        TramTime pm10 = TramTime.of(22,0);

        WalkingFromStationStage walkingStage = new WalkingFromStationStage(of(Deansgate), nearPiccGardensLocation, 10, pm10);
        stages.add(walkingStage);

        StageDTO stageDTOA = new StageDTO();
        EasyMock.expect(stageFactory.build(walkingStage, TravelAction.WalkFrom, when)).andReturn(stageDTOA);

        JourneyDTO journeyDTO = new JourneyDTO();
        List<StationRefWithPosition> refWithPositions = Collections.singletonList(new StationRefWithPosition(of(Deansgate)));
        EasyMock.expect(journeyFactory.build(Collections.singletonList(stageDTOA), pm10, notes, refWithPositions, when)).andReturn(journeyDTO);

        Journey journey = new Journey(stages, pm10, Collections.singletonList(of(Deansgate)));
        EasyMock.expect(providesNotes.createNotesForJourney(journey, tramServiceDate)).andReturn(notes);

        replayAll();
        JourneyDTO result = mapper.createJourneyDTO(journey, tramServiceDate);
        verifyAll();

        assertEquals(journeyDTO, result);
    }

    @Test
    void shouldMapJoruneyWithConnectingStage() {
        TramTime time = TramTime.of(15,45);
        ConnectingStage connectingStage = new ConnectingStage(
                BusStations.of(AltrinchamInterchange), TramStations.of(Altrincham), 1, time);
        VehicleStage tramStage = getRawVehicleStage(TramStations.of(Altrincham), TramStations.of(TramStations.Shudehill),
                createRoute("route"), time.plusMinutes(1), 35, 9, true);

        stages.add(connectingStage);
        stages.add(tramStage);

        StageDTO stageDTOA = new StageDTO();
        StageDTO stageDTOB = new StageDTO();
        List<StageDTO> dtoStages = Arrays.asList(stageDTOA, stageDTOB);

        EasyMock.expect(stageFactory.build(connectingStage, TravelAction.ConnectTo, when)).andReturn(stageDTOA);

        // TODO Should be board
        EasyMock.expect(stageFactory.build(tramStage, TravelAction.Change, when)).andReturn(stageDTOB);

        JourneyDTO journeyDTO = new JourneyDTO();
        List<StationRefWithPosition> refWithPositions = Collections.singletonList(new StationRefWithPosition(of(Altrincham)));

        EasyMock.expect(journeyFactory.build(dtoStages,time, notes, refWithPositions, when)).andReturn(journeyDTO);

        Journey journey = new Journey(stages, time, Collections.singletonList(of(Altrincham)));
        EasyMock.expect(providesNotes.createNotesForJourney(journey, tramServiceDate)).andReturn(notes);

        replayAll();
        JourneyDTO result = mapper.createJourneyDTO(journey, tramServiceDate);
        verifyAll();

        assertEquals(journeyDTO, result);
    }

    // TODO MAKE REALISTIC
    @Test
    void shouldMapThreeStageJourneyWithWalk() {
        TramTime am10 = TramTime.of(10,0);
        Station begin = of(Altrincham);
        MyLocation middleA = nearPiccGardensLocation;
        Station middleB = of(MarketStreet);
        Station end = of(Bury);

        VehicleStage rawStageA = getRawVehicleStage(begin, of(PiccadillyGardens),
                createRoute("route text"), am10, 42, 8, true);

        int walkCost = 10;
        WalkingToStationStage walkingStage = new WalkingToStationStage(middleA, middleB, walkCost, am10);
        VehicleStage finalStage = getRawVehicleStage(middleB, end, createRoute("route3 text"), am10, 42,
                9, true);

        stages.add(rawStageA);
        stages.add(walkingStage);
        stages.add(finalStage);

        StageDTO stageDTOA = new StageDTO();
        StageDTO stageDTOB = new StageDTO();
        StageDTO stageDTOC = new StageDTO();

        List<StageDTO> dtoStages = Arrays.asList(stageDTOA, stageDTOB, stageDTOC);

        EasyMock.expect(stageFactory.build(rawStageA, TravelAction.Board, when)).andReturn(stageDTOA);
        EasyMock.expect(stageFactory.build(walkingStage, TravelAction.WalkTo, when)).andReturn(stageDTOB);
        // TODO Ought to be Board?
        EasyMock.expect(stageFactory.build(finalStage, TravelAction.Change, when)).andReturn(stageDTOC);

        JourneyDTO journeyDTO = new JourneyDTO();
        List<StationRefWithPosition> refWithPositions = Collections.singletonList(new StationRefWithPosition(begin));

        EasyMock.expect(journeyFactory.build(dtoStages, am10, notes, refWithPositions, when)).andReturn(journeyDTO);

        Journey journey = new Journey(stages, am10, Collections.singletonList(begin));
        EasyMock.expect(providesNotes.createNotesForJourney(journey, tramServiceDate)).andReturn(notes);

        replayAll();
        JourneyDTO result = mapper.createJourneyDTO(journey, tramServiceDate);
        verifyAll();

        assertEquals(journeyDTO, result);
    }

    @Test
    void shouldMap2StageJoruneyWithChange() {
        TramTime startTime = TramTime.of(22,50);
        Station start = transportData.getFirst();
        Station middle = transportData.getSecond();
        Station finish = transportData.getInterchange();

        VehicleStage rawStageA = getRawVehicleStage(start, middle, createRoute("route text"), startTime,
                18, 8, true);
        VehicleStage rawStageB = getRawVehicleStage(middle, finish, createRoute("route2 text"), startTime.plusMinutes(18),
                42, 9, true);

        stages.add(rawStageA);
        stages.add(rawStageB);

        StageDTO stageDTOA = new StageDTO();
        StageDTO stageDTOB = new StageDTO();
        List<StageDTO> dtoStages = Arrays.asList(stageDTOA, stageDTOB);

        EasyMock.expect(stageFactory.build(rawStageA, TravelAction.Board, when)).andReturn(stageDTOA);
        EasyMock.expect(stageFactory.build(rawStageB, TravelAction.Change, when)).andReturn(stageDTOB);

        JourneyDTO journeyDTO = new JourneyDTO();
        List<StationRefWithPosition> refWithPositions = Collections.singletonList(new StationRefWithPosition(start));
        EasyMock.expect(journeyFactory.build(dtoStages, startTime, notes, refWithPositions, when)).andReturn(journeyDTO);

        Journey journey = new Journey(stages, startTime, Collections.singletonList(start));
        EasyMock.expect(providesNotes.createNotesForJourney(journey, tramServiceDate)).andReturn(notes);

        replayAll();
        JourneyDTO result = mapper.createJourneyDTO(journey, tramServiceDate);
        verifyAll();

        assertEquals(journeyDTO, result);
    }

    private VehicleStage getRawVehicleStage(Station start, Station finish, Route route, TramTime startTime,
                                            int cost, int passedStops, boolean hasPlatforms) {

        Trip validTrip = transportData.getTripById(StringIdFor.createId(TRIP_A_ID));

        List<Integer> passedStations = new ArrayList<>();
        VehicleStage vehicleStage = new VehicleStage(start, route, TransportMode.Tram, validTrip,
                startTime.plusMinutes(1), finish, passedStations, hasPlatforms);

        vehicleStage.setCost(cost);
        Platform platform = new Platform(start.forDTO() + "1", "platform name", start.getLatLong());
        vehicleStage.setPlatform(platform);

        return vehicleStage;

    }

}
