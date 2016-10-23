package com.tramchester.domain.presentation;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.tramchester.Stations;
import com.tramchester.domain.*;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import org.junit.Test;

import org.joda.time.LocalTime;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JourneyTest {

    private Journey journeyA = new Journey(createStages(new LocalTime(10, 20)));
    private Location stationA = new Station("stationA", "area", "nameA", new LatLong(-2, -1), false);
    private Location stationB = new Station("stationB", "area", "nameA", new LatLong(-3, 1), false);

    @Test
    public void shouldHaveCorrectSummaryForDirect() {
        assertEquals("Direct", journeyA.getSummary());
    }

    @Test
    public void shouldHaveCorrectSummaryAndHeadingForWalkAndTram() {
        List<TransportStage> stages = new LinkedList<>();
        Location start = new MyLocation(new LatLong(-2,1));
        Location destination = Stations.Cornbrook;
        stages.add(new WalkingStage(new RawWalkingStage(start, destination, 3), 10*60));
        stages.add(createStage(Stations.Cornbrook, TravelAction.Change, Stations.Deansgate));

        Journey journey = new Journey(stages);

        assertEquals("Direct", journey.getSummary());
        assertEquals("Walk and Tram with No Changes - 20 minutes", journey.getHeading());
    }

    @Test
    public void shouldHaveRightSummaryAndHeadingFor2Stage() {
        List<TransportStage> stages = new LinkedList<>();
        stages.add(createStage(Stations.Altrincham, TravelAction.Board, Stations.Cornbrook));
        stages.add(createStage(Stations.Cornbrook, TravelAction.Change, Stations.Deansgate));

        Journey journey = new Journey(stages);

        assertEquals("Change at Cornbrook", journey.getSummary());
        assertEquals("Tram with 1 change - 12 minutes", journey.getHeading());
    }

    @Test
    public void shouldHaveRightSummaryAndHeadingFor3Stage() {
        List<TransportStage> stages = createThreeStages();

        Journey journey = new Journey(stages);

        assertEquals("Change at Cornbrook and Victoria", journey.getSummary());
        assertEquals("Tram with 2 changes - 12 minutes", journey.getHeading());
    }

    @Test
    public void shouldHaveBeginAndEnd() {
        List<TransportStage> stages = createThreeStages();
        Journey journey = new Journey(stages);

        assertEquals(Stations.Altrincham, journey.getBegin());
        assertEquals(Stations.ExchangeSquare, journey.getEnd());
    }

    @Test
    public void shouldHaveRightSummaryAndHeadingFor4Stage() {
        List<TransportStage> stages = createThreeStages();
        stages.add(createStage(Stations.ExchangeSquare, TravelAction.Change, Stations.Rochdale));

        Journey journey = new Journey(stages);

        assertEquals("Change at Cornbrook, Victoria and Exchange Square", journey.getSummary());
        assertEquals("Tram with 3 changes - 12 minutes", journey.getHeading());
    }

    @Test
    public void shouldHaveCorrectSummaryAndHeadingForSingleWalkingStage() {
        List<TransportStage> stages = new LinkedList<>();
        MyLocation myLocation = new MyLocation(new LatLong(-1, 2));
        stages.add(new WalkingStage(new RawWalkingStage(myLocation, Stations.Victoria, 2), 8*60));

        Journey journey = new Journey(stages);

        assertEquals("Direct", journey.getSummary());
        assertEquals("Walk with No Changes - 2 minutes", journey.getHeading());
    }

    @Test
    public void shouldHaveCorrectSummaryAndHeadingForTramStagesConnectedByWalk() {
        List<TransportStage> stages = new LinkedList<>();
        stages.add(createStage(Stations.ManAirport, TravelAction.Board, Stations.Deansgate));
        stages.add(new WalkingStage(new RawWalkingStage(Stations.Deansgate, Stations.MarketStreet, 14), 8*60));
        stages.add(createStage(Stations.MarketStreet, TravelAction.Change, Stations.Bury));

        Journey journey = new Journey(stages);

        assertEquals("Change at Deansgate-Castlefield and Market Street", journey.getSummary());
        assertEquals("Tram and Walk with 2 changes - 12 minutes", journey.getHeading());
    }

    @Test
    public void reproduceIssueWithJourneyWithJustWalking() throws JsonProcessingException {
        List<TransportStage> stages = new LinkedList<>();
        MyLocation start = new MyLocation(new LatLong(1, 2));
        RawWalkingStage rawWalkingStage = new RawWalkingStage(start, Stations.Altrincham, 8*60);
        stages.add(new WalkingStage(rawWalkingStage, 8*60));
        Journey journey = new Journey(stages);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());

        objectMapper.writeValueAsString(journey);
    }

    @Test
    public void shouldCreateJourneyDTO() {
        List<TransportStage> stages = new LinkedList<>();
        stages.add(createStage(Stations.ManAirport, TravelAction.Board, Stations.Deansgate));
        Journey journey = new Journey(stages);

        JourneyDTO dto = journey.asDTO();

        assertEquals(journey.getExpectedArrivalTime(), dto.getExpectedArrivalTime());
        assertEquals(journey.getBegin().getId(), dto.getBegin().getId());
        assertEquals(journey.getFirstDepartureTime(), dto.getFirstDepartureTime());
        assertEquals(journey.getEnd().getId(), dto.getEnd().getId());
        assertEquals(journey.getHeading(), dto.getHeading());
        assertEquals(journey.getSummary(), dto.getSummary());
        assertEquals(journey.getStages().size(), dto.getStages().size()); // see also asDTO tests for stages
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

    private List<TransportStage> createStages(LocalTime arrivesEnd) {
        List<TransportStage> stages = new LinkedList<>();
        RawVehicleStage rawTravelStage = new RawVehicleStage(stationA, "routeName", TransportMode.Bus, "cssClass").
                setLastStation(stationB).setCost(42);
        ServiceTime serviceTime = new ServiceTime(new LocalTime(10, 8), arrivesEnd, "svcId", "headSign", "tripId");
        stages.add(new VehicleStageWithTiming(rawTravelStage, serviceTime, TravelAction.Board));
        return stages;
    }
}
