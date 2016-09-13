package com.tramchester.domain;


import com.tramchester.domain.presentation.LatLong;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import org.joda.time.LocalTime;

import static org.junit.Assert.*;

public class StopsTest {
    private int am10Minutes;
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
    public void beforeEachTestRuns() {
        stationA = new Station(stationIdA, "areaA", "nameA", new LatLong(-1,1), false);
        stationB = new Station(stationIdB, "areaB", "nameB", new LatLong(-2,2), false);
        stationC = new Station(stationIdC, "areaC", "nameC", new LatLong(-3,3), false);
        stationD = new Station("statD", "areaC", "nameC", new LatLong(-3,3), false);

        stopA = new Stop(stationA, new LocalTime(10, 00), new LocalTime(10, 01));
        stopB = new Stop(stationB, new LocalTime(10, 02), new LocalTime(10, 03));
        stopC = new Stop(stationC, new LocalTime(10, 10), new LocalTime(10, 10));
        busStopD = new Stop(stationD, new LocalTime(10,10), new LocalTime(10,11));
        am10Minutes = 10 * 60;
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

        assertTrue(stops.travelsBetween(stationIdA, stationIdB, new TimeWindow(am10Minutes, 30)));
        assertTrue(stops.travelsBetween(stationIdA, stationIdC, new TimeWindow(am10Minutes, 30)));
        assertTrue(stops.travelsBetween(stationIdB, stationIdC, new TimeWindow(am10Minutes, 30)));
        assertFalse(stops.travelsBetween(stationIdC, stationIdA, new TimeWindow(am10Minutes, 30)));
        assertFalse(stops.travelsBetween(stationIdA, stationIdA, new TimeWindow(am10Minutes, 30)));
    }

    @Test
    public void shouldCopeWithSameDepartArriveTimeForAdjacentStops() {
        // this can happen for buses
        Stops stops = new Stops();

        stops.add(stopA);
        stops.add(stopB);
        stops.add(stopC);
        stops.add(busStopD);
        assertTrue(stops.travelsBetween(stationIdC, stationD.getId(), new TimeWindow(am10Minutes+9, 30)));
    }

    @Test
    public void shouldModelMultipleVisitsToSameStation() {
        Stops stops = new Stops();

        stops.add(stopA);
        stops.add(stopB);
        stops.add(stopC);

        Stop stopD = new Stop(stationA, new LocalTime(10, 20), new LocalTime(10, 21));
        stops.add(stopD);

        assertTrue(stops.visitsStation(stationIdA));
        List<Stop> result = stops.getStopsFor(stationIdA);
        assertEquals(2,result.size());

        assertTrue(stops.travelsBetween(stationIdA, stationIdB, new TimeWindow(am10Minutes, 30)));
        assertTrue(stops.travelsBetween(stationIdA, stationIdC, new TimeWindow(am10Minutes, 30)));
        assertTrue(stops.travelsBetween(stationIdB, stationIdC, new TimeWindow(am10Minutes, 30)));
        assertTrue(stops.travelsBetween(stationIdC, stationIdA, new TimeWindow(am10Minutes, 30)));
        assertTrue(stops.travelsBetween(stationIdB, stationIdA, new TimeWindow(am10Minutes, 30)));
        assertTrue(stops.travelsBetween(stationIdA, stationIdA, new TimeWindow(am10Minutes, 30)));
    }
}
