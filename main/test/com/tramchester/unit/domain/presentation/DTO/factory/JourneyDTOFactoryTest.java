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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class JourneyDTOFactoryTest extends EasyMockSupport {
    private Location stationA = new Station("stationA", "area", "nameA", new LatLong(-2, -1), false);
    private Location stationB = new Station("stationB", "area", "nameB", new LatLong(-3, 1), false);
    private StageDTOFactory stageFactory;
    private JourneyDTOFactory factory;

    @Before
    public void beforeEachTestRuns() {
        stageFactory = createMock(StageDTOFactory.class);
        factory = new JourneyDTOFactory(stageFactory, new HeadsignMapper());
    }

    @Test
    public void shouldCreateJourneyDTO() throws TramchesterException {

        TransportStage transportStage = createStage(TramTime.create(10, 8), TramTime.create(10, 20), 11);
        Journey journey = new Journey(Arrays.asList(transportStage));

        StageDTO stageDTO = new StageDTO();
        EasyMock.expect(stageFactory.build(transportStage)).andReturn(stageDTO);

        replayAll();
        JourneyDTO journeyDTO = factory.build(journey);
        verifyAll();

        assertEquals("Direct", journeyDTO.getSummary());
        assertEquals("Bus with No Changes - 12 minutes", journeyDTO.getHeading());
        assertEquals(TramTime.create(10, 20), journeyDTO.getExpectedArrivalTime());
        assertEquals(TramTime.create(10, 8), journeyDTO.getFirstDepartureTime());
        assertEquals(stationA.getId(), journeyDTO.getBegin().getId());
        assertEquals(stationB.getId(), journeyDTO.getEnd().getId());

        assertEquals(journey.getStages().size(),journeyDTO.getStages().size());
        assertEquals(stageDTO, journeyDTO.getStages().get(0));
    }

    @Test
    public void shouldCreateJourneyDTOWithDueTram() throws TramchesterException {
        LocalDateTime when = LocalDateTime.of(2017,11,30,18,41);

        TransportStage transportStage = createStage(TramTime.create(18,42), TramTime.create(19, 00), 11);
        Journey journey = new Journey(Arrays.asList(transportStage));

        StageDTO stageDTO = createStageDTOWithDueTram("headSign", when, 5);

        EasyMock.expect(stageFactory.build(transportStage)).andReturn(stageDTO);

        replayAll();
        JourneyDTO journeyDTO = factory.build(journey);
        verifyAll();

        assertEquals("Double tram Due at 18:46", journeyDTO.getDueTram());
    }

    @Test
    public void shouldCreateJourneyDTOWithDueTramTimeOutOfRange() throws TramchesterException {

        TransportStage transportStage = createStage(TramTime.create(10, 8), TramTime.create(10, 20), 22);
        Journey journey = new Journey(Arrays.asList(transportStage));

        LocalDateTime dateTime = LocalDateTime.of(2017,11,30,18,41);
        StageDTO stageDTO = createStageDTOWithDueTram("headSign", dateTime, 15);

        EasyMock.expect(stageFactory.build(transportStage)).andReturn(stageDTO);

        replayAll();
        JourneyDTO journeyDTO = factory.build(journey);
        verifyAll();

        assertNull(journeyDTO.getDueTram());
    }

    @Test
    public void shouldCreateJourneyDTOWithLaterDueTramMatching() throws TramchesterException {
        LocalDateTime when = LocalDateTime.of(2017,11,30,18,41);

        TransportStage transportStage = createStage(TramTime.create(18,50), TramTime.create(19, 00), 23);
        Journey journey = new Journey(Arrays.asList(transportStage));

        StageDTO stageDTO = createStageDTOWithDueTram("headSign", when, 5);

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
        Location start = new MyLocation(new LatLong(-2,1));
        Location destination = Stations.Cornbrook;
        stages.add(new WalkingStage(new RawWalkingStage(start, destination, 3), java.time.LocalTime.of(10,00)));
        stages.add(createStage(Stations.Cornbrook, TravelAction.Change, Stations.Deansgate, 1));

        EasyMock.expect(stageFactory.build(isA(TransportStage.class))).andStubReturn(new StageDTO());

        replayAll();
        JourneyDTO journey = factory.build(new Journey(stages));
        verifyAll();

        assertEquals("Direct", journey.getSummary());
        assertTrue(journey.getIsDirect());
        assertEquals("Walk and Tram with No Changes - 20 minutes", journey.getHeading());
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

        assertEquals("Change at Cornbrook", journey.getSummary());
        assertFalse(journey.getIsDirect());
        assertEquals("Tram with 1 change - 12 minutes", journey.getHeading());
    }

    @Test
    public void shouldHaveRightSummaryAndHeadingFor3Stage() throws TramchesterException {
        List<TransportStage> stages = createThreeStages();

        EasyMock.expect(stageFactory.build(isA(TransportStage.class))).andStubReturn(new StageDTO());

        replayAll();
        JourneyDTO journey = factory.build(new Journey(stages));
        verifyAll();

        assertEquals("Change at Cornbrook and Victoria", journey.getSummary());
        assertEquals("Tram with 2 changes - 12 minutes", journey.getHeading());
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

        assertEquals("Change at Cornbrook, Victoria and Exchange Square", journey.getSummary());
        assertEquals("Tram with 3 changes - 12 minutes", journey.getHeading());
    }

    @Test
    public void shouldHaveCorrectSummaryAndHeadingForSingleWalkingStage() throws TramchesterException {
        List<TransportStage> stages = new LinkedList<>();
        MyLocation myLocation = new MyLocation(new LatLong(-1, 2));
        stages.add(new WalkingStage(new RawWalkingStage(myLocation, Stations.Victoria, 2), LocalTime.of(10,00)));

        EasyMock.expect(stageFactory.build(isA(TransportStage.class))).andStubReturn(new StageDTO());

        replayAll();
        JourneyDTO journey = factory.build(new Journey(stages));
        verifyAll();

        assertEquals("Direct", journey.getSummary());
        assertEquals("Walk with No Changes - 2 minutes", journey.getHeading());
    }

    @Test
    public void shouldHaveCorrectSummaryAndHeadingForTramStagesConnectedByWalk() throws TramchesterException {
        List<TransportStage> stages = new LinkedList<>();
        stages.add(createStage(Stations.ManAirport, TravelAction.Board, Stations.Deansgate, 13));
        stages.add(new WalkingStage(new RawWalkingStage(Stations.Deansgate, Stations.MarketStreet, 14), LocalTime.of(8,0)));
        stages.add(createStage(Stations.MarketStreet, TravelAction.Change, Stations.Bury, 16));

        EasyMock.expect(stageFactory.build(isA(TransportStage.class))).andStubReturn(new StageDTO());

        replayAll();
        JourneyDTO journey = factory.build(new Journey(stages));
        verifyAll();

        assertEquals("Change at Deansgate-Castlefield and Market Street", journey.getSummary());
        assertEquals("Tram and Walk with 2 changes - 12 minutes", journey.getHeading());
    }

    @Test
    public void reproduceIssueWithJourneyWithJustWalking() throws JsonProcessingException, TramchesterException {
        List<TransportStage> stages = new LinkedList<>();
        MyLocation start = new MyLocation(new LatLong(1, 2));
        RawWalkingStage rawWalkingStage = new RawWalkingStage(start, Stations.Altrincham, 8*60);
        stages.add(new WalkingStage(rawWalkingStage, LocalTime.of(8,0)));

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
        ServiceTime serviceTime = new ServiceTime(TramTime.create(10, 8), TramTime.create(10, 20), "svcId", "headSign", "tripId");
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
        VehicleStageWithTiming vehicleStageWithTiming = new VehicleStageWithTiming(rawTravelStage, serviceTime, TravelAction.Board);
        return vehicleStageWithTiming;
    }

    private StageDTO createStageDTOWithDueTram(String matchingHeadsign, LocalDateTime whenTime, int wait) throws TramchesterException {
        StationDepartureInfo departureInfo = new StationDepartureInfo("displayId", "lineName",
                "platform", "platformLocation", "message", whenTime);

        LocalTime when = whenTime.toLocalTime();

        departureInfo.addDueTram(new DueTram("other", "Due", 10, "Single", when));
        departureInfo.addDueTram(new DueTram(matchingHeadsign, "Departed", 0, "Single",when));
        departureInfo.addDueTram(new DueTram(matchingHeadsign, "Due", wait, "Double",when));
        departureInfo.addDueTram(new DueTram(matchingHeadsign, "Due", wait+2, "Double",when));

        PlatformDTO platform = new PlatformDTO(new Platform("platformId", "platformName"));
        platform.setDepartureInfo(departureInfo);

        int durationOfStage = 15;
        return new StageDTO(new LocationDTO(stationA),
                new LocationDTO(stationB), new LocationDTO(stationB),
                true, platform, TramTime.of(when.plusMinutes(1)),
                TramTime.of(when.plusMinutes(durationOfStage)),
                durationOfStage, "summary", "prompt",
                matchingHeadsign, TransportMode.Tram,
                false, true, "displayClass", 23);
    }
}
