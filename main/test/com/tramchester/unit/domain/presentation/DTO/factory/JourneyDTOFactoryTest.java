package com.tramchester.unit.domain.presentation.DTO.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.HasId;
import com.tramchester.domain.Platform;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.MyLocationFactory;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.*;
import com.tramchester.domain.presentation.DTO.factory.JourneyDTOFactory;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.domain.time.TramTime;
import com.tramchester.mappers.HeadsignMapper;
import com.tramchester.testSupport.Stations;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.*;

class JourneyDTOFactoryTest extends EasyMockSupport {
    private final Location stationA = new Station("stationA", "area", "nameA", new LatLong(-2, -1), false);
    private final Location stationB = new Station("stationB", "area", "nameB", new LatLong(-3, 1), false);
    private JourneyDTOFactory factory;
    private MyLocationFactory myLocationFactory;

    private final TramTime queryTime = TramTime.of(8,46);
    private List<Note> notes;

    @BeforeEach
    void beforeEachTestRuns() {
        myLocationFactory = new MyLocationFactory(new ObjectMapper());
        factory = new JourneyDTOFactory(new HeadsignMapper());
        notes = Collections.singletonList(new Note(Note.NoteType.Live, "someText"));
    }

    @Test
    void shouldCreateJourneyDTO() {

        StageDTO transportStage = createStage(TramTime.of(10, 8), TramTime.of(10, 20), 11);

        replayAll();
        JourneyDTO journeyDTO = factory.build(Collections.singletonList(transportStage), queryTime, notes);
        verifyAll();

        assertEquals(TramTime.of(10, 20), journeyDTO.getExpectedArrivalTime());
        assertEquals(TramTime.of(10, 8), journeyDTO.getFirstDepartureTime());
        assertEquals(stationA.getId(), journeyDTO.getBegin().getId());
        assertEquals(stationB.getId(), journeyDTO.getEnd().getId());
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
        JourneyDTO journeyDTO = factory.build(Arrays.asList(transportStageA, transportStageB), queryTime, notes);
        verifyAll();

        assertEquals(TramTime.of(10, 8), journeyDTO.getFirstDepartureTime());
        assertEquals(TramTime.of(11, 45), journeyDTO.getExpectedArrivalTime());
        assertEquals(stationA.getId(), journeyDTO.getBegin().getId());
        assertEquals(stationB.getId(), journeyDTO.getEnd().getId());
        assertFalse(journeyDTO.getIsDirect());
        assertEquals(2,journeyDTO.getStages().size());
        assertEquals(transportStageA, journeyDTO.getStages().get(0));
        assertEquals(transportStageB, journeyDTO.getStages().get(1));
        assertEquals(notes, journeyDTO.getNotes());

    }

    @Test
    void shouldHaveRightSummaryAndHeadingFor2Stage() {

        PlatformDTO boardingPlatformA = new PlatformDTO(new Platform(Stations.Altrincham.getId()+"1", Stations.Altrincham.getName()));
        StageDTO stageA = new StageDTO(new LocationDTO(Stations.Altrincham), new LocationDTO(Stations.Cornbrook),
                new LocationDTO(Stations.Altrincham),
                true, boardingPlatformA, TramTime.of(10, 8), TramTime.of(10, 20),
                20-8,
                "headSign", TransportMode.Tram, "cssClass", 9, "routeName",
                TravelAction.Board, "routeShortName");

        PlatformDTO boardingPlatformB = new PlatformDTO(new Platform(Stations.Cornbrook.getId()+"1", Stations.Cornbrook.getName()));
        StageDTO stageB = new StageDTO(new LocationDTO(Stations.Cornbrook), new LocationDTO(Stations.Deansgate),
                new LocationDTO(Stations.Cornbrook),
                true, boardingPlatformB, TramTime.of(10, 8), TramTime.of(10, 20),
                20-8,
                "headSign", TransportMode.Tram, "cssClass", 9, "routeName",
                TravelAction.Change, "routeShortName");

        List<StageDTO> stages = Arrays.asList(stageA, stageB);

        replayAll();
        JourneyDTO journey = factory.build(stages, queryTime, notes);
        verifyAll();

        assertEquals(2, journey.getStages().size());
        assertFalse(journey.getIsDirect());
        assertEquals(Stations.Altrincham.getId(), journey.getBegin().getId());
        assertEquals(Stations.Deansgate.getId(), journey.getEnd().getId());
        assertEquals(Collections.singletonList(Stations.Cornbrook.getName()), journey.getChangeStations());

        List<HasId> callingPlatformIds = journey.getCallingPlatformIds();
        assertEquals(2, callingPlatformIds.size());
        Set<String> ids = callingPlatformIds.stream().map(HasId::getId).collect(Collectors.toSet());
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
        JourneyDTO journey = factory.build(stages, queryTime, notes);
        verifyAll();

        assertFalse(journey.getIsDirect());
        assertEquals(Stations.Altrincham.getId(), journey.getBegin().getId());
        assertEquals(Stations.Bury.getId(), journey.getEnd().getId());
        List<String> changes = Arrays.asList(Stations.Cornbrook.getName(), Stations.Deansgate.getName());
        assertEquals(changes, journey.getChangeStations());
    }

    @Test
    void shouldCreateJourneyDTOWithDueTram() {
        LocalDateTime when = LocalDateTime.of(2017,11,30,18,41);

        StageDTO stageDTO = createStageDTOWithDueTram(Stations.Cornbrook, when, 5);

        replayAll();
        JourneyDTO journeyDTO = factory.build(Collections.singletonList(stageDTO), queryTime, notes);
        verifyAll();

        assertEquals("Double tram Due at 18:46", journeyDTO.getDueTram());
    }

    @Test
    void shouldCreateJourneyDTOWithDueTramTimeOutOfRange() {

        LocalDateTime dateTime = LocalDateTime.of(2017,11,30,18,41);
        StageDTO stageDTO = createStageDTOWithDueTram(Stations.Cornbrook, dateTime, 22);

        replayAll();
        JourneyDTO journeyDTO = factory.build(Collections.singletonList(stageDTO), queryTime, notes);
        verifyAll();

        Assertions.assertNull(journeyDTO.getDueTram());
    }

    @Test
    void shouldCreateJourneyDTOWithLaterDueTramMatching() {
        LocalDateTime when = LocalDateTime.of(2017,11,30, 18,41);
        StageDTO stageDTO = createStageDTOWithDueTram(Stations.Cornbrook, when, 7);

        replayAll();
        JourneyDTO journeyDTO = factory.build(Collections.singletonList(stageDTO), queryTime, notes);
        verifyAll();

        // select due tram that is closer to stage departure when more than one available
        assertEquals("Double tram Due at 18:48", journeyDTO.getDueTram());
    }

    @Test
    void shouldHaveCorrectSummaryAndHeadingForWalkAndTram() {
        List<StageDTO> stages = new LinkedList<>();
        Location start = myLocationFactory.create(new LatLong(-2,1));
        Location destination = Stations.Cornbrook;

        stages.add(createWalkingStage(start, destination, TramTime.of(8,13), TramTime.of(8,16)));
        stages.add(createStage(Stations.Cornbrook, TravelAction.Change, Stations.Deansgate, 1));

        replayAll();
        JourneyDTO journey = factory.build(stages, queryTime, notes);
        verifyAll();

        assertEquals(TramTime.of(8,13), journey.getFirstDepartureTime());
        Assertions.assertTrue(journey.getChangeStations().isEmpty());
        Assertions.assertTrue(journey.getIsDirect());
    }

    @Test
    void shouldHaveBeginAndEnd() {
        List<StageDTO> stages = createThreeStages();

        replayAll();
        JourneyDTO journey = factory.build(stages, queryTime, notes);
        verifyAll();

        assertEquals(Stations.Altrincham.getId(), journey.getBegin().getId());
        assertEquals(Stations.ExchangeSquare.getId(), journey.getEnd().getId());
    }

    @Test
    void shouldHaveRightSummaryAndHeadingFor4Stage() {
        List<StageDTO> stages = createThreeStages();
        stages.add(createStage(Stations.ExchangeSquare, TravelAction.Change, Stations.Rochdale, 24));

        replayAll();
        JourneyDTO journey = factory.build(stages, queryTime, notes);
        verifyAll();

        assertThat(journey.getChangeStations(), contains("Cornbrook", "Victoria", "Exchange Square"));
    }

    @Test
    void shouldHaveCorrectSummaryAndHeadingForSingleWalkingStage() {
        List<StageDTO> stages = new LinkedList<>();
        MyLocation myLocation = myLocationFactory.create(new LatLong(-1, 2));
        stages.add(createWalkingStage(myLocation, Stations.Victoria, TramTime.of(8,13), TramTime.of(8,16)));

        replayAll();
        JourneyDTO journey = factory.build(stages, queryTime, notes);
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
        JourneyDTO journey = factory.build(stages, queryTime, notes);
        verifyAll();

        assertThat(journey.getChangeStations(), contains("Deansgate-Castlefield", "Market Street"));
    }

    @Test
    void reproduceIssueWithJourneyWithJustWalking() {
        List<StageDTO> stages = new LinkedList<>();
        MyLocation start = myLocationFactory.create(new LatLong(1, 2));
        stages.add(createWalkingStage(start, Stations.Altrincham, TramTime.of(8,0), TramTime.of(8,10)));

        replayAll();
        JourneyDTO journey = factory.build(stages, queryTime, notes);
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
        return new StageDTO(new LocationDTO(firstStation), new LocationDTO(lastStation), new LocationDTO(firstStation),
                false, null, TramTime.of(10, 8), TramTime.of(10, 20), 20-8,
                "headSign", TransportMode.Tram,
                "cssClass", passedStops, "routeName", travelAction, "routeShortName");
    }

    private StageDTO createStage(TramTime departs, TramTime arrivesEnd, int passedStops) {
        return new StageDTO(new LocationDTO(stationA), new LocationDTO(stationB), new LocationDTO(stationA),
                false, null, departs, arrivesEnd, 42,
                "headSign", TransportMode.Tram,
                "cssClass", passedStops, "routeName", TravelAction.Board, "routeShortName");

    }

    private StageDTO createWalkingStage(Location start, Location destination, TramTime departs, TramTime arrivesEnd) {

        return new StageDTO(new LocationDTO(start), new LocationDTO(destination), new LocationDTO(stationA),
                false, null, departs, arrivesEnd, 9,
                "walking", TransportMode.Walk,
                "cssClass", 0, "walking", TravelAction.Board, "routeShortName");
    }

    private StageDTO createStageDTOWithDueTram(Station station, LocalDateTime whenTime, int wait) {
        StationDepartureInfo departureInfo = new StationDepartureInfo("displayId", "lineName",
                StationDepartureInfo.Direction.Incoming, "platform", station, "message", whenTime);

        LocalTime when = whenTime.toLocalTime();

        departureInfo.addDueTram(new DueTram(Stations.Deansgate, "Due", 10, "Single", when));
        departureInfo.addDueTram(new DueTram(station, "Departed", 0, "Single",when));
        departureInfo.addDueTram(new DueTram(station, "Due", wait, "Double",when));
        departureInfo.addDueTram(new DueTram(station, "Due", wait+2, "Double",when));

        PlatformDTO platform = new PlatformDTO(new Platform("platformId", "platformName"));
        platform.setDepartureInfo(new StationDepartureInfoDTO(departureInfo));

        int durationOfStage = 15;
        return new StageDTO(new LocationDTO(stationA),
                new LocationDTO(stationB), new LocationDTO(stationB),
                true, platform, TramTime.of(when.plusMinutes(1)),
                TramTime.of(when.plusMinutes(durationOfStage)),
                durationOfStage,
                station.getName(), TransportMode.Tram,
                "displayClass", 23,"routeName", TravelAction.Board, "routeShortName");
    }
}
