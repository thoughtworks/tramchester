package com.tramchester.unit.mappers;


import com.tramchester.domain.*;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.*;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.StationRefDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.domain.presentation.*;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.ConnectingStage;
import com.tramchester.domain.transportStages.VehicleStage;
import com.tramchester.domain.transportStages.WalkingFromStationStage;
import com.tramchester.domain.transportStages.WalkingToStationStage;
import com.tramchester.mappers.JourneyToDTOMapper;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.BusStations.StopAtAltrinchamInterchange;
import static com.tramchester.testSupport.reference.TramStations.*;
import static com.tramchester.testSupport.reference.TramTransportDataForTestFactory.TramTransportDataForTest.TRIP_A_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JourneyToDTOMapperTest extends EasyMockSupport {
    private static TramTransportDataForTestFactory.TramTransportDataForTest transportData;
    private final LocalDate when = TestEnv.testDay();

    private JourneyToDTOMapper mapper;
    private List<TransportStage<?,?>> stages;
    private TramServiceDate tramServiceDate;

    private StageDTOFactory stageFactory;
    private ProvidesNotes providesNotes;
    private List<Note> notes;
    private MyLocation nearPiccGardensLocation;
    private final int requestedNumberChanges = 5;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        ProvidesNow providesNow = new ProvidesLocalNow();
        TramTransportDataForTestFactory dataForTestProvider = new TramTransportDataForTestFactory(providesNow);
        dataForTestProvider.start();
        transportData = dataForTestProvider.getTestData();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stageFactory = createMock(StageDTOFactory.class);
        providesNotes = createMock(ProvidesNotes.class);

        notes = Collections.singletonList(new StationNote(Note.NoteType.Live, "someText", of(StPetersSquare)));

        mapper = new JourneyToDTOMapper(stageFactory, providesNotes);
        stages = new LinkedList<>();
        tramServiceDate = new TramServiceDate(when);
        nearPiccGardensLocation = new MyLocation(TestEnv.nearPiccGardens);
    }

    private Route createRoute(String name) {
        return MutableRoute.getRoute(StringIdFor.createId("routeId"), "shortName", name,
                TestEnv.MetAgency(), TransportMode.Tram);
    }

    @Test
    void shouldMapWalkingStageJourneyFromMyLocation() {
        TramTime pm10 = TramTime.of(22,0);

        WalkingToStationStage walkingStage = new WalkingToStationStage(nearPiccGardensLocation, of(MarketStreet), 10, pm10);
        stages.add(walkingStage);

        StageDTO stageDTOA = new StageDTO();
        EasyMock.expect(stageFactory.build(walkingStage, TravelAction.WalkTo, when)).andReturn(stageDTOA);

        final List<Location<?>> path = Collections.singletonList(of(Deansgate));
        Journey journey = new Journey(pm10.plusMinutes(5), pm10, pm10.plusMinutes(10), stages, path, requestedNumberChanges);
        EasyMock.expect(providesNotes.createNotesForJourney(journey, tramServiceDate)).andReturn(notes);

        replayAll();
        JourneyDTO result = mapper.createJourneyDTO(journey, tramServiceDate);
        verifyAll();

        assertEquals(journey.getArrivalTime().asLocalTime(), result.getExpectedArrivalTime().toLocalTime());
        assertEquals(journey.getDepartTime().asLocalTime(), result.getFirstDepartureTime().toLocalTime());
        assertEquals(journey.getBeginning().forDTO(), result.getBegin().getId());
        assertEquals(notes, result.getNotes());
        assertEquals(stageDTOA, result.getStages().get(0));
        assertEquals(when, result.getQueryDate());
        validateStationList(path, result.getPath());
        validateStationList(Collections.emptyList(), result.getChangeStations());
    }


    @Test
    void shouldMapWalkingStageJourneyToMyLocation() {
        TramTime pm10 = TramTime.of(22,0);

        WalkingFromStationStage walkingStage = new WalkingFromStationStage(of(Deansgate), nearPiccGardensLocation, 10, pm10);
        stages.add(walkingStage);

        StageDTO stageDTOA = new StageDTO();
        EasyMock.expect(stageFactory.build(walkingStage, TravelAction.WalkFrom, when)).andReturn(stageDTOA);

        Journey journey = new Journey(pm10.plusMinutes(5), pm10, pm10.plusMinutes(10), stages, Collections.singletonList(of(Deansgate)), requestedNumberChanges);
        EasyMock.expect(providesNotes.createNotesForJourney(journey, tramServiceDate)).andReturn(notes);

        replayAll();
        JourneyDTO result = mapper.createJourneyDTO(journey, tramServiceDate);
        verifyAll();

        assertEquals(journey.getArrivalTime().asLocalTime(), result.getExpectedArrivalTime().toLocalTime());
        assertEquals(journey.getDepartTime().asLocalTime(), result.getFirstDepartureTime().toLocalTime());
        assertEquals(journey.getBeginning().forDTO(), result.getBegin().getId());
        assertEquals(notes, result.getNotes());
        assertEquals(stageDTOA, result.getStages().get(0));
        assertEquals(when, result.getQueryDate());
        validateStationList(Collections.singletonList(of(Deansgate)), result.getPath());
        validateStationList(Collections.emptyList(), result.getChangeStations());
    }

    @Test
    void shouldMapJourneyWithConnectingStage() {
        TramTime time = TramTime.of(15,45);
        final MutableStation startStation = of(Altrincham);
        Platform platform = MutablePlatform.buildForTFGMTram(startStation.forDTO() + "1", "platform name", startStation.getLatLong());
        startStation.addPlatform(platform);

        ConnectingStage connectingStage = new ConnectingStage(
                BusStations.of(StopAtAltrinchamInterchange), startStation, 1, time);

        VehicleStage tramStage = getRawVehicleStage(startStation, TramStations.of(TramStations.Shudehill),
                createRoute("route"), time.plusMinutes(1), 35, platform);

        stages.add(connectingStage);
        stages.add(tramStage);

        StageDTO stageDTOA = new StageDTO();
        StageDTO stageDTOB = new StageDTO();

        EasyMock.expect(stageFactory.build(connectingStage, TravelAction.ConnectTo, when)).andReturn(stageDTOA);
        // TODO Should be board?
        EasyMock.expect(stageFactory.build(tramStage, TravelAction.Change, when)).andReturn(stageDTOB);

        final List<Location<?>> path = Collections.singletonList(of(Altrincham));
        Journey journey = new Journey(time.plusMinutes(5), time, time.plusMinutes(10), stages, path, requestedNumberChanges);
        EasyMock.expect(providesNotes.createNotesForJourney(journey, tramServiceDate)).andReturn(notes);

        replayAll();
        JourneyDTO result = mapper.createJourneyDTO(journey, tramServiceDate);
        verifyAll();

        assertEquals(journey.getArrivalTime().asLocalTime(), result.getExpectedArrivalTime().toLocalTime());
        assertEquals(journey.getDepartTime().asLocalTime(), result.getFirstDepartureTime().toLocalTime());
        assertEquals(journey.getBeginning().forDTO(), result.getBegin().getId());
        assertEquals(notes, result.getNotes());
        assertEquals(journey.getStages().size(), result.getStages().size());
        assertEquals(stageDTOA, result.getStages().get(0));
        assertEquals(stageDTOB, result.getStages().get(1));
        assertEquals(when, result.getQueryDate());
        validateStationList(path, result.getPath());
        validateStationList(Collections.singletonList(startStation), result.getChangeStations());
    }

    // TODO MAKE REALISTIC
    @Test
    void shouldMapThreeStageJourneyWithWalk() {
        TramTime am10 = TramTime.of(10,0);
        MutableStation begin = of(Altrincham);
        Platform platformA = MutablePlatform.buildForTFGMTram(begin.forDTO() + "1", "platform name", begin.getLatLong());
        begin.addPlatform(platformA);

        MyLocation middleA = nearPiccGardensLocation;
        MutableStation middleB = of(MarketStreet);
        Platform platformB = MutablePlatform.buildForTFGMTram(middleB.forDTO() + "1", "platform name", middleB.getLatLong());
        middleB.addPlatform(platformB);

        Station end = of(Bury);

        VehicleStage rawStageA = getRawVehicleStage(begin, of(PiccadillyGardens),
                createRoute("route text"), am10, 42, platformA);

        int walkCost = 10;
        WalkingToStationStage walkingStage = new WalkingToStationStage(middleA, middleB, walkCost, am10);
        VehicleStage finalStage = getRawVehicleStage(middleB, end, createRoute("route3 text"), am10, 42,
                platformA);

        stages.add(rawStageA);
        stages.add(walkingStage);
        stages.add(finalStage);

        StageDTO stageDTOA = new StageDTO();
        StageDTO stageDTOB = new StageDTO();
        StageDTO stageDTOC = new StageDTO();

        EasyMock.expect(stageFactory.build(rawStageA, TravelAction.Board, when)).andReturn(stageDTOA);
        EasyMock.expect(stageFactory.build(walkingStage, TravelAction.WalkTo, when)).andReturn(stageDTOB);
        // TODO Ought to be Board?
        EasyMock.expect(stageFactory.build(finalStage, TravelAction.Change, when)).andReturn(stageDTOC);

        final List<Location<?>> path = Collections.singletonList(begin);
        Journey journey = new Journey(am10.plusMinutes(5), am10, am10.plusMinutes(10), stages, path, requestedNumberChanges);
        EasyMock.expect(providesNotes.createNotesForJourney(journey, tramServiceDate)).andReturn(notes);

        replayAll();
        JourneyDTO result = mapper.createJourneyDTO(journey, tramServiceDate);
        verifyAll();

        assertEquals(journey.getArrivalTime().asLocalTime(), result.getExpectedArrivalTime().toLocalTime());
        assertEquals(journey.getDepartTime().asLocalTime(), result.getFirstDepartureTime().toLocalTime());
        assertEquals(journey.getBeginning().forDTO(), result.getBegin().getId());
        assertEquals(notes, result.getNotes());
        assertEquals(journey.getStages().size(), result.getStages().size());
        assertEquals(stageDTOA, result.getStages().get(0));
        assertEquals(stageDTOB, result.getStages().get(1));
        assertEquals(stageDTOC, result.getStages().get(2));
        assertEquals(when, result.getQueryDate());
        validateStationList(path, result.getPath());
        validateStationList(Arrays.asList( middleA, middleB ), result.getChangeStations());
    }

    @Test
    void shouldMap2StageJoruneyWithChange() {
        TramTime startTime = TramTime.of(22,50);
        Station start = transportData.getFirst();
        Station middle = transportData.getSecond();
        Station finish = transportData.getInterchange();
        Platform platform = MutablePlatform.buildForTFGMTram(start.forDTO() + "1", "platform name", start.getLatLong());

        VehicleStage rawStageA = getRawVehicleStage(start, middle, createRoute("route text"), startTime,
                18, platform);
        VehicleStage rawStageB = getRawVehicleStage(middle, finish, createRoute("route2 text"), startTime.plusMinutes(18),
                42, platform);

        stages.add(rawStageA);
        stages.add(rawStageB);

        StageDTO stageDTOA = new StageDTO();
        StageDTO stageDTOB = new StageDTO();

        EasyMock.expect(stageFactory.build(rawStageA, TravelAction.Board, when)).andReturn(stageDTOA);
        EasyMock.expect(stageFactory.build(rawStageB, TravelAction.Change, when)).andReturn(stageDTOB);

        final List<Location<?>> path = Collections.singletonList(start);
        Journey journey = new Journey(startTime.plusMinutes(5), startTime, startTime.plusMinutes(10), stages, path, requestedNumberChanges);
        EasyMock.expect(providesNotes.createNotesForJourney(journey, tramServiceDate)).andReturn(notes);

        replayAll();
        JourneyDTO result = mapper.createJourneyDTO(journey, tramServiceDate);
        verifyAll();

        assertEquals(journey.getArrivalTime().asLocalTime(), result.getExpectedArrivalTime().toLocalTime());
        assertEquals(journey.getDepartTime().asLocalTime(), result.getFirstDepartureTime().toLocalTime());
        assertEquals(journey.getBeginning().forDTO(), result.getBegin().getId());
        assertEquals(notes, result.getNotes());
        assertEquals(journey.getStages().size(), result.getStages().size());
        assertEquals(stageDTOA, result.getStages().get(0));
        assertEquals(stageDTOB, result.getStages().get(1));
        assertEquals(when, result.getQueryDate());
        validateStationList(path, result.getPath());
        validateStationList(Collections.singletonList(middle), result.getChangeStations());
    }

    private VehicleStage getRawVehicleStage(Station start, Station finish, Route route, TramTime startTime,
                                            int cost, Platform platform) {


        Trip validTrip = transportData.getTripById(StringIdFor.createId(TRIP_A_ID));

        List<Integer> passedStations = new ArrayList<>();
        VehicleStage vehicleStage = new VehicleStage(start, route, TransportMode.Tram, validTrip,
                startTime.plusMinutes(1), finish, passedStations);

        vehicleStage.setCost(cost);
        vehicleStage.setPlatform(platform);

        return vehicleStage;

    }

    private void validateStationList(List<Location<?>> expected, List<StationRefWithPosition> results) {
        assertEquals(expected.size(), results.size());
        List<String> expectedIds = expected.stream().map(IdForDTO::forDTO).collect(Collectors.toList());
        List<String> resultIds = results.stream().map(StationRefDTO::getId).collect(Collectors.toList());
        assertEquals(expectedIds, resultIds);
    }

}
