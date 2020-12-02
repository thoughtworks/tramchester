package com.tramchester.unit.domain.presentation.DTO.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.Platform;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.MyLocationFactory;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.*;
import com.tramchester.domain.presentation.DTO.factory.JourneyDTOFactory;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.presentation.StationNote;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestStation;
import com.tramchester.testSupport.TramStations;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.operation.TransformException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.*;

class JourneyDTOFactoryTest extends EasyMockSupport {
    private final Station stationA = TestStation.forTest("stationA", "area", "nameA", new LatLong(-2, -1), TransportMode.Tram);
    private final Station stationB = TestStation.forTest("stationB", "area", "nameB", new LatLong(-3, 1), TransportMode.Tram);
    private JourneyDTOFactory factory;
    private MyLocationFactory myLocationFactory;
    private final LocalDate when = TestEnv.testDay();

    private final TramTime queryTime = TramTime.of(8, 46);
    private List<Note> notes;
    private final List<StationRefWithPosition> path = new ArrayList<>();

    JourneyDTOFactoryTest() throws TransformException {
    }

    @BeforeEach
    void beforeEachTestRuns() {
        myLocationFactory = new MyLocationFactory(new ObjectMapper());
        factory = new JourneyDTOFactory();
        notes = Collections.singletonList(new StationNote(Note.NoteType.Live, "someText", TramStations.of(TramStations.Bury)));
    }

    @Test
    void shouldCreateJourneyDTO() {

        StageDTO transportStage = createStage(timeToday(10, 8), timeToday(10, 20), 11);

        replayAll();
        JourneyDTO journeyDTO = factory.build(Collections.singletonList(transportStage), queryTime, notes, path, when);
        verifyAll();

        assertEquals(LocalDateTime.of(when, LocalTime.of(10, 20)), journeyDTO.getExpectedArrivalTime());
        assertEquals(LocalDateTime.of(when, LocalTime.of(10, 8)), journeyDTO.getFirstDepartureTime());
        assertEquals(stationA.forDTO(), journeyDTO.getBegin().getId());
        assertEquals(stationB.forDTO(), journeyDTO.getEnd().getId());
        assertTrue(journeyDTO.getIsDirect());
        assertEquals(1,journeyDTO.getStages().size());
        assertEquals(transportStage, journeyDTO.getStages().get(0));
        assertEquals(notes, journeyDTO.getNotes());
        assertEquals(when, journeyDTO.getQueryDate());
    }

    @Test
    void shouldCreateJourneyDTOWithTwoStages() {

        StageDTO transportStageA = createStage(timeToday(10, 8), timeToday(10, 20), 11);
        StageDTO transportStageB = createStage(timeToday(10, 22), timeToday(11, 45), 30);

        replayAll();
        JourneyDTO journeyDTO = factory.build(Arrays.asList(transportStageA, transportStageB), queryTime, notes, path, when);
        verifyAll();

        assertEquals(LocalDateTime.of(when, LocalTime.of(10, 8)), journeyDTO.getFirstDepartureTime());
        assertEquals(LocalDateTime.of(when, LocalTime.of(11, 45)), journeyDTO.getExpectedArrivalTime());
        assertEquals(stationA.forDTO(), journeyDTO.getBegin().getId());
        assertEquals(stationB.forDTO(), journeyDTO.getEnd().getId());
        assertFalse(journeyDTO.getIsDirect());
        assertEquals(2,journeyDTO.getStages().size());
        assertEquals(transportStageA, journeyDTO.getStages().get(0));
        assertEquals(transportStageB, journeyDTO.getStages().get(1));
        assertEquals(notes, journeyDTO.getNotes());
        assertEquals(when, journeyDTO.getQueryDate());

    }

    @Test
    void shouldHaveRightSummaryAndHeadingFor2Stage() {

        PlatformDTO boardingPlatformA = new PlatformDTO(new Platform(TramStations.Altrincham.getId()+"1", TramStations.Altrincham.getName()));
        StageDTO stageA = new StageDTO(createStationRef(TramStations.Altrincham), createStationRef(TramStations.Cornbrook),
                createStationRef(TramStations.Altrincham),
                boardingPlatformA, timeToday(10, 8), timeToday(10, 20),
                20-8,
                "headSign", TransportMode.Tram, 9, "routeName",
                TravelAction.Board, "routeShortName", when);

        PlatformDTO boardingPlatformB = new PlatformDTO(new Platform(TramStations.Cornbrook.getId()+"1", TramStations.Cornbrook.getName()));
        StageDTO stageB = new StageDTO(createStationRef(TramStations.Cornbrook), createStationRef(TramStations.Deansgate),
                createStationRef(TramStations.Cornbrook),
                boardingPlatformB, timeToday(10, 8), timeToday(10, 20),
                20-8,
                "headSign", TransportMode.Tram, 9, "routeName",
                TravelAction.Change, "routeShortName", when);

        List<StageDTO> stages = Arrays.asList(stageA, stageB);

        replayAll();
        JourneyDTO journeyDTO = factory.build(stages, queryTime, notes, path, when);
        verifyAll();

        assertEquals(2, journeyDTO.getStages().size());
        assertFalse(journeyDTO.getIsDirect());
        assertEquals(TramStations.Altrincham.forDTO(), journeyDTO.getBegin().getId());
        assertEquals(TramStations.Deansgate.forDTO(), journeyDTO.getEnd().getId());
        assertEquals(1, journeyDTO.getChangeStations().size());
        assertEquals(TramStations.Cornbrook.forDTO(), journeyDTO.getChangeStations().get(0).getId());

        IdSet<Platform> callingPlatformIds = journeyDTO.getCallingPlatformIds();
        assertEquals(2, callingPlatformIds.size());
        Set<String> ids = callingPlatformIds.stream().map(IdFor::forDTO).collect(Collectors.toSet());
        assertTrue(ids.contains(boardingPlatformA.getId()));
        assertTrue(ids.contains(boardingPlatformB.getId()));
    }

    private LocalDateTime timeToday(int hour, int minute) {
        return LocalDateTime.of(when, LocalTime.of(hour, minute));
    }

    @Test
    void shouldHaveRightSummaryAndHeadingFor3Stage() {
        List<StageDTO> stages = new LinkedList<>();
        stages.add(createStage(TramStations.Altrincham, TravelAction.Board, TramStations.Cornbrook, 9));
        stages.add(createStage(TramStations.Cornbrook, TravelAction.Change, TramStations.Deansgate, 1));
        stages.add(createStage(TramStations.Deansgate, TravelAction.Change, TramStations.Bury, 1));


        replayAll();
        JourneyDTO journeyDTO = factory.build(stages, queryTime, notes, path, when);
        verifyAll();

        assertFalse(journeyDTO.getIsDirect());
        assertEquals(TramStations.Altrincham.forDTO(), journeyDTO.getBegin().getId());
        assertEquals(TramStations.Bury.forDTO(), journeyDTO.getEnd().getId());

        List<String> changes = journeyDTO.getChangeStations().stream().
                map(StationRefDTO::getName).collect(Collectors.toList());
        assertEquals(Arrays.asList(TramStations.Cornbrook.getName(), TramStations.Deansgate.getName()), changes);
    }

    @Test
    void shouldHaveCorrectSummaryAndHeadingForWalkAndTram() {
        List<StageDTO> stages = new LinkedList<>();
        MyLocation start = myLocationFactory.create(new LatLong(-2,1));
        TramStations destination = TramStations.Cornbrook;

        stages.add(createWalkingStage(start, destination, timeToday(8, 13), timeToday(8, 16)));
        stages.add(createStage(TramStations.Cornbrook, TravelAction.Change, TramStations.Deansgate, 1));

        replayAll();
        JourneyDTO journey = factory.build(stages, queryTime, notes, path, when);
        verifyAll();

        assertEquals(LocalDateTime.of(when, LocalTime.of(8,13)), journey.getFirstDepartureTime());
        Assertions.assertTrue(journey.getChangeStations().isEmpty());
        Assertions.assertTrue(journey.getIsDirect());
    }

    @Test
    void shouldHaveBeginAndEnd() {
        List<StageDTO> stages = createThreeStages();

        replayAll();
        JourneyDTO journeyDTO = factory.build(stages, queryTime, notes, path, when);
        verifyAll();

        assertEquals(TramStations.Altrincham.forDTO(), journeyDTO.getBegin().getId());
        assertEquals(TramStations.ExchangeSquare.forDTO(), journeyDTO.getEnd().getId());
    }

    @Test
    void shouldHaveRightSummaryAndHeadingFor4Stage() {
        List<StageDTO> stages = createThreeStages();
        stages.add(createStage(TramStations.ExchangeSquare, TravelAction.Change, TramStations.Rochdale, 24));

        replayAll();
        JourneyDTO journey = factory.build(stages, queryTime, notes, path, when);
        verifyAll();

        List<String> changeStations = journey.getChangeStations().stream().
                map(StationRefDTO::getName).collect(Collectors.toList());
        assertEquals(Arrays.asList("Cornbrook", "Victoria", "Exchange Square"), changeStations);
    }

    @Test
    void shouldHaveCorrectSummaryAndHeadingForSingleWalkingStage() {
        List<StageDTO> stages = new LinkedList<>();
        MyLocation myLocation = myLocationFactory.create(new LatLong(-1, 2));
        stages.add(createWalkingStage(myLocation, TramStations.Victoria, timeToday(8, 13), timeToday(8, 16)));

        replayAll();
        JourneyDTO journey = factory.build(stages, queryTime, notes, path, when);
        verifyAll();

        Assertions.assertTrue(journey.getChangeStations().isEmpty());
    }

    @Test
    void shouldHaveCorrectSummaryAndHeadingForTramStagesConnectedByWalk() {
        List<StageDTO> stages = new LinkedList<>();
        stages.add(createStage(TramStations.ManAirport, TravelAction.Board, TramStations.Deansgate, 13));
        stages.add(createConnectingStage(TramStations.Deansgate, TramStations.MarketStreet, timeToday(8, 13), timeToday(8, 16)));
        stages.add(createStage(TramStations.MarketStreet, TravelAction.Change, TramStations.Bury, 16));

        replayAll();
        JourneyDTO journey = factory.build(stages, queryTime, notes, path, when);
        verifyAll();

        List<String> changeNames = journey.getChangeStations().stream().
                map(StationRefDTO::getName).collect(Collectors.toList());
        assertThat(changeNames, contains("Deansgate-Castlefield", "Market Street"));
    }

    @Test
    void reproduceIssueWithJourneyWithJustWalking() {
        List<StageDTO> stages = new LinkedList<>();
        MyLocation start = myLocationFactory.create(new LatLong(1, 2));
        stages.add(createWalkingStage(start, TramStations.Altrincham, timeToday(8, 0), timeToday(8, 10)));

        replayAll();
        JourneyDTO journey = factory.build(stages, queryTime, notes, path, when);
        verifyAll();

        ObjectMapper objectMapper = new ObjectMapper();

        Assertions.assertAll(() -> objectMapper.writeValueAsString(journey));
    }

    private List<StageDTO> createThreeStages() {
        List<StageDTO> stages = new LinkedList<>();
        stages.add(createStage(TramStations.Altrincham, TravelAction.Board, TramStations.Cornbrook, 7));
        stages.add(createStage(TramStations.Cornbrook, TravelAction.Change, TramStations.Victoria, 3));
        stages.add(createStage(TramStations.Victoria, TravelAction.Change, TramStations.ExchangeSquare, 13));
        return stages;
    }

    private StageDTO createStage(TramStations firstStation, TravelAction travelAction, TramStations lastStation, int passedStops) {
        return new StageDTO(createStationRef(firstStation), createStationRef(lastStation), createStationRef(firstStation),
                timeToday(10, 8), timeToday(10, 20), 20-8,
                "headSign", TransportMode.Tram,
                passedStops, "routeName", travelAction, "routeShortName",when);
    }

    private StationRefWithPosition createStationRef(TramStations station) {
        return new StationRefWithPosition(TramStations.of(station));
    }

    private StageDTO createStage(LocalDateTime departs, LocalDateTime arrivesEnd, int passedStops) {
        return new StageDTO(new StationRefWithPosition(stationA), new StationRefWithPosition(stationB), new StationRefWithPosition(stationA),
                departs, arrivesEnd, 42,
                "headSign", TransportMode.Tram,
                passedStops, "routeName", TravelAction.Board, "routeShortName", when);

    }

    private StageDTO createConnectingStage(TramStations start, TramStations destination, LocalDateTime departs, LocalDateTime arrivesEnd) {

        return new StageDTO(createStationRef(start), createStationRef(destination), new StationRefWithPosition(stationA),
                departs, arrivesEnd, 9,
                "walking", TransportMode.Walk,
                0, "walking", TravelAction.Board, "routeShortName", when);
    }

    private StageDTO createWalkingStage(MyLocation start, TramStations destination, LocalDateTime departs, LocalDateTime arrivesEnd) {

        return new StageDTO(new StationRefWithPosition(start), createStationRef(destination), new StationRefWithPosition(stationA),
                departs, arrivesEnd, 9,
                "walking", TransportMode.Walk,
                0, "walking", TravelAction.Board, "routeShortName", when);
    }

}
