package com.tramchester.domain;


import org.junit.Before;
import org.junit.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.Assert.*;

public class TestStops {

    private final String stationIdA = "statA";
    private final String stationIdB = "statB";
    private final String stationIdC = "statC";
    private Station stationA;
    private Station stationB;
    private Station stationC;
    private Stop stopA;
    private Stop stopB;
    private Stop stopC;
    private int am10Minutes;

    @Before
    public void beforeEachTestRuns() {
        stationA = new Station(stationIdA, "areaA", "nameA", -1,1, false);
        stationB = new Station(stationIdB, "areaB", "nameB", -2,2, false);
        stationC = new Station(stationIdC, "areaC", "nameC", -3,3, false);
        stopA = new Stop(stationA, LocalTime.of(10, 00), LocalTime.of(10, 01));
        stopB = new Stop(stationB, LocalTime.of(10, 02), LocalTime.of(10, 03));
        stopC = new Stop(stationC, LocalTime.of(10, 10), LocalTime.of(10, 10));
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

        assertTrue(stops.travelsBetween(stationIdA, stationIdB, am10Minutes));
        assertTrue(stops.travelsBetween(stationIdA, stationIdC, am10Minutes));
        assertTrue(stops.travelsBetween(stationIdB, stationIdC, am10Minutes));
        assertFalse(stops.travelsBetween(stationIdC, stationIdA, am10Minutes));
        assertFalse(stops.travelsBetween(stationIdA, stationIdA, am10Minutes));

    }

    @Test
    public void shouldModelMultipleVisitsToSameStation() {
        Stops stops = new Stops();

        stops.add(stopA);
        stops.add(stopB);
        stops.add(stopC);

        Stop stopD = new Stop(stationA, LocalTime.of(10, 20), LocalTime.of(10, 21));
        stops.add(stopD);

        assertTrue(stops.visitsStation(stationIdA));
        List<Stop> result = stops.getStopsFor(stationIdA);
        assertEquals(2,result.size());

        assertTrue(stops.travelsBetween(stationIdA, stationIdB, am10Minutes));
        assertTrue(stops.travelsBetween(stationIdA, stationIdC, am10Minutes));
        assertTrue(stops.travelsBetween(stationIdB, stationIdC, am10Minutes));
        assertTrue(stops.travelsBetween(stationIdC, stationIdA, am10Minutes));
        assertTrue(stops.travelsBetween(stationIdB, stationIdA, am10Minutes));
        assertTrue(stops.travelsBetween(stationIdA, stationIdA, am10Minutes));
    }
}
