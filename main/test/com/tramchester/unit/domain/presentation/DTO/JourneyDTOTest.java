package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.integration.Stations;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JourneyDTOTest {
    private LocationDTO stationA = new LocationDTO(Stations.Deansgate);
    private LocationDTO stationB = new LocationDTO(Stations.VeloPark);

    private JourneyDTO journeyA;
    private JourneyDTO journeyB;
    private List<String> changeStations = new ArrayList<>();
    private TramTime queryTime = TramTime.of(8,46);

    @Before
    public void beforeEachTestRuns() throws TramchesterException {
        journeyA = new JourneyDTO(stationA, stationB, new LinkedList<>(),
                TramTime.of(10, 20), TramTime.of(10, 8),
                false, changeStations, queryTime);
        journeyB = new JourneyDTO(stationA, stationB, new LinkedList<>(),
                TramTime.of(10, 25), TramTime.of(10, 8),
                false, changeStations, queryTime);
    }

    @Test
    public void shouldCompareJourneysNearMidnight() throws TramchesterException {
        JourneyDTO journeyC = new JourneyDTO(stationA, stationB, new LinkedList<>(),
                TramTime.of(23, 27), TramTime.of(23, 47),
                false, changeStations, queryTime);
        JourneyDTO journeyD = new JourneyDTO(stationA, stationB, new LinkedList<>(),
                TramTime.of(0, 3), TramTime.of(23, 23),
                false, changeStations, queryTime);
        assertTrue(journeyC.compareTo(journeyD)<0);
        assertTrue(journeyD.compareTo(journeyC)>0);
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
        assertEquals(TramTime.of(10,20), set.first().getExpectedArrivalTime());
    }

    @Test
    public void shouldHaveSortedSetInExpectedOrderAccrossMidnight() throws TramchesterException {
        JourneyDTO beforeMidnight = new JourneyDTO(stationA, stationB, new LinkedList<>(),
                TramTime.of(00, 10), TramTime.of(10, 8),
                false, changeStations, queryTime);

        JourneyDTO afterMidnight = new JourneyDTO(stationA, stationB, new LinkedList<>(),
                TramTime.of(23, 50), TramTime.of(10, 8),
                false, changeStations, queryTime);

        SortedSet<JourneyDTO> set = new TreeSet<>();
        set.add(afterMidnight);
        set.add(beforeMidnight);

        assertEquals(TramTime.of(23,50), set.first().getExpectedArrivalTime());
        assertEquals(TramTime.of(23,50), set.stream().findFirst().get().getExpectedArrivalTime());
    }

    @Test
    public void shouldHaveSortedSetInExpectedOrderLateNight() throws TramchesterException {
        JourneyDTO beforeMidnight = new JourneyDTO(stationA, stationB, new LinkedList<>(),
                TramTime.of(23, 42), TramTime.of(10, 8),
                false, changeStations, queryTime);

        JourneyDTO afterMidnight = new JourneyDTO(stationA, stationB, new LinkedList<>(),
                TramTime.of(23, 12), TramTime.of(10, 8),
                false, changeStations, queryTime);

        SortedSet<JourneyDTO> set = new TreeSet<>();
        set.add(afterMidnight);
        set.add(beforeMidnight);

        assertEquals(TramTime.of(23,12), set.first().getExpectedArrivalTime());
        assertEquals(TramTime.of(23,12), set.stream().findFirst().get().getExpectedArrivalTime());
    }

}
