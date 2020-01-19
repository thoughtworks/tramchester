package com.tramchester.unit.domain.presentation.DTO.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.*;
import com.tramchester.domain.presentation.DTO.*;
import com.tramchester.domain.presentation.DTO.factory.JourneyDTOFactory;
import com.tramchester.integration.Stations;
import com.tramchester.mappers.HeadsignMapper;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.*;

public class JourneyDTOFactoryTest extends EasyMockSupport {
    private Location stationA = new Station("stationA", "area", "nameA", new LatLong(-2, -1), false);
    private Location stationB = new Station("stationB", "area", "nameB", new LatLong(-3, 1), false);
    private JourneyDTOFactory factory;
    private MyLocationFactory myLocationFactory;

    @Before
    public void beforeEachTestRuns() {
        myLocationFactory = new MyLocationFactory(new ObjectMapper());
        factory = new JourneyDTOFactory(new HeadsignMapper());
    }

    @Test
    public void shouldCreateJourneyDTO() {

        StageDTO transportStage = createStage(TramTime.of(10, 8), TramTime.of(10, 20), 11);

        replayAll();
        JourneyDTO journeyDTO = factory.build(Collections.singletonList(transportStage));
        verifyAll();

        assertEquals(TramTime.of(10, 20), journeyDTO.getExpectedArrivalTime());
        assertEquals(TramTime.of(10, 8), journeyDTO.getFirstDepartureTime());
        assertEquals(stationA.getId(), journeyDTO.getBegin().getId());
        assertEquals(stationB.getId(), journeyDTO.getEnd().getId());

        assertEquals(1,journeyDTO.getStages().size());
        assertEquals(transportStage, journeyDTO.getStages().get(0));
    }

    @Test
    public void shouldCreateJourneyDTOWithDueTram() {
        LocalDateTime when = LocalDateTime.of(2017,11,30,18,41);

        StageDTO stageDTO = createStageDTOWithDueTram(Stations.Cornbrook, when, 5);

        replayAll();
        JourneyDTO journeyDTO = factory.build(Collections.singletonList(stageDTO));
        verifyAll();

        assertEquals("Double tram Due at 18:46", journeyDTO.getDueTram());
    }

    @Test
    public void shouldCreateJourneyDTOWithDueTramTimeOutOfRange() {

        LocalDateTime dateTime = LocalDateTime.of(2017,11,30,18,41);
        StageDTO stageDTO = createStageDTOWithDueTram(Stations.Cornbrook, dateTime, 22);

        replayAll();
        JourneyDTO journeyDTO = factory.build(Collections.singletonList(stageDTO));
        verifyAll();

        assertNull(journeyDTO.getDueTram());
    }

    @Test
    public void shouldCreateJourneyDTOWithLaterDueTramMatching() {
        LocalDateTime when = LocalDateTime.of(2017,11,30, 18,41);
        StageDTO stageDTO = createStageDTOWithDueTram(Stations.Cornbrook, when, 7);

        replayAll();
        JourneyDTO journeyDTO = factory.build(Collections.singletonList(stageDTO));
        verifyAll();

        // select due tram that is closer to stage departure when more than one available
        assertEquals("Double tram Due at 18:48", journeyDTO.getDueTram());
    }

    @Test
    public void shouldHaveCorrectSummaryAndHeadingForWalkAndTram() {
        List<StageDTO> stages = new LinkedList<>();
        Location start = myLocationFactory.create(new LatLong(-2,1));
        Location destination = Stations.Cornbrook;
        stages.add(createWalkingStage(start, destination, TramTime.of(8,13), TramTime.of(8,16)));

        stages.add(createStage(Stations.Cornbrook, TravelAction.Change, Stations.Deansgate, 1));

        replayAll();
        JourneyDTO journey = factory.build(stages);
        verifyAll();

        assertTrue(journey.getChangeStations().isEmpty());
        assertTrue(journey.getIsDirect());
    }

    @Test
    public void shouldHaveRightSummaryAndHeadingFor2Stage() {
        List<StageDTO> stages = new LinkedList<>();
        stages.add(createStage(Stations.Altrincham, TravelAction.Board, Stations.Cornbrook, 9));
        stages.add(createStage(Stations.Cornbrook, TravelAction.Change, Stations.Deansgate, 1));

        replayAll();
        JourneyDTO journey = factory.build(stages);
        verifyAll();

        assertThat(journey.getChangeStations(), contains("Cornbrook"));
        assertFalse(journey.getIsDirect());
    }

    @Test
    public void shouldHaveRightSummaryAndHeadingFor3Stage() {
        List<StageDTO> stages = createThreeStages();

        replayAll();
        JourneyDTO journey = factory.build(stages);
        verifyAll();

        assertThat(journey.getChangeStations(), contains("Cornbrook", "Victoria"));
    }

    @Test
    public void shouldHaveBeginAndEnd() {
        List<StageDTO> stages = createThreeStages();

        replayAll();
        JourneyDTO journey = factory.build(stages);
        verifyAll();

        assertEquals(Stations.Altrincham.getId(), journey.getBegin().getId());
        assertEquals(Stations.ExchangeSquare.getId(), journey.getEnd().getId());
    }

    @Test
    public void shouldHaveRightSummaryAndHeadingFor4Stage() {
        List<StageDTO> stages = createThreeStages();
        stages.add(createStage(Stations.ExchangeSquare, TravelAction.Change, Stations.Rochdale, 24));

        replayAll();
        JourneyDTO journey = factory.build(stages);
        verifyAll();

        assertThat(journey.getChangeStations(), contains("Cornbrook", "Victoria", "Exchange Square"));
    }

    @Test
    public void shouldHaveCorrectSummaryAndHeadingForSingleWalkingStage() {
        List<StageDTO> stages = new LinkedList<>();
        MyLocation myLocation = myLocationFactory.create(new LatLong(-1, 2));
        stages.add(createWalkingStage(myLocation, Stations.Victoria, TramTime.of(8,13), TramTime.of(8,16)));

        replayAll();
        JourneyDTO journey = factory.build(stages);
        verifyAll();

        assertTrue(journey.getChangeStations().isEmpty());
    }

    @Test
    public void shouldHaveCorrectSummaryAndHeadingForTramStagesConnectedByWalk() {
        List<StageDTO> stages = new LinkedList<>();
        stages.add(createStage(Stations.ManAirport, TravelAction.Board, Stations.Deansgate, 13));
        stages.add(createWalkingStage(Stations.Deansgate, Stations.MarketStreet, TramTime.of(8,13), TramTime.of(8,16)));
        stages.add(createStage(Stations.MarketStreet, TravelAction.Change, Stations.Bury, 16));

        replayAll();
        JourneyDTO journey = factory.build(stages);
        verifyAll();

        assertThat(journey.getChangeStations(), contains("Deansgate-Castlefield", "Market Street"));
    }

    @Test
    public void reproduceIssueWithJourneyWithJustWalking() throws JsonProcessingException {
        List<StageDTO> stages = new LinkedList<>();
        MyLocation start = myLocationFactory.create(new LatLong(1, 2));
        stages.add(createWalkingStage(start, Stations.Altrincham, TramTime.of(8,0), TramTime.of(8,10)));

        replayAll();
        JourneyDTO journey = factory.build(stages);
        verifyAll();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());

        objectMapper.writeValueAsString(journey);
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
                "cssClass", passedStops, "routeName", travelAction);
    }

    private StageDTO createStage(TramTime departs, TramTime arrivesEnd, int passedStops) {
        return new StageDTO(new LocationDTO(stationA), new LocationDTO(stationB), new LocationDTO(stationA),
                false, null, departs, arrivesEnd, 42,
                "headSign", TransportMode.Tram,
                "cssClass", passedStops, "routeName", TravelAction.Board);

    }

    private StageDTO createWalkingStage(Location start, Location destination, TramTime departs, TramTime arrivesEnd) {

        return new StageDTO(new LocationDTO(start), new LocationDTO(destination), new LocationDTO(stationA),
                false, null, departs, arrivesEnd, 9,
                "walking", TransportMode.Walk,
                "cssClass", 0, "walking", TravelAction.Board);
    }

    private StageDTO createStageDTOWithDueTram(Station station, LocalDateTime whenTime, int wait) {
        StationDepartureInfo departureInfo = new StationDepartureInfo("displayId", "lineName",
                StationDepartureInfo.Direction.Incoming, "platform", "platformLocation", "message", whenTime);

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
                "displayClass", 23,"routeName", TravelAction.Board);
    }
}
