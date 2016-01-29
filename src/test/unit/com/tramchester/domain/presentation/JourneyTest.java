package com.tramchester.domain.presentation;


import com.tramchester.domain.RawStage;
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

    private List<Stage> createStages(LocalTime arrivesEnd) {
        List<Stage> stages = new LinkedList<>();
        RawStage rawStage = new RawStage(stationA, "routeName", TransportMode.Bus, "cssClass", 20).
                setLastStation(stationB);
        SortedSet<ServiceTime> serviceTimes = new TreeSet<>();
        serviceTimes.add(new ServiceTime(LocalTime.of(10,8), arrivesEnd, "svcId", "headSign", "tripId"));
        stages.add(new Stage(rawStage, serviceTimes));
        return stages;
    }
}
