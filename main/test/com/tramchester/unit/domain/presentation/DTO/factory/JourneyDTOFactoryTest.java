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
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.integration.Stations;
import com.tramchester.mappers.HeadsignMapper;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.easymock.EasyMock.isA;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.*;

public class JourneyDTOFactoryTest extends EasyMockSupport {
    private Location stationA = new Station("stationA", "area", "nameA", new LatLong(-2, -1), false);
    private Location stationB = new Station("stationB", "area", "nameB", new LatLong(-3, 1), false);
    private StageDTOFactory stageFactory;
    private JourneyDTOFactory factory;
    private MyLocationFactory myLocationFactory;

    @Before
    public void beforeEachTestRuns() {
        myLocationFactory = new MyLocationFactory(new ObjectMapper());
        stageFactory = createMock(StageDTOFactory.class);
        factory = new JourneyDTOFactory(stageFactory, new HeadsignMapper());
    }

    @Test
    public void shouldCreateJourneyDTO() throws TramchesterException {

        TransportStage transportStage = createStage(TramTime.of(10, 8), TramTime.of(10, 20), 11);
        Journey journey = new Journey(Arrays.asList(transportStage));

        StageDTO stageDTO = new StageDTO();
        EasyMock.expect(stageFactory.build(transportStage)).andReturn(stageDTO);

        replayAll();
        JourneyDTO journeyDTO = factory.build(journey);
        verifyAll();

        assertEquals(TramTime.of(10, 20), journeyDTO.getExpectedArrivalTime());
        assertEquals(TramTime.of(10, 8), journeyDTO.getFirstDepartureTime());
        assertEquals(stationA.getId(), journeyDTO.getBegin().getId());
        assertEquals(stationB.getId(), journeyDTO.getEnd().getId());

        assertEquals(journey.getStages().size(),journeyDTO.getStages().size());
        assertEquals(stageDTO, journeyDTO.getStages().get(0));
    }

    @Test
    public void shouldCreateJourneyDTOWithDueTram() throws TramchesterException {
        LocalDateTime when = LocalDateTime.of(2017,11,30,18,41);

        TransportStage transportStage = createStage(TramTime.of(18,42), TramTime.of(19, 00), 11);
        Journey journey = new Journey(Arrays.asList(transportStage));

        StageDTO stageDTO = createStageDTOWithDueTram(Stations.Cornbrook, when, 5);

        EasyMock.expect(stageFactory.build(transportStage)).andReturn(stageDTO);

        replayAll();
        JourneyDTO journeyDTO = factory.build(journey);
        verifyAll();

        assertEquals("Double tram Due at 18:46", journeyDTO.getDueTram());
    }

    @Test
    public void shouldCreateJourneyDTOWithDueTramTimeOutOfRange() throws TramchesterException {

        TransportStage transportStage = createStage(TramTime.of(10, 8), TramTime.of(10, 20), 22);
        Journey journey = new Journey(Arrays.asList(transportStage));

        LocalDateTime dateTime = LocalDateTime.of(2017,11,30,18,41);
        StageDTO stageDTO = createStageDTOWithDueTram(Stations.Cornbrook, dateTime, 15);

        EasyMock.expect(stageFactory.build(transportStage)).andReturn(stageDTO);

        replayAll();
        JourneyDTO journeyDTO = factory.build(journey);
        verifyAll();

        assertNull(journeyDTO.getDueTram());
    }

    @Test
    public void shouldCreateJourneyDTOWithLaterDueTramMatching() throws TramchesterException {
        LocalDateTime when = LocalDateTime.of(2017,11,30,18,41);

        TransportStage transportStage = createStage(TramTime.of(18,50), TramTime.of(19, 00), 23);
        Journey journey = new Journey(Arrays.asList(transportStage));

        StageDTO stageDTO = createStageDTOWithDueTram(Stations.Cornbrook, when, 5);

        EasyMock.expect(stageFactory.build(transportStage)).andReturn(stageDTO);

        replayAll();
        JourneyDTO journeyDTO = factory.build(journey);
        verifyAll();

        // select due tram that is closer to stage departure when more than one available
        assertEquals("Double tram Due at 18:48", journeyDTO.getDueTram());
    }

    @Test
    public void shouldHaveCorrectSummaryAndHeadingForWalkAndTram() throws TramchesterException {
        List<TransportStage> stages = new LinkedList<>();
        Location start = myLocationFactory.create(new LatLong(-2,1));
        Location destination = Stations.Cornbrook;
        stages.add(new WalkingStage(new RawWalkingStage(start, destination, 3), TramTime.of(10,00)));
        stages.add(createStage(Stations.Cornbrook, TravelAction.Change, Stations.Deansgate, 1));

        EasyMock.expect(stageFactory.build(isA(TransportStage.class))).andStubReturn(new StageDTO());

        replayAll();
        JourneyDTO journey = factory.build(new Journey(stages));
        verifyAll();

        assertTrue(journey.getChangeStations().isEmpty());
        assertTrue(journey.getIsDirect());
    }

    @Test
    public void shouldHaveRightSummaryAndHeadingFor2Stage() throws TramchesterException {
        List<TransportStage> stages = new LinkedList<>();
        stages.add(createStage(Stations.Altrincham, TravelAction.Board, Stations.Cornbrook, 9));
        stages.add(createStage(Stations.Cornbrook, TravelAction.Change, Stations.Deansgate, 1));

        EasyMock.expect(stageFactory.build(isA(TransportStage.class))).andStubReturn(new StageDTO());

        replayAll();
        JourneyDTO journey = factory.build(new Journey(stages));
        verifyAll();

        assertThat(journey.getChangeStations(), contains("Cornbrook"));
        assertFalse(journey.getIsDirect());
    }

    @Test
    public void shouldHaveRightSummaryAndHeadingFor3Stage() throws TramchesterException {
        List<TransportStage> stages = createThreeStages();

        EasyMock.expect(stageFactory.build(isA(TransportStage.class))).andStubReturn(new StageDTO());

        replayAll();
        JourneyDTO journey = factory.build(new Journey(stages));
        verifyAll();

        assertThat(journey.getChangeStations(), contains("Cornbrook", "Victoria"));
    }

    @Test
    public void shouldHaveBeginAndEnd() throws TramchesterException {
        List<TransportStage> stages = createThreeStages();

        EasyMock.expect(stageFactory.build(isA(TransportStage.class))).andStubReturn(new StageDTO());

        replayAll();
        JourneyDTO journey = factory.build(new Journey(stages));
        verifyAll();

        assertEquals(Stations.Altrincham.getId(), journey.getBegin().getId());
        assertEquals(Stations.ExchangeSquare.getId(), journey.getEnd().getId());
    }

    @Test
    public void shouldHaveRightSummaryAndHeadingFor4Stage() throws TramchesterException {
        List<TransportStage> stages = createThreeStages();
        stages.add(createStage(Stations.ExchangeSquare, TravelAction.Change, Stations.Rochdale, 24));

        EasyMock.expect(stageFactory.build(isA(TransportStage.class))).andStubReturn(new StageDTO());

        replayAll();
        JourneyDTO journey = factory.build(new Journey(stages));
        verifyAll();

        assertThat(journey.getChangeStations(), contains("Cornbrook", "Victoria", "Exchange Square"));
    }

    @Test
    public void shouldHaveCorrectSummaryAndHeadingForSingleWalkingStage() throws TramchesterException {
        List<TransportStage> stages = new LinkedList<>();
        MyLocation myLocation = myLocationFactory.create(new LatLong(-1, 2));
        stages.add(new WalkingStage(new RawWalkingStage(myLocation, Stations.Victoria, 2), TramTime.of(10,00)));

        EasyMock.expect(stageFactory.build(isA(TransportStage.class))).andStubReturn(new StageDTO());

        replayAll();
        JourneyDTO journey = factory.build(new Journey(stages));
        verifyAll();

        assertTrue(journey.getChangeStations().isEmpty());
    }

    @Test
    public void shouldHaveCorrectSummaryAndHeadingForTramStagesConnectedByWalk() throws TramchesterException {
        List<TransportStage> stages = new LinkedList<>();
        stages.add(createStage(Stations.ManAirport, TravelAction.Board, Stations.Deansgate, 13));
        stages.add(new WalkingStage(new RawWalkingStage(Stations.Deansgate, Stations.MarketStreet, 14), TramTime.of(8,0)));
        stages.add(createStage(Stations.MarketStreet, TravelAction.Change, Stations.Bury, 16));

        EasyMock.expect(stageFactory.build(isA(TransportStage.class))).andStubReturn(new StageDTO());

        replayAll();
        JourneyDTO journey = factory.build(new Journey(stages));
        verifyAll();

        assertThat(journey.getChangeStations(), contains("Deansgate-Castlefield", "Market Street"));
    }

    @Test
    public void reproduceIssueWithJourneyWithJustWalking() throws JsonProcessingException, TramchesterException {
        List<TransportStage> stages = new LinkedList<>();
        MyLocation start = myLocationFactory.create(new LatLong(1, 2));
        RawWalkingStage rawWalkingStage = new RawWalkingStage(start, Stations.Altrincham, 8*60);
        stages.add(new WalkingStage(rawWalkingStage, TramTime.of(8,0)));

        EasyMock.expect(stageFactory.build(isA(TransportStage.class))).andStubReturn(new StageDTO());

        replayAll();
        JourneyDTO journey = factory.build(new Journey(stages));
        verifyAll();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());

        objectMapper.writeValueAsString(journey);
    }

    private List<TransportStage> createThreeStages() throws TramchesterException {
        List<TransportStage> stages = new LinkedList<>();
        stages.add(createStage(Stations.Altrincham, TravelAction.Board, Stations.Cornbrook, 7));
        stages.add(createStage(Stations.Cornbrook, TravelAction.Change, Stations.Victoria, 3));
        stages.add(createStage(Stations.Victoria, TravelAction.Change, Stations.ExchangeSquare, 13));
        return stages;
    }

    private VehicleStageWithTiming createStage(Location firstStation, TravelAction travelAction, Location lastStation, int passedStops) throws TramchesterException {
        ServiceTime serviceTime = new ServiceTime(TramTime.of(10, 8), TramTime.of(10, 20), "svcId", "headSign", "tripId");
        RawVehicleStage rawVehicleStage = new RawVehicleStage(firstStation, "routeName", TransportMode.Tram, "cssClass");
        rawVehicleStage.setLastStation(lastStation,passedStops);
        rawVehicleStage.setCost(20-8); // 12 mins
        return new VehicleStageWithTiming(rawVehicleStage, serviceTime, travelAction);
    }

    private TransportStage createStage(TramTime departs, TramTime arrivesEnd, int passedStops) {
        RawVehicleStage rawTravelStage = new RawVehicleStage(stationA, "routeName", TransportMode.Bus, "cssClass").
                setLastStation(stationB, passedStops).setCost(42);
        ServiceTime serviceTime = new ServiceTime(departs, arrivesEnd, "svcId",
                "headSign", "tripId");
        return new VehicleStageWithTiming(rawTravelStage, serviceTime, TravelAction.Board);
    }

    private StageDTO createStageDTOWithDueTram(Station station, LocalDateTime whenTime, int wait) {
        StationDepartureInfo departureInfo = new StationDepartureInfo("displayId", "lineName",
                "platform", "platformLocation", "message", whenTime);

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
