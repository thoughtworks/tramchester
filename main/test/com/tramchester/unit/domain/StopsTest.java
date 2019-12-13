package com.tramchester.unit.domain;


import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.input.Stop;
import com.tramchester.domain.input.Stops;
import com.tramchester.domain.presentation.LatLong;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.Assert.*;

public class StopsTest {
    private TramTime am10;
    private final String stationIdA = "statA";
    private final String stationIdB = "statB";
    private final String stationIdC = "statC";
    private Location stationA;
    private Location stationB;
    private Location stationC;
    private Location stationD;
    private Stop stopA;
    private Stop stopB;
    private Stop stopC;
    private Stop busStopD;

    @Before
    public void beforeEachTestRuns() throws TramchesterException {
        stationA = new Station(stationIdA, "areaA", "nameA", new LatLong(-1,1), false);
        stationB = new Station(stationIdB, "areaB", "nameB", new LatLong(-2,2), false);
        stationC = new Station(stationIdC, "areaC", "nameC", new LatLong(-3,3), false);
        stationD = new Station("statD", "areaC", "nameC", new LatLong(-3,3), false);

        String routeId = "routeID";
        String serviceId = "serviceId";
        stopA = new Stop("statA1", stationA, 1, TramTime.of(10, 00), TramTime.of(10, 01));
        stopB = new Stop("statB1", stationB, 2, TramTime.of(10, 02), TramTime.of(10, 03));
        stopC = new Stop("statC1", stationC, 3, TramTime.of(10, 10), TramTime.of(10, 10));
        busStopD = new Stop("statD1", stationD, 4, TramTime.of(10,10), TramTime.of(10,11));
        am10 = TramTime.of(10,0);
    }

    @Test
    public void shouldFindStopsByTimeCrossingMidnight() {
        Stop stopF = new Stop("stop1", stationA, 1, TramTime.of(LocalTime.of(23, 45)),
                TramTime.of(LocalTime.of(23, 46)));
        Stop stopG = new Stop("stop2", stationB, 2, TramTime.of(LocalTime.of(00, 5)),
                TramTime.of(LocalTime.of(00, 6)));
        Stops stops = new Stops();

        stops.add(stopF);
        stops.add(stopG);
        assertTrue(stops.travelsBetween(stationIdA, stationIdB, new TimeWindow(TramTime.of(23,40), 30)));
        assertFalse(stops.travelsBetween(stationIdA, stationIdB, new TimeWindow(TramTime.of(23,47), 30)));

        assertFalse(stops.travelsBetween(stationIdA, stationIdB, new TimeWindow(TramTime.of(00,17), 30)));
        assertFalse(stops.travelsBetween(stationIdA, stationIdB, new TimeWindow(TramTime.of(23,15), 30)));
    }

    @Test
    public void shouldAddStops() {
        Stops stops = new Stops();

        stops.add(stopA);
        stops.add(stopB);
        stops.add(stopC);

        assertTrue(stops.visitsStation(stationIdA));
        assertTrue(stops.visitsStation(stationIdB));
        assertTrue(stops.visitsStation(stationIdC));

        List<Stop> result = stops.getStopsFor(stationIdA);
        assertEquals(1,result.size());
        assertTrue(result.contains(stopA));
        result = stops.getStopsFor(stationIdB);
        assertEquals(1,result.size());
        assertTrue(result.contains(stopB));
        result = stops.getStopsFor(stationIdC);
        assertEquals(1,result.size());
        assertTrue(result.contains(stopC));

        assertTrue(stops.travelsBetween(stationIdA, stationIdB, new TimeWindow(am10, 30)));
        assertFalse(stops.travelsBetween(stationIdA, stationIdB, new TimeWindow(am10.minusMinutes(31), 30)));
        assertFalse(stops.travelsBetween(stationIdA, stationIdB, new TimeWindow(am10.plusMinutes(41), 30)));

        assertTrue(stops.travelsBetween(stationIdA, stationIdC, new TimeWindow(am10, 30)));
        assertTrue(stops.travelsBetween(stationIdB, stationIdC, new TimeWindow(am10, 30)));
        assertFalse(stops.travelsBetween(stationIdC, stationIdA, new TimeWindow(am10, 30)));
        assertFalse(stops.travelsBetween(stationIdA, stationIdA, new TimeWindow(am10, 30)));
    }

    @Test
    public void shouldCopeWithSameDepartArriveTimeForAdjacentStops() {
        // this can happen for buses
        Stops stops = new Stops();

        stops.add(stopA);
        stops.add(stopB);
        stops.add(stopC);
        stops.add(busStopD);
        assertTrue(stops.travelsBetween(stationIdC, stationD.getId(), new TimeWindow(am10.plusMinutes(9), 30)));
    }

    @Test
    public void shouldModelMultipleVisitsToSameStation() throws TramchesterException {
        Stops stops = new Stops();

        stops.add(stopA);
        stops.add(stopB);
        stops.add(stopC);

        Stop stopD = new Stop("stopA1", stationA, 4, TramTime.create(10, 20),
                TramTime.create(10, 21));
        stops.add(stopD);

        assertTrue(stops.visitsStation(stationIdA));
        List<Stop> result = stops.getStopsFor(stationIdA);
        assertEquals(2,result.size());

        assertTrue(stops.travelsBetween(stationIdA, stationIdB, new TimeWindow(am10, 30)));
        assertTrue(stops.travelsBetween(stationIdA, stationIdC, new TimeWindow(am10, 30)));
        assertTrue(stops.travelsBetween(stationIdB, stationIdC, new TimeWindow(am10, 30)));
        assertTrue(stops.travelsBetween(stationIdC, stationIdA, new TimeWindow(am10, 30)));
        assertTrue(stops.travelsBetween(stationIdB, stationIdA, new TimeWindow(am10, 30)));
        assertTrue(stops.travelsBetween(stationIdA, stationIdA, new TimeWindow(am10, 30)));
    }
}
