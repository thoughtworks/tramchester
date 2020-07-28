package com.tramchester.unit.domain.presentation.DTO.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.*;
import com.tramchester.domain.places.Location;
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
import com.tramchester.testSupport.Stations;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.operation.TransformException;

import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.*;

class JourneyDTOFactoryTest extends EasyMockSupport {
    private final Location stationA = Station.forTest("stationA", "area", "nameA", new LatLong(-2, -1), TransportMode.Tram);
    private final Location stationB = Station.forTest("stationB", "area", "nameB", new LatLong(-3, 1), TransportMode.Tram);
    private JourneyDTOFactory factory;
    private MyLocationFactory myLocationFactory;

    private final TramTime queryTime = TramTime.of(8,46);
    private List<Note> notes;
    private final List<StationRefWithPosition> path = new ArrayList<>();

    JourneyDTOFactoryTest() throws TransformException {
    }

    @BeforeEach
    void beforeEachTestRuns() {
        myLocationFactory = new MyLocationFactory(new ObjectMapper());
        factory = new JourneyDTOFactory();
        notes = Collections.singletonList(new StationNote(Note.NoteType.Live, "someText", Stations.Bury));
    }

    @Test
    void shouldCreateJourneyDTO() {

        StageDTO transportStage = createStage(TramTime.of(10, 8), TramTime.of(10, 20), 11);

        replayAll();
        JourneyDTO journeyDTO = factory.build(Collections.singletonList(transportStage), queryTime, notes, path);
        verifyAll();

        assertEquals(TramTime.of(10, 20), journeyDTO.getExpectedArrivalTime());
        assertEquals(TramTime.of(10, 8), journeyDTO.getFirstDepartureTime());
        assertEquals(stationA.forDTO(), journeyDTO.getBegin().getId());
        assertEquals(stationB.forDTO(), journeyDTO.getEnd().getId());
        assertTrue(journeyDTO.getIsDirect());
        assertEquals(1,journeyDTO.getStages().size());
        assertEquals(transportStage, journeyDTO.getStages().get(0));
        assertEquals(notes, journeyDTO.getNotes());
    }

    @Test
    void shouldCreateJourneyDTOWithTwoStages() {

        StageDTO transportStageA = createStage(TramTime.of(10, 8), TramTime.of(10, 20), 11);
        StageDTO transportStageB = createStage(TramTime.of(10, 22), TramTime.of(11, 45), 30);

        replayAll();
        JourneyDTO journeyDTO = factory.build(Arrays.asList(transportStageA, transportStageB), queryTime, notes, path);
        verifyAll();

        assertEquals(TramTime.of(10, 8), journeyDTO.getFirstDepartureTime());
        assertEquals(TramTime.of(11, 45), journeyDTO.getExpectedArrivalTime());
        assertEquals(stationA.forDTO(), journeyDTO.getBegin().getId());
        assertEquals(stationB.forDTO(), journeyDTO.getEnd().getId());
        assertFalse(journeyDTO.getIsDirect());
        assertEquals(2,journeyDTO.getStages().size());
        assertEquals(transportStageA, journeyDTO.getStages().get(0));
        assertEquals(transportStageB, journeyDTO.getStages().get(1));
        assertEquals(notes, journeyDTO.getNotes());

    }

    @Test
    void shouldHaveRightSummaryAndHeadingFor2Stage() {

        PlatformDTO boardingPlatformA = new PlatformDTO(new Platform(Stations.Altrincham.getId()+"1", Stations.Altrincham.getName()));
        StageDTO stageA = new StageDTO(new StationRefWithPosition(Stations.Altrincham), new StationRefWithPosition(Stations.Cornbrook),
                new StationRefWithPosition(Stations.Altrincham),
                true, boardingPlatformA, TramTime.of(10, 8), TramTime.of(10, 20),
                20-8,
                "headSign", TransportMode.Tram, 9, "routeName",
                TravelAction.Board, "routeShortName");

        PlatformDTO boardingPlatformB = new PlatformDTO(new Platform(Stations.Cornbrook.getId()+"1", Stations.Cornbrook.getName()));
        StageDTO stageB = new StageDTO(new StationRefWithPosition(Stations.Cornbrook), new StationRefWithPosition(Stations.Deansgate),
                new StationRefWithPosition(Stations.Cornbrook),
                true, boardingPlatformB, TramTime.of(10, 8), TramTime.of(10, 20),
                20-8,
                "headSign", TransportMode.Tram, 9, "routeName",
                TravelAction.Change, "routeShortName");

        List<StageDTO> stages = Arrays.asList(stageA, stageB);

        replayAll();
        JourneyDTO journeyDTO = factory.build(stages, queryTime, notes, path);
        verifyAll();

        assertEquals(2, journeyDTO.getStages().size());
        assertFalse(journeyDTO.getIsDirect());
        assertEquals(Stations.Altrincham.forDTO(), journeyDTO.getBegin().getId());
        assertEquals(Stations.Deansgate.forDTO(), journeyDTO.getEnd().getId());
        assertEquals(1, journeyDTO.getChangeStations().size());
        assertEquals(Stations.Cornbrook.forDTO(), journeyDTO.getChangeStations().get(0).getId());

        IdSet<Platform> callingPlatformIds = journeyDTO.getCallingPlatformIds();
        assertEquals(2, callingPlatformIds.size());
        Set<String> ids = callingPlatformIds.stream().map(IdFor::forDTO).collect(Collectors.toSet());
        assertTrue(ids.contains(boardingPlatformA.getId()));
        assertTrue(ids.contains(boardingPlatformB.getId()));
    }

    @Test
    void shouldHaveRightSummaryAndHeadingFor3Stage() {
        List<StageDTO> stages = new LinkedList<>();
        stages.add(createStage(Stations.Altrincham, TravelAction.Board, Stations.Cornbrook, 9));
        stages.add(createStage(Stations.Cornbrook, TravelAction.Change, Stations.Deansgate, 1));
        stages.add(createStage(Stations.Deansgate, TravelAction.Change, Stations.Bury, 1));


        replayAll();
        JourneyDTO journeyDTO = factory.build(stages, queryTime, notes, path);
        verifyAll();

        assertFalse(journeyDTO.getIsDirect());
        assertEquals(Stations.Altrincham.forDTO(), journeyDTO.getBegin().getId());
        assertEquals(Stations.Bury.forDTO(), journeyDTO.getEnd().getId());

        List<String> changes = journeyDTO.getChangeStations().stream().
                map(StationRefDTO::getName).collect(Collectors.toList());
        assertEquals(Arrays.asList(Stations.Cornbrook.getName(), Stations.Deansgate.getName()), changes);
    }

//    @Test
//    void shouldCreateJourneyDTOWithDueTram() {
//        LocalDateTime when = LocalDateTime.of(2017,11,30,18,41);
//
//        StageDTO stageDTO = createStageDTOWithDueTram(Stations.Cornbrook, when, 5);
//
//        replayAll();
//        JourneyDTO journeyDTO = factory.build(Collections.singletonList(stageDTO), queryTime, notes);
//        verifyAll();
//
//        assertEquals("Double tram Due at 18:46", journeyDTO.getDueTram());
//    }
//
//    @Test
//    void shouldCreateJourneyDTOWithDueTramTimeOutOfRange() {
//
//        LocalDateTime dateTime = LocalDateTime.of(2017,11,30,18,41);
//        StageDTO stageDTO = createStageDTOWithDueTram(Stations.Cornbrook, dateTime, 22);
//
//        replayAll();
//        JourneyDTO journeyDTO = factory.build(Collections.singletonList(stageDTO), queryTime, notes);
//        verifyAll();
//
//        Assertions.assertNull(journeyDTO.getDueTram());
//    }
//
//    @Test
//    void shouldCreateJourneyDTOWithLaterDueTramMatching() {
//        LocalDateTime when = LocalDateTime.of(2017,11,30, 18,41);
//        StageDTO stageDTO = createStageDTOWithDueTram(Stations.Cornbrook, when, 7);
//
//        replayAll();
//        JourneyDTO journeyDTO = factory.build(Collections.singletonList(stageDTO), queryTime, notes);
//        verifyAll();
//
//        // select due tram that is closer to stage departure when more than one available
//        assertEquals("Double tram Due at 18:48", journeyDTO.getDueTram());
//    }

    @Test
    void shouldHaveCorrectSummaryAndHeadingForWalkAndTram() {
        List<StageDTO> stages = new LinkedList<>();
        Location start = myLocationFactory.create(new LatLong(-2,1));
        Location destination = Stations.Cornbrook;

        stages.add(createWalkingStage(start, destination, TramTime.of(8,13), TramTime.of(8,16)));
        stages.add(createStage(Stations.Cornbrook, TravelAction.Change, Stations.Deansgate, 1));

        replayAll();
        JourneyDTO journey = factory.build(stages, queryTime, notes, path);
        verifyAll();

        assertEquals(TramTime.of(8,13), journey.getFirstDepartureTime());
        Assertions.assertTrue(journey.getChangeStations().isEmpty());
        Assertions.assertTrue(journey.getIsDirect());
    }

    @Test
    void shouldHaveBeginAndEnd() {
        List<StageDTO> stages = createThreeStages();

        replayAll();
        JourneyDTO journeyDTO = factory.build(stages, queryTime, notes, path);
        verifyAll();

        assertEquals(Stations.Altrincham.forDTO(), journeyDTO.getBegin().getId());
        assertEquals(Stations.ExchangeSquare.forDTO(), journeyDTO.getEnd().getId());
    }

    @Test
    void shouldHaveRightSummaryAndHeadingFor4Stage() {
        List<StageDTO> stages = createThreeStages();
        stages.add(createStage(Stations.ExchangeSquare, TravelAction.Change, Stations.Rochdale, 24));

        replayAll();
        JourneyDTO journey = factory.build(stages, queryTime, notes, path);
        verifyAll();

        List<String> changeStations = journey.getChangeStations().stream().
                map(StationRefDTO::getName).collect(Collectors.toList());
        assertEquals(Arrays.asList("Cornbrook", "Victoria", "Exchange Square"), changeStations);
    }

    @Test
    void shouldHaveCorrectSummaryAndHeadingForSingleWalkingStage() {
        List<StageDTO> stages = new LinkedList<>();
        MyLocation myLocation = myLocationFactory.create(new LatLong(-1, 2));
        stages.add(createWalkingStage(myLocation, Stations.Victoria, TramTime.of(8,13), TramTime.of(8,16)));

        replayAll();
        JourneyDTO journey = factory.build(stages, queryTime, notes, path);
        verifyAll();

        Assertions.assertTrue(journey.getChangeStations().isEmpty());
    }

    @Test
    void shouldHaveCorrectSummaryAndHeadingForTramStagesConnectedByWalk() {
        List<StageDTO> stages = new LinkedList<>();
        stages.add(createStage(Stations.ManAirport, TravelAction.Board, Stations.Deansgate, 13));
        stages.add(createWalkingStage(Stations.Deansgate, Stations.MarketStreet, TramTime.of(8,13), TramTime.of(8,16)));
        stages.add(createStage(Stations.MarketStreet, TravelAction.Change, Stations.Bury, 16));

        replayAll();
        JourneyDTO journey = factory.build(stages, queryTime, notes, path);
        verifyAll();

        List<String> changeNames = journey.getChangeStations().stream().
                map(ref -> ref.getName()).collect(Collectors.toList());
        assertThat(changeNames, contains("Deansgate-Castlefield", "Market Street"));
    }

    @Test
    void reproduceIssueWithJourneyWithJustWalking() {
        List<StageDTO> stages = new LinkedList<>();
        MyLocation start = myLocationFactory.create(new LatLong(1, 2));
        stages.add(createWalkingStage(start, Stations.Altrincham, TramTime.of(8,0), TramTime.of(8,10)));

        replayAll();
        JourneyDTO journey = factory.build(stages, queryTime, notes, path);
        verifyAll();

        ObjectMapper objectMapper = new ObjectMapper();

        Assertions.assertAll(() -> objectMapper.writeValueAsString(journey));
    }

    private List<StageDTO> createThreeStages() {
        List<StageDTO> stages = new LinkedList<>();
        stages.add(createStage(Stations.Altrincham, TravelAction.Board, Stations.Cornbrook, 7));
        stages.add(createStage(Stations.Cornbrook, TravelAction.Change, Stations.Victoria, 3));
        stages.add(createStage(Stations.Victoria, TravelAction.Change, Stations.ExchangeSquare, 13));
        return stages;
    }

    private StageDTO createStage(Location firstStation, TravelAction travelAction, Location lastStation, int passedStops) {
        return new StageDTO(new StationRefWithPosition(firstStation), new StationRefWithPosition(lastStation), new StationRefWithPosition(firstStation),
                false, null, TramTime.of(10, 8), TramTime.of(10, 20), 20-8,
                "headSign", TransportMode.Tram,
                passedStops, "routeName", travelAction, "routeShortName");
    }

    private StageDTO createStage(TramTime departs, TramTime arrivesEnd, int passedStops) {
        return new StageDTO(new StationRefWithPosition(stationA), new StationRefWithPosition(stationB), new StationRefWithPosition(stationA),
                false, null, departs, arrivesEnd, 42,
                "headSign", TransportMode.Tram,
                passedStops, "routeName", TravelAction.Board, "routeShortName");

    }

    private StageDTO createWalkingStage(Location start, Location destination, TramTime departs, TramTime arrivesEnd) {

        return new StageDTO(new StationRefWithPosition(start), new StationRefWithPosition(destination), new StationRefWithPosition(stationA),
                false, null, departs, arrivesEnd, 9,
                "walking", TransportMode.Walk,
                0, "walking", TravelAction.Board, "routeShortName");
    }

//    private StageDTO createStageDTO(Station station, LocalDateTime whenTime, int wait) {
//        StationDepartureInfo departureInfo = new StationDepartureInfo("displayId", "lineName",
//                StationDepartureInfo.Direction.Incoming, "platform", station, "message", whenTime);
//
//        LocalTime when = whenTime.toLocalTime();
//
//        departureInfo.addDueTram(new DueTram(Stations.Deansgate, "Due", 10, "Single", when));
//        departureInfo.addDueTram(new DueTram(station, "Departed", 0, "Single",when));
//        departureInfo.addDueTram(new DueTram(station, "Due", wait, "Double",when));
//        departureInfo.addDueTram(new DueTram(station, "Due", wait+2, "Double",when));
//
//        PlatformDTO platform = new PlatformDTO(new Platform("platformId", "platformName"));
////        platform.setDepartureInfo(new StationDepartureInfoDTO(departureInfo));
//
//        int durationOfStage = 15;
//        return new StageDTO(new LocationDTO(stationA),
//                new LocationDTO(stationB), new LocationDTO(stationB),
//                true, platform, TramTime.of(when.plusMinutes(1)),
//                TramTime.of(when.plusMinutes(durationOfStage)),
//                durationOfStage,
//                station.getName(), TransportMode.Tram,
//                "displayClass", 23,"routeName", TravelAction.Board, "routeShortName");
//    }
}
