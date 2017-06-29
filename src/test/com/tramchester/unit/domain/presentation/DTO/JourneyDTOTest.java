package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.Location;
import com.tramchester.domain.RawVehicleStage;
import com.tramchester.domain.Station;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.*;
import org.joda.time.LocalTime;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JourneyDTOTest {
    private Location stationA = new Station("stationA", "area", "nameA", new LatLong(-2, -1), false);
    private Location stationB = new Station("stationB", "area", "nameB", new LatLong(-3, 1), false);

    private JourneyDTO journeyA = new Journey(createStages(new LocalTime(10, 20))).asDTO();
    private JourneyDTO journeyB = new Journey(createStages(new LocalTime(10, 25))).asDTO();

    @Test
    public void shouldCompareJourneysBasedOnEarliestArrival() {
        assertTrue(journeyA.compareTo(journeyB)<0);
        assertTrue(journeyB.compareTo(journeyA)>0);
    }

    @Test
    public void shouldHaveSortedSetInExpectedOrder() {
        SortedSet<JourneyDTO> set = new TreeSet<>();
        set.add(journeyB);
        set.add(journeyA);
        assertEquals(new LocalTime(10,20), set.first().getExpectedArrivalTime());
    }

    @Test
    public void shouldHaveSortedSetInExpectedOrderAccrossMidnight() {
        SortedSet<JourneyDTO> set = new TreeSet<>();
        set.add(new Journey(createStages(new LocalTime(00, 10))).asDTO());
        set.add(new Journey(createStages(new LocalTime(23, 50))).asDTO());

        assertEquals(new LocalTime(23,50), set.first().getExpectedArrivalTime());
        assertEquals(new LocalTime(23,50), set.stream().findFirst().get().getExpectedArrivalTime());
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
