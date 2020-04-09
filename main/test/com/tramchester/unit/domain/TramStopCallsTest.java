package com.tramchester.unit.domain;


import com.tramchester.domain.*;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.TramStopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TimeWindow;
import com.tramchester.domain.time.TramTime;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalTime;
import java.util.List;

import static com.tramchester.domain.Platform.from;
import static org.junit.Assert.*;

public class TramStopCallsTest {
    private TramTime am10;
    private final String stationIdA = "statA";
    private final String stationIdB = "statB";
    private final String stationIdC = "statC";
    private Station stationA;
    private Station stationB;
    private Station stationC;
    private Station stationD;
    private TramStopCall stopA;
    private TramStopCall stopB;
    private TramStopCall stopC;
    private TramStopCall busStopD;

    @Before
    public void beforeEachTestRuns() {
        stationA = new Station(stationIdA, "areaA", "nameA", new LatLong(-1,1), false);
        stationB = new Station(stationIdB, "areaB", "nameB", new LatLong(-2,2), false);
        stationC = new Station(stationIdC, "areaC", "nameC", new LatLong(-3,3), false);
        stationD = new Station("statD", "areaC", "nameC", new LatLong(-3,3), false);

        stopA = new TramStopCall(from("statA1"), stationA, (byte) 1, TramTime.of(10, 0), TramTime.of(10, 1));
        stopB = new TramStopCall(from("statB1"), stationB, (byte) 2, TramTime.of(10, 2), TramTime.of(10, 3));
        stopC = new TramStopCall(from("statC1"), stationC, (byte) 3, TramTime.of(10, 10), TramTime.of(10, 10));
        busStopD = new TramStopCall(from("statD1"), stationD, (byte) 4, TramTime.of(10,10), TramTime.of(10,11));
        am10 = TramTime.of(10,0);
    }

    @Test
    public void shouldFindStopsByTimeCrossingMidnight() {
        TramStopCall stopF = new TramStopCall(from("stop1"), stationA, (byte) 1, TramTime.of(LocalTime.of(23, 45)),
                TramTime.of(LocalTime.of(23, 46)));
        TramStopCall stopG = new TramStopCall(from("stop2"), stationB, (byte) 2, TramTime.of(LocalTime.of(0, 5)),
                TramTime.of(LocalTime.of(0, 6)));
        StopCalls stops = new StopCalls();

        stops.add(stopF);
        stops.add(stopG);
        assertTrue(stops.travelsBetween(stationIdA, stationIdB, new TimeWindow(TramTime.of(23,40), 30)));
        assertFalse(stops.travelsBetween(stationIdA, stationIdB, new TimeWindow(TramTime.of(23,47), 30)));

        assertFalse(stops.travelsBetween(stationIdA, stationIdB, new TimeWindow(TramTime.of(0,17), 30)));
        assertFalse(stops.travelsBetween(stationIdA, stationIdB, new TimeWindow(TramTime.of(23,15), 30)));
    }

    @Test
    public void shouldAddStops() {
        StopCalls stops = new StopCalls();

        stops.add(stopA);
        stops.add(stopB);
        stops.add(stopC);

        assertTrue(stops.visitsStation(stationIdA));
        assertTrue(stops.visitsStation(stationIdB));
        assertTrue(stops.visitsStation(stationIdC));

        List<StopCall> result = stops.getStopsFor(stationIdA);
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
        StopCalls stops = new StopCalls();

        stops.add(stopA);
        stops.add(stopB);
        stops.add(stopC);
        stops.add(busStopD);
        assertTrue(stops.travelsBetween(stationIdC, stationD.getId(), new TimeWindow(am10.plusMinutes(9), 30)));
    }

    @Test
    public void shouldModelMultipleVisitsToSameStation() {
        StopCalls stops = new StopCalls();

        stops.add(stopA);
        stops.add(stopB);
        stops.add(stopC);

        TramStopCall stopD = new TramStopCall(from("stopA1"), stationA, (byte) 4, TramTime.of(10, 20),
                TramTime.of(10, 21));
        stops.add(stopD);

        assertTrue(stops.visitsStation(stationIdA));
        List<StopCall> result = stops.getStopsFor(stationIdA);
        assertEquals(2,result.size());

        assertTrue(stops.travelsBetween(stationIdA, stationIdB, new TimeWindow(am10, 30)));
        assertTrue(stops.travelsBetween(stationIdA, stationIdC, new TimeWindow(am10, 30)));
        assertTrue(stops.travelsBetween(stationIdB, stationIdC, new TimeWindow(am10, 30)));
        assertTrue(stops.travelsBetween(stationIdC, stationIdA, new TimeWindow(am10, 30)));
        assertTrue(stops.travelsBetween(stationIdB, stationIdA, new TimeWindow(am10, 30)));
        assertTrue(stops.travelsBetween(stationIdA, stationIdA, new TimeWindow(am10, 30)));
    }
}
