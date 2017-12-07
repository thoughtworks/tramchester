package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.RawVehicleStage;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.ServiceTime;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.domain.presentation.VehicleStageWithTiming;
import com.tramchester.integration.Stations;
import org.joda.time.LocalTime;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JourneyDTOTest {
    private LocationDTO stationA = new LocationDTO(Stations.Deansgate);
    private LocationDTO stationB = new LocationDTO(Stations.VeloPark);

    private JourneyDTO journeyA = new JourneyDTO(stationA, stationB, new LinkedList<StageDTO>(),
            new LocalTime(10, 20), new LocalTime(10, 8),
            "summary", "heading", false);

    private JourneyDTO journeyB = new JourneyDTO(stationA, stationB, new LinkedList<StageDTO>(),
            new LocalTime(10, 25), new LocalTime(10, 8),
            "summary", "heading", false);

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
        JourneyDTO beforeMidnight = new JourneyDTO(stationA, stationB, new LinkedList<StageDTO>(),
                new LocalTime(23, 50), new LocalTime(10, 8),
                "summary", "heading", false);

        JourneyDTO afterMidnight = new JourneyDTO(stationA, stationB, new LinkedList<StageDTO>(),
                new LocalTime(00, 10), new LocalTime(10, 8),
                "summary", "heading", false);

        SortedSet<JourneyDTO> set = new TreeSet<>();
        set.add(afterMidnight);
        set.add(beforeMidnight);
//        set.add(new Journey(createStages(new LocalTime(00, 10))).asDTO(liveDataEnricher));
//        set.add(new Journey(createStages(new LocalTime(23, 50))).asDTO(liveDataEnricher));

        assertEquals(new LocalTime(23,50), set.first().getExpectedArrivalTime());
        assertEquals(new LocalTime(23,50), set.stream().findFirst().get().getExpectedArrivalTime());
    }

    private List<TransportStage> createStages(LocalTime arrivesEnd) {
        List<TransportStage> stages = new LinkedList<>();
        RawVehicleStage rawTravelStage = new RawVehicleStage(Stations.Deansgate, "routeName", TransportMode.Bus, "cssClass").
                setLastStation(Stations.VeloPark).setCost(42);
        ServiceTime serviceTime = new ServiceTime(new LocalTime(10, 8), arrivesEnd, "svcId", "headSign", "tripId");
        stages.add(new VehicleStageWithTiming(rawTravelStage, serviceTime, TravelAction.Board));
        return stages;
    }
}
