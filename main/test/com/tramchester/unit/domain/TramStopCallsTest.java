package com.tramchester.unit.domain;


import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.TramStopCall;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramTime;
import org.junit.Before;
import org.junit.Test;

import static com.tramchester.domain.Platform.from;
import static org.junit.Assert.*;

public class TramStopCallsTest {
    private Station stationA;
    private Station stationB;
    private Station stationC;
    private Station stationD;
    private TramStopCall stopA;
    private TramStopCall stopB;
    private TramStopCall stopC;

    @Before
    public void beforeEachTestRuns() {
        stationA = new Station("statA", "areaA", "nameA", new LatLong(-1,1), false);
        stationB = new Station("statB", "areaB", "nameB", new LatLong(-2,2), false);
        stationC = new Station("statC", "areaC", "nameC", new LatLong(-3,3), false);
        stationD = new Station("statD", "areaC", "nameC", new LatLong(-3,3), false);

        stopA = new TramStopCall(from("statA1"), stationA, (byte) 1, TramTime.of(10, 0), TramTime.of(10, 1));
        stopB = new TramStopCall(from("statB1"), stationB, (byte) 2, TramTime.of(10, 2), TramTime.of(10, 3));
        stopC = new TramStopCall(from("statC1"), stationC, (byte) 3, TramTime.of(10, 10), TramTime.of(10, 10));
    }

    @Test
    public void shouldAddStops() {
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
