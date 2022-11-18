package com.tramchester.unit.mappers;


import com.tramchester.domain.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.IdForDTO;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.presentation.StationNote;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.ConnectingStage;
import com.tramchester.domain.transportStages.VehicleStage;
import com.tramchester.domain.transportStages.WalkingFromStationStage;
import com.tramchester.domain.transportStages.WalkingToStationStage;
import com.tramchester.livedata.tfgm.ProvidesTramNotes;
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

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.BusStations.StopAtAltrinchamInterchange;
import static com.tramchester.testSupport.reference.KnownLocations.nearPiccGardens;
import static com.tramchester.testSupport.reference.TramStations.*;
import static com.tramchester.testSupport.reference.TramTransportDataForTestFactory.TramTransportDataForTest.TRIP_A_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JourneyToDTOMapperTest extends EasyMockSupport {
    private static TramTransportDataForTestFactory.TramTransportDataForTest transportData;
    private final TramDate when = TestEnv.testDay();

    private JourneyToDTOMapper mapper;
    private List<TransportStage<?,?>> stages;
    //private TramServiceDate tramServiceDate;

    private StageDTOFactory stageFactory;
    private ProvidesTramNotes providesNotes;
    private List<Note> notes;
    private MyLocation nearPiccGardensLocation;
    private final int requestedNumberChanges = 5;
    private DTOFactory DTOFactory;

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
        providesNotes = createMock(ProvidesTramNotes.class);

        DTOFactory = createMock(DTOFactory.class);

        LocationRefDTO stationRef = new LocationRefDTO(StPetersSquare.fake());
        notes = Collections.singletonList(new StationNote(Note.NoteType.Live, "someText", stationRef));

        mapper = new JourneyToDTOMapper(stageFactory, DTOFactory, providesNotes);
        stages = new LinkedList<>();
        //tramServiceDate = new TramServiceDate(when);
        nearPiccGardensLocation = nearPiccGardens.location();
    }

    private Route createRoute(String name) {
        return MutableRoute.getRoute(StringIdFor.createId("routeId"), "shortName", name,
                TestEnv.MetAgency(), TransportMode.Tram);
    }

    @Test
    void shouldMapWalkingStageJourneyFromMyLocation() {
        TramTime pm10 = TramTime.of(22,0);

        WalkingToStationStage walkingStage = new WalkingToStationStage(nearPiccGardensLocation, MarketStreet.fake(),
                Duration.ofMinutes(10), pm10);
        stages.add(walkingStage);

        StageDTO stageDTOA = new StageDTO();
        EasyMock.expect(stageFactory.build(walkingStage, TravelAction.WalkTo, when)).andReturn(stageDTOA);

        final List<Location<?>> path = Collections.singletonList(Deansgate.fake());
        Journey journey = new Journey(pm10.plusMinutes(5), pm10, pm10.plusMinutes(10), stages, path, requestedNumberChanges);
        EasyMock.expect(providesNotes.createNotesForJourney(journey, when)).andReturn(notes);

        EasyMock.expect(DTOFactory.createLocationRefWithPosition(nearPiccGardensLocation)).
                andReturn(new LocationRefWithPosition(nearPiccGardensLocation));
        EasyMock.expect(DTOFactory.createLocationRefWithPosition(Deansgate.fake())).
                andReturn(new LocationRefWithPosition(Deansgate.fake()));

        replayAll();
        JourneyDTO result = mapper.createJourneyDTO(journey, when);
        verifyAll();

        assertEquals(journey.getArrivalTime().asLocalTime(), result.getExpectedArrivalTime().toLocalTime());
        assertEquals(journey.getDepartTime().asLocalTime(), result.getFirstDepartureTime().toLocalTime());
        assertEquals(journey.getBeginning().forDTO(), result.getBegin().getId());
        assertEquals(notes, result.getNotes());
        assertEquals(stageDTOA, result.getStages().get(0));
        assertEquals(when.toLocalDate(), result.getQueryDate());
        validateStationList(path, result.getPath());
        validateStationList(Collections.emptyList(), result.getChangeStations());
    }

    @Test
    void shouldMapWalkingStageJourneyToMyLocation() {
        TramTime pm10 = TramTime.of(22,0);

        WalkingFromStationStage walkingStage = new WalkingFromStationStage(Deansgate.fake(), nearPiccGardensLocation,
                Duration.ofMinutes(10), pm10);
        stages.add(walkingStage);

        StageDTO stageDTOA = new StageDTO();
        EasyMock.expect(stageFactory.build(walkingStage, TravelAction.WalkFrom, when)).andReturn(stageDTOA);

        EasyMock.expect(DTOFactory.createLocationRefWithPosition(Deansgate.fake())).
                andStubReturn(new LocationRefWithPosition(Deansgate.fake()));

        Journey journey = new Journey(pm10.plusMinutes(5), pm10, pm10.plusMinutes(10), stages,
                Collections.singletonList(Deansgate.fake()), requestedNumberChanges);
        EasyMock.expect(providesNotes.createNotesForJourney(journey, when)).andReturn(notes);

        replayAll();
        JourneyDTO result = mapper.createJourneyDTO(journey, when);
        verifyAll();

        assertEquals(journey.getArrivalTime().asLocalTime(), result.getExpectedArrivalTime().toLocalTime());
        assertEquals(journey.getDepartTime().asLocalTime(), result.getFirstDepartureTime().toLocalTime());
        assertEquals(journey.getBeginning().forDTO(), result.getBegin().getId());
        assertEquals(notes, result.getNotes());
        assertEquals(stageDTOA, result.getStages().get(0));
        assertEquals(when.toLocalDate(), result.getQueryDate());
        validateStationList(Collections.singletonList(Deansgate.fake()), result.getPath());
        validateStationList(Collections.emptyList(), result.getChangeStations());
    }

    @Test
    void shouldMapJourneyWithConnectingStage() {
        TramTime time = TramTime.of(15,45);

        Station startStation = Altrincham.fakeWithPlatform(Altrincham.getRawId() + "1", Altrincham.getLatLong(),
                DataSourceID.unknown, IdFor.invalid());

        ConnectingStage<Station,Station> connectingStage = new ConnectingStage<>(BusStations.of(StopAtAltrinchamInterchange),
                startStation, Duration.ofMinutes(1), time);

        VehicleStage tramStage = getRawVehicleStage(startStation, TramStations.Shudehill.fake(),
                createRoute("route"), time.plusMinutes(1), 35, TestEnv.onlyPlatform(startStation));

        stages.add(connectingStage);
        stages.add(tramStage);

        StageDTO stageDTOA = new StageDTO();
        StageDTO stageDTOB = new StageDTO();

        EasyMock.expect(stageFactory.build(connectingStage, TravelAction.ConnectTo, when)).andReturn(stageDTOA);
        // TODO Should be board?
        EasyMock.expect(stageFactory.build(tramStage, TravelAction.Change, when)).andReturn(stageDTOB);

        EasyMock.expect(DTOFactory.createLocationRefWithPosition(Altrincham.fake())).
                andStubReturn(new LocationRefWithPosition(Altrincham.fake()));
        EasyMock.expect(DTOFactory.createLocationRefWithPosition(StopAtAltrinchamInterchange.fake())).
                andStubReturn(new LocationRefWithPosition(StopAtAltrinchamInterchange.fake()));

        final List<Location<?>> path = Collections.singletonList(Altrincham.fake());
        Journey journey = new Journey(time.plusMinutes(5), time, time.plusMinutes(10), stages, path, requestedNumberChanges);
        EasyMock.expect(providesNotes.createNotesForJourney(journey, when)).andReturn(notes);

        replayAll();
        JourneyDTO result = mapper.createJourneyDTO(journey, when);
        verifyAll();

        assertEquals(journey.getArrivalTime().asLocalTime(), result.getExpectedArrivalTime().toLocalTime());
        assertEquals(journey.getDepartTime().asLocalTime(), result.getFirstDepartureTime().toLocalTime());
        assertEquals(journey.getBeginning().forDTO(), result.getBegin().getId());
        assertEquals(notes, result.getNotes());
        assertEquals(journey.getStages().size(), result.getStages().size());
        assertEquals(stageDTOA, result.getStages().get(0));
        assertEquals(stageDTOB, result.getStages().get(1));
        assertEquals(when.toLocalDate(), result.getQueryDate());
        validateStationList(path, result.getPath());
        validateStationList(Collections.singletonList(startStation), result.getChangeStations());
    }

    // TODO MAKE REALISTIC
    @Test
    void shouldMapThreeStageJourneyWithWalk() {
        TramTime am10 = TramTime.of(10,0);
        Station begin = Altrincham.fakeWithPlatform(Altrincham.getRawId() + "1", Altrincham.getLatLong(),
                DataSourceID.unknown, IdFor.invalid());
        Platform platformA = TestEnv.onlyPlatform(begin);

        MyLocation middleA = nearPiccGardensLocation;

        Station middleB = MarketStreet.fakeWithPlatform(MarketStreet.getRawId() + "1",
                MarketStreet.getLatLong(), DataSourceID.unknown, IdFor.invalid());

        Station end = Bury.fake();

        VehicleStage rawStageA = getRawVehicleStage(begin, PiccadillyGardens.fake(),
                createRoute("route text"), am10, 42, platformA);

        Duration walkCost = Duration.ofMinutes(10);
        WalkingToStationStage walkingStage = new WalkingToStationStage(middleA, middleB, walkCost, am10);
        VehicleStage finalStage = getRawVehicleStage(middleB, end, createRoute("route3 text"), am10, 42,
                platformA);

        stages.add(rawStageA);
        stages.add(walkingStage);
        stages.add(finalStage);

        StageDTO stageDTOA = new StageDTO();
        StageDTO stageDTOB = new StageDTO();
        StageDTO stageDTOC = new StageDTO();

        EasyMock.expect(DTOFactory.createLocationRefWithPosition(Altrincham.fake())).
                andStubReturn(new LocationRefWithPosition(Altrincham.fake()));
        EasyMock.expect(DTOFactory.createLocationRefWithPosition(nearPiccGardensLocation)).
                andStubReturn(new LocationRefWithPosition(nearPiccGardensLocation));
        EasyMock.expect(DTOFactory.createLocationRefWithPosition(MarketStreet.fake())).
                andStubReturn(new LocationRefWithPosition(MarketStreet.fake()));

        EasyMock.expect(stageFactory.build(rawStageA, TravelAction.Board, when)).andReturn(stageDTOA);
        EasyMock.expect(stageFactory.build(walkingStage, TravelAction.WalkTo, when)).andReturn(stageDTOB);
        // TODO Ought to be Board?
        EasyMock.expect(stageFactory.build(finalStage, TravelAction.Change, when)).andReturn(stageDTOC);

        final List<Location<?>> path = Collections.singletonList(begin);
        Journey journey = new Journey(am10.plusMinutes(5), am10, am10.plusMinutes(10), stages, path, requestedNumberChanges);
        EasyMock.expect(providesNotes.createNotesForJourney(journey, when)).andReturn(notes);

        replayAll();
        JourneyDTO result = mapper.createJourneyDTO(journey, when);
        verifyAll();

        assertEquals(journey.getArrivalTime().asLocalTime(), result.getExpectedArrivalTime().toLocalTime());
        assertEquals(journey.getDepartTime().asLocalTime(), result.getFirstDepartureTime().toLocalTime());
        assertEquals(journey.getBeginning().forDTO(), result.getBegin().getId());
        assertEquals(notes, result.getNotes());
        assertEquals(journey.getStages().size(), result.getStages().size());
        assertEquals(stageDTOA, result.getStages().get(0));
        assertEquals(stageDTOB, result.getStages().get(1));
        assertEquals(stageDTOC, result.getStages().get(2));
        assertEquals(when.toLocalDate(), result.getQueryDate());
        validateStationList(path, result.getPath());
        validateStationList(Arrays.asList( middleA, middleB ), result.getChangeStations());
    }

    @Test
    void shouldMap2StageJoruneyWithChange() {
        TramTime startTime = TramTime.of(22,50);
        Station start = transportData.getFirst();
        Station middle = transportData.getSecond();
        Station finish = transportData.getInterchange();
        Platform platform = MutablePlatform.buildForTFGMTram(start.forDTO() + "1", start,
                start.getLatLong(), DataSourceID.unknown, IdFor.invalid());

        VehicleStage rawStageA = getRawVehicleStage(start, middle, createRoute("route text"), startTime,
                18, platform);
        VehicleStage rawStageB = getRawVehicleStage(middle, finish, createRoute("route2 text"), startTime.plusMinutes(18),
                42, platform);

        stages.add(rawStageA);
        stages.add(rawStageB);

        StageDTO stageDTOA = new StageDTO();
        StageDTO stageDTOB = new StageDTO();

        EasyMock.expect(DTOFactory.createLocationRefWithPosition(start)).andStubReturn(new LocationRefWithPosition(start));
        EasyMock.expect(DTOFactory.createLocationRefWithPosition(middle)).andReturn(new LocationRefWithPosition(middle));

        EasyMock.expect(stageFactory.build(rawStageA, TravelAction.Board, when)).andReturn(stageDTOA);
        EasyMock.expect(stageFactory.build(rawStageB, TravelAction.Change, when)).andReturn(stageDTOB);

        final List<Location<?>> path = Collections.singletonList(start);
        Journey journey = new Journey(startTime.plusMinutes(5), startTime, startTime.plusMinutes(10), stages, path, requestedNumberChanges);
        EasyMock.expect(providesNotes.createNotesForJourney(journey, when)).andReturn(notes);

        replayAll();
        JourneyDTO result = mapper.createJourneyDTO(journey, when);
        verifyAll();

        assertEquals(journey.getArrivalTime().asLocalTime(), result.getExpectedArrivalTime().toLocalTime());
        assertEquals(journey.getDepartTime().asLocalTime(), result.getFirstDepartureTime().toLocalTime());
        assertEquals(journey.getBeginning().forDTO(), result.getBegin().getId());
        assertEquals(notes, result.getNotes());
        assertEquals(journey.getStages().size(), result.getStages().size());
        assertEquals(stageDTOA, result.getStages().get(0));
        assertEquals(stageDTOB, result.getStages().get(1));
        assertEquals(when.toLocalDate(), result.getQueryDate());
        validateStationList(path, result.getPath());
        validateStationList(Collections.singletonList(middle), result.getChangeStations());
    }

    private VehicleStage getRawVehicleStage(Station start, Station finish, Route route, TramTime startTime,
                                            int costInMinutes, Platform platform) {


        Trip validTrip = transportData.getTripById(StringIdFor.createId(TRIP_A_ID));

        List<Integer> passedStations = new ArrayList<>();
        VehicleStage vehicleStage = new VehicleStage(start, route, TransportMode.Tram, validTrip,
                startTime.plusMinutes(1), finish, passedStations);

        vehicleStage.setCost(Duration.ofMinutes(costInMinutes));
        vehicleStage.setPlatform(platform);

        return vehicleStage;

    }

    private void validateStationList(List<Location<?>> expected, List<LocationRefWithPosition> results) {
        assertEquals(expected.size(), results.size());
        List<String> expectedIds = expected.stream().map(IdForDTO::forDTO).collect(Collectors.toList());
        List<String> resultIds = results.stream().map(LocationRefDTO::getId).collect(Collectors.toList());
        assertEquals(expectedIds, resultIds);
    }

}
