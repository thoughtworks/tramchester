package com.tramchester.unit.domain.presentation.DTO.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.tramchester.domain.*;
import com.tramchester.domain.presentation.*;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.factory.JourneyDTOFactory;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.integration.Stations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

public class JourneyDTOFactoryTest extends EasyMockSupport {
    private Location stationA = new Station("stationA", "area", "nameA", new LatLong(-2, -1), false);
    private Location stationB = new Station("stationB", "area", "nameB", new LatLong(-3, 1), false);
    private StageDTOFactory stageFactory;
    private JourneyDTOFactory factory;

    @Before
    public void beforeEachTestRuns() {
        stageFactory = createMock(StageDTOFactory.class);
        factory = new JourneyDTOFactory(stageFactory);
    }

    @Test
    public void shouldCreateJourneyDTODeprecated() {

        TransportStage transportStage = createStage(new LocalTime(10, 20));
        Journey journey = new Journey(Arrays.asList(transportStage));

        StageDTO stageDTO = new StageDTO();
        EasyMock.expect(stageFactory.build(transportStage)).andReturn(stageDTO);

        replayAll();
        JourneyDTO journeyDTO = factory.build(journey);
        verifyAll();

        assertEquals("Direct", journeyDTO.getSummary());
        assertEquals("Bus with No Changes - 12 minutes", journeyDTO.getHeading());
        assertEquals(new LocalTime(10, 20), journeyDTO.getExpectedArrivalTime());
        assertEquals(new LocalTime(10, 8), journeyDTO.getFirstDepartureTime());
        assertEquals(stationA.getId(), journeyDTO.getBegin().getId());
        assertEquals(stationB.getId(), journeyDTO.getEnd().getId());

        assertEquals(journey.getStages().size(),journeyDTO.getStages().size());
        assertEquals(stageDTO, journeyDTO.getStages().get(0));
    }

    @Test
    public void shouldHaveCorrectSummaryAndHeadingForWalkAndTram() {
        List<TransportStage> stages = new LinkedList<>();
        Location start = new MyLocation(new LatLong(-2,1));
        Location destination = Stations.Cornbrook;
        stages.add(new WalkingStage(new RawWalkingStage(start, destination, 3), 10*60));
        stages.add(createStage(Stations.Cornbrook, TravelAction.Change, Stations.Deansgate));

        EasyMock.expect(stageFactory.build(isA(TransportStage.class))).andStubReturn(new StageDTO());

        replayAll();
        JourneyDTO journey = factory.build(new Journey(stages));
        verifyAll();

        assertEquals("Direct", journey.getSummary());
        assertEquals("Walk and Tram with No Changes - 20 minutes", journey.getHeading());
    }

    @Test
    public void shouldHaveRightSummaryAndHeadingFor2Stage() {
        List<TransportStage> stages = new LinkedList<>();
        stages.add(createStage(Stations.Altrincham, TravelAction.Board, Stations.Cornbrook));
        stages.add(createStage(Stations.Cornbrook, TravelAction.Change, Stations.Deansgate));

        EasyMock.expect(stageFactory.build(isA(TransportStage.class))).andStubReturn(new StageDTO());

        replayAll();
        JourneyDTO journey = factory.build(new Journey(stages));
        verifyAll();

        assertEquals("Change at Cornbrook", journey.getSummary());
        assertEquals("Tram with 1 change - 12 minutes", journey.getHeading());
    }

    @Test
    public void shouldHaveRightSummaryAndHeadingFor3Stage() {
        List<TransportStage> stages = createThreeStages();

        EasyMock.expect(stageFactory.build(isA(TransportStage.class))).andStubReturn(new StageDTO());

        replayAll();
        JourneyDTO journey = factory.build(new Journey(stages));
        verifyAll();

        assertEquals("Change at Cornbrook and Victoria", journey.getSummary());
        assertEquals("Tram with 2 changes - 12 minutes", journey.getHeading());
    }

    @Test
    public void shouldHaveBeginAndEnd() {
        List<TransportStage> stages = createThreeStages();

        EasyMock.expect(stageFactory.build(isA(TransportStage.class))).andStubReturn(new StageDTO());

        replayAll();
        JourneyDTO journey = factory.build(new Journey(stages));
        verifyAll();

        assertEquals(Stations.Altrincham.getId(), journey.getBegin().getId());
        assertEquals(Stations.ExchangeSquare.getId(), journey.getEnd().getId());
    }

    @Test
    public void shouldHaveRightSummaryAndHeadingFor4Stage() {
        List<TransportStage> stages = createThreeStages();
        stages.add(createStage(Stations.ExchangeSquare, TravelAction.Change, Stations.Rochdale));

        EasyMock.expect(stageFactory.build(isA(TransportStage.class))).andStubReturn(new StageDTO());

        replayAll();
        JourneyDTO journey = factory.build(new Journey(stages));
        verifyAll();

        assertEquals("Change at Cornbrook, Victoria and Exchange Square", journey.getSummary());
        assertEquals("Tram with 3 changes - 12 minutes", journey.getHeading());
    }

    @Test
    public void shouldHaveCorrectSummaryAndHeadingForSingleWalkingStage() {
        List<TransportStage> stages = new LinkedList<>();
        MyLocation myLocation = new MyLocation(new LatLong(-1, 2));
        stages.add(new WalkingStage(new RawWalkingStage(myLocation, Stations.Victoria, 2), 8*60));

        EasyMock.expect(stageFactory.build(isA(TransportStage.class))).andStubReturn(new StageDTO());

        replayAll();
        JourneyDTO journey = factory.build(new Journey(stages));
        verifyAll();

        assertEquals("Direct", journey.getSummary());
        assertEquals("Walk with No Changes - 2 minutes", journey.getHeading());
    }

    @Test
    public void shouldHaveCorrectSummaryAndHeadingForTramStagesConnectedByWalk() {
        List<TransportStage> stages = new LinkedList<>();
        stages.add(createStage(Stations.ManAirport, TravelAction.Board, Stations.Deansgate));
        stages.add(new WalkingStage(new RawWalkingStage(Stations.Deansgate, Stations.MarketStreet, 14), 8*60));
        stages.add(createStage(Stations.MarketStreet, TravelAction.Change, Stations.Bury));

        EasyMock.expect(stageFactory.build(isA(TransportStage.class))).andStubReturn(new StageDTO());

        replayAll();
        JourneyDTO journey = factory.build(new Journey(stages));
        verifyAll();

        assertEquals("Change at Deansgate-Castlefield and Market Street", journey.getSummary());
        assertEquals("Tram and Walk with 2 changes - 12 minutes", journey.getHeading());
    }

    @Test
    public void reproduceIssueWithJourneyWithJustWalking() throws JsonProcessingException {
        List<TransportStage> stages = new LinkedList<>();
        MyLocation start = new MyLocation(new LatLong(1, 2));
        RawWalkingStage rawWalkingStage = new RawWalkingStage(start, Stations.Altrincham, 8*60);
        stages.add(new WalkingStage(rawWalkingStage, 8*60));

        EasyMock.expect(stageFactory.build(isA(TransportStage.class))).andStubReturn(new StageDTO());

        replayAll();
        JourneyDTO journey = factory.build(new Journey(stages));
        verifyAll();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());

        objectMapper.writeValueAsString(journey);
    }

    private List<TransportStage> createThreeStages() {
        List<TransportStage> stages = new LinkedList<>();
        stages.add(createStage(Stations.Altrincham, TravelAction.Board, Stations.Cornbrook));
        stages.add(createStage(Stations.Cornbrook, TravelAction.Change, Stations.Victoria));
        stages.add(createStage(Stations.Victoria, TravelAction.Change, Stations.ExchangeSquare));
        return stages;
    }

    private VehicleStageWithTiming createStage(Location firstStation, TravelAction travelAction, Location lastStation) {
        ServiceTime serviceTime = new ServiceTime(new LocalTime(10, 8), new LocalTime(10, 20), "svcId", "headSign", "tripId");
        RawVehicleStage rawVehicleStage = new RawVehicleStage(firstStation, "routeName", TransportMode.Tram, "cssClass");
        rawVehicleStage.setLastStation(lastStation);
        rawVehicleStage.setCost(20-8); // 12 mins
        return new VehicleStageWithTiming(rawVehicleStage, serviceTime, travelAction);
    }

    private TransportStage createStage(LocalTime arrivesEnd) {
        RawVehicleStage rawTravelStage = new RawVehicleStage(stationA, "routeName", TransportMode.Bus, "cssClass").
                setLastStation(stationB).setCost(42);
        ServiceTime serviceTime = new ServiceTime(new LocalTime(10, 8), arrivesEnd, "svcId",
                "headSign", "tripId");
        VehicleStageWithTiming vehicleStageWithTiming = new VehicleStageWithTiming(rawTravelStage, serviceTime, TravelAction.Board);
        return vehicleStageWithTiming;
    }
}
