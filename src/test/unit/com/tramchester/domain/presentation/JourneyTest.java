package com.tramchester.domain.presentation;


import com.tramchester.Stations;
import com.tramchester.domain.*;
import org.junit.Ignore;
import org.junit.Test;

import org.joda.time.LocalTime;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JourneyTest {

    private Journey journeyA = new Journey(createStages(new LocalTime(10, 20)));
    private Journey journeyB = new Journey(createStages(new LocalTime(10, 25)));
    private Location stationA = new Station("stationA", "area", "nameA", new LatLong(-2, -1), false);
    private Location stationB = new Station("stationB", "area", "nameA", new LatLong(-3, 1), false);

    @Test
    public void shouldCompareJourneysBasedOnEarliestArrival() {
        assertTrue(journeyA.compareTo(journeyB)<0);
        assertTrue(journeyB.compareTo(journeyA)>0);
    }

    @Test
    public void shouldHaveSortedSetInExpectedOrder() {
        SortedSet<Journey> set = new TreeSet<>();
        set.add(journeyB);
        set.add(journeyA);
        assertEquals(new LocalTime(10,20), set.first().getExpectedArrivalTime());
    }

    @Test
    public void shouldHaveSortedSetInExpectedOrderAccrossMidnight() {
        SortedSet<Journey> set = new TreeSet<>();
        set.add(new Journey(createStages(new LocalTime(00, 10))));
        set.add(new Journey(createStages(new LocalTime(23, 50))));

        assertEquals(new LocalTime(23,50), set.first().getExpectedArrivalTime());
        assertEquals(new LocalTime(23,50), set.stream().findFirst().get().getExpectedArrivalTime());
    }

    @Test
    public void shouldHaveCorrectSummaryForDirect() {
        assertEquals("Direct", journeyA.getSummary());
    }

    @Test
    public void shouldHaveCorrectSummaryAndHeadingForWalkAndTram() {
        List<PresentableStage> stages = new LinkedList<>();
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
        List<PresentableStage> stages = new LinkedList<>();
        stages.add(createStage(Stations.Altrincham, TravelAction.Board, Stations.Cornbrook));
        stages.add(createStage(Stations.Cornbrook, TravelAction.Change, Stations.Deansgate));

        Journey journey = new Journey(stages);

        assertEquals("Change at Cornbrook", journey.getSummary());
        assertEquals("Tram with 1 change - 12 minutes", journey.getHeading());
    }

    @Test
    public void shouldHaveRightSummaryAndHeadingFor3Stage() {
        List<PresentableStage> stages = createThreeStages();

        Journey journey = new Journey(stages);

        assertEquals("Change at Cornbrook and Victoria", journey.getSummary());
        assertEquals("Tram with 2 changes - 12 minutes", journey.getHeading());
    }

    @Test
    public void shouldHaveBeginAndEnd() {
        List<PresentableStage> stages = createThreeStages();
        Journey journey = new Journey(stages);

        assertEquals(Stations.Altrincham, journey.getBegin());
        assertEquals(Stations.ExchangeSquare, journey.getEnd());
    }

    @Test
    public void shouldHaveRightSummaryAndHeadingFor4Stage() {
        List<PresentableStage> stages = createThreeStages();
        stages.add(createStage(Stations.ExchangeSquare, TravelAction.Change, Stations.Rochdale));

        Journey journey = new Journey(stages);

        assertEquals("Change at Cornbrook, Victoria and Exchange Square", journey.getSummary());
        assertEquals("Tram with 3 changes - 12 minutes", journey.getHeading());
    }

    @Test
    public void shouldHaveCorrectSummaryAndHeadingForSingleWalkingStage() {
        List<PresentableStage> stages = new LinkedList<>();
        MyLocation myLocation = new MyLocation(new LatLong(-1, 2));
        stages.add(new WalkingStage(new RawWalkingStage(myLocation, Stations.Victoria, 2), 8*60));

        Journey journey = new Journey(stages);

        assertEquals("Direct", journey.getSummary());
        assertEquals("Walk with No Changes - 2 minutes", journey.getHeading());
    }

    @Test
    public void shouldHaveCorrectSummaryAndHeadingForTramStagesConnectedByWalk() {
        List<PresentableStage> stages = new LinkedList<>();
        stages.add(createStage(Stations.ManAirport, TravelAction.Board, Stations.Deansgate));
        stages.add(new WalkingStage(new RawWalkingStage(Stations.Deansgate, Stations.MarketStreet, 14), 8*60));
        stages.add(createStage(Stations.MarketStreet, TravelAction.Change, Stations.Bury));

        Journey journey = new Journey(stages);

        assertEquals("Change at Deansgate-Castlefield and Market Street", journey.getSummary());
        assertEquals("Tram and Walk with 2 changes - 12 minutes", journey.getHeading());
    }

    @Test
    @Ignore("Work in progress")
    public void shouldCreateJourneyDTO() {
        List<PresentableStage> stages = new LinkedList<>();
        stages.add(createStage(Stations.ManAirport, TravelAction.Board, Stations.Deansgate));
        Journey journey = new Journey(stages);

        JourneyDTO dto = null; //journey.asDTO();

        assertEquals(journey.getExpectedArrivalTime(), dto.getExpectedArrivalTime());
        assertEquals(journey.getBegin(), dto.getBegin());
        assertEquals(journey.getFirstDepartureTime(), dto.getFirstDepartureTime());
        assertEquals(journey.getEnd(), dto.getEnd());
        assertEquals(journey.getHeading(), dto.getHeading());
        assertEquals(journey.getSummary(), dto.getSummary());
        assertEquals(journey.getStages(), dto.getStages());

    }

    private List<PresentableStage> createThreeStages() {
        List<PresentableStage> stages = new LinkedList<>();
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

    private List<PresentableStage> createStages(LocalTime arrivesEnd) {
        List<PresentableStage> stages = new LinkedList<>();
        RawVehicleStage rawTravelStage = new RawVehicleStage(stationA, "routeName", TransportMode.Bus, "cssClass").
                setLastStation(stationB).setCost(42);
        ServiceTime serviceTime = new ServiceTime(new LocalTime(10, 8), arrivesEnd, "svcId", "headSign", "tripId");
        stages.add(new VehicleStageWithTiming(rawTravelStage, serviceTime, TravelAction.Board));
        return stages;
    }
}
