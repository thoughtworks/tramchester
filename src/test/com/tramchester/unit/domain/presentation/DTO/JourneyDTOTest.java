package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.TramTime;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.integration.Stations;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JourneyDTOTest {
    private LocationDTO stationA = new LocationDTO(Stations.Deansgate);
    private LocationDTO stationB = new LocationDTO(Stations.VeloPark);

    private JourneyDTO journeyA;
    private JourneyDTO journeyB;

    @Before
    public void beforeEachTestRuns() throws TramchesterException {
        journeyA = new JourneyDTO(stationA, stationB, new LinkedList<>(),
                TramTime.create(10, 20), TramTime.create(10, 8),
                "summary", "heading", false);
        journeyB = new JourneyDTO(stationA, stationB, new LinkedList<>(),
                TramTime.create(10, 25), TramTime.create(10, 8),
                "summary", "heading", false);
    }

    @Test
    public void shouldCompareJourneysBasedOnEarliestArrival() {
        assertTrue(journeyA.compareTo(journeyB)<0);
        assertTrue(journeyB.compareTo(journeyA)>0);
    }

    @Test
    public void shouldHaveSortedSetInExpectedOrder() throws TramchesterException {
        SortedSet<JourneyDTO> set = new TreeSet<>();
        set.add(journeyB);
        set.add(journeyA);
        assertEquals(TramTime.create(10,20), set.first().getExpectedArrivalTime());
    }

    @Test
    public void shouldHaveSortedSetInExpectedOrderAccrossMidnight() throws TramchesterException {
        JourneyDTO beforeMidnight = new JourneyDTO(stationA, stationB, new LinkedList<>(),
                TramTime.create(23, 50), TramTime.create(10, 8),
                "summary", "heading", false);

        JourneyDTO afterMidnight = new JourneyDTO(stationA, stationB, new LinkedList<>(),
                TramTime.create(00, 10), TramTime.create(10, 8),
                "summary", "heading", false);

        SortedSet<JourneyDTO> set = new TreeSet<>();
        set.add(afterMidnight);
        set.add(beforeMidnight);

        assertEquals(TramTime.create(23,50), set.first().getExpectedArrivalTime());
        assertEquals(TramTime.create(23,50), set.stream().findFirst().get().getExpectedArrivalTime());
    }

}
