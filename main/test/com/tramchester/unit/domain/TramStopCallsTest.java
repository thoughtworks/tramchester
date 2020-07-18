package com.tramchester.unit.domain;


import com.tramchester.domain.TransportMode;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.TramStopCall;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TramStopCallsTest {
    private Station stationA;
    private Station stationB;
    private Station stationC;
    private Station stationD;
    private TramStopCall stopA;
    private TramStopCall stopB;
    private TramStopCall stopC;

    @BeforeEach
    void beforeEachTestRuns() {
        stationA = Station.forTest("statA", "areaA", "nameA", new LatLong(-1,1), TransportMode.Bus);
        stationB = Station.forTest("statB", "areaB", "nameB", new LatLong(-2,2), TransportMode.Bus);
        stationC = Station.forTest("statC", "areaC", "nameC", new LatLong(-3,3), TransportMode.Bus);
        stationD = Station.forTest("statD", "areaC", "nameC", new LatLong(-3,3), TransportMode.Bus);

        stopA = TestEnv.createTramStopCall("tripid", "statA1", stationA, 1, ServiceTime.of(10, 0), ServiceTime.of(10, 1));
        stopB = TestEnv.createTramStopCall("tripid", "statB1", stationB, 2, ServiceTime.of(10, 2), ServiceTime.of(10, 3));
        stopC = TestEnv.createTramStopCall("tripid", "statC1", stationC, 3, ServiceTime.of(10, 10), ServiceTime.of(10, 10));
    }

    @Test
    void shouldAddStops() {
        StopCalls stops = new StopCalls();

        stops.add(stopA);
        stops.add(stopB);
        stops.add(stopC);

        assertTrue(stops.callsAt(stationA));
        assertTrue(stops.callsAt(stationB));
        assertTrue(stops.callsAt(stationC));
        assertFalse(stops.callsAt(stationD));

        assertEquals(3, stops.size());

    }
}
