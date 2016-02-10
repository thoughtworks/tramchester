package com.tramchester.domain.presentation;


import com.tramchester.Stations;
import com.tramchester.domain.RawVehicleStage;
import com.tramchester.domain.Station;
import com.tramchester.domain.TransportMode;
import org.junit.Test;

import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;

public class JourneyTest {

    Journey journeyA = new Journey(createStages(LocalTime.of(10, 20)));
    Journey journeyB = new Journey(createStages(LocalTime.of(10, 25)));
    Station stationA = new Station("stationA", "area", "nameA", -2, -1, false);
    Station stationB = new Station("stationB", "area", "nameA", -3, 1, false);

    @Test
    public void shouldCompareJourneysBasedOnEarliestArrival() {
        assertEquals(-1,journeyA.compareTo(journeyB));
        assertEquals(1,journeyB.compareTo(journeyA));
    }

    @Test
    public void shouldHaveSortedSetInExpectedOrder() {
        SortedSet<Journey> set = new TreeSet<>();
        set.add(journeyB);
        set.add(journeyA);

        assertEquals(LocalTime.of(10,20), set.first().getExpectedArrivalTime());
    }

    @Test
    public void shouldHaveCorrectSummaryForDirect() {
        assertEquals("Direct", journeyA.getSummary());
    }

    @Test
    public void shouldHaveRightSummaryAndHeadingFor2Stage() {
        List<StageWithTiming> stages = new LinkedList<>();
        stages.add(createStage(Stations.Altrincham, TravelAction.Board, Stations.Cornbrook));
        stages.add(createStage(Stations.Cornbrook, TravelAction.Change, Stations.Deansgate));

        Journey journey = new Journey(stages);

        assertEquals("Change at Cornbrook", journey.getSummary());
        assertEquals("Tram with 1 change - 12 minutes", journey.getHeading());
    }

    @Test
    public void shouldHaveRightSummaryAndHeadingFor3Stage() {
        List<StageWithTiming> stages = createThreeStages();

        Journey journey = new Journey(stages);

        assertEquals("Change at Cornbrook and Victoria", journey.getSummary());
        assertEquals("Tram with 2 changes - 12 minutes", journey.getHeading());
    }

    @Test
    public void shouldHaveBeginAndEnd() {
        List<StageWithTiming> stages = createThreeStages();
        Journey journey = new Journey(stages);

        assertEquals(Stations.Altrincham, journey.getBegin());
        assertEquals(Stations.ExchangeSquare, journey.getEnd());
    }

    @Test
    public void shouldHaveRightSummaryAndHeadingFor4Stage() {
        List<StageWithTiming> stages = createThreeStages();
        stages.add(createStage(Stations.ExchangeSquare, TravelAction.Change, Stations.Rochdale));

        Journey journey = new Journey(stages);

        assertEquals("Change at Cornbrook, Victoria and Exchange Square", journey.getSummary());
        assertEquals("Tram with 3 changes - 12 minutes", journey.getHeading());
    }


    private List<StageWithTiming> createThreeStages() {
        List<StageWithTiming> stages = new LinkedList<>();
        stages.add(createStage(Stations.Altrincham, TravelAction.Board, Stations.Cornbrook));
        stages.add(createStage(Stations.Cornbrook, TravelAction.Change, Stations.Victoria));
        stages.add(createStage(Stations.Victoria, TravelAction.Change, Stations.ExchangeSquare));
        return stages;
    }

    private StageWithTiming createStage(Station firstStation, TravelAction travelAction, Station lastStation) {
        SortedSet<ServiceTime> serviceTimes = new TreeSet<>();
        serviceTimes.add(new ServiceTime(LocalTime.of(10,8), LocalTime.of(10,20), "svcId", "headSign", "tripId"));
        RawVehicleStage rawVehicleStage = new RawVehicleStage(firstStation, "routeName", TransportMode.Tram, "cssClass", 20);
        rawVehicleStage.setLastStation(lastStation);
        return new StageWithTiming(rawVehicleStage, serviceTimes, travelAction);
    }

    private List<StageWithTiming> createStages(LocalTime arrivesEnd) {
        List<StageWithTiming> stages = new LinkedList<>();
        RawVehicleStage rawTravelStage = new RawVehicleStage(stationA, "routeName", TransportMode.Bus, "cssClass", 20).
                setLastStation(stationB);
        SortedSet<ServiceTime> serviceTimes = new TreeSet<>();
        serviceTimes.add(new ServiceTime(LocalTime.of(10,8), arrivesEnd, "svcId", "headSign", "tripId"));
        stages.add(new StageWithTiming(rawTravelStage, serviceTimes, TravelAction.Board));
        return stages;
    }
}
