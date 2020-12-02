package com.tramchester.unit.domain;


import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.TramStopCall;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramStations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.operation.TransformException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TramStopCallsTest {
    private TramStations stationA;
    private TramStations stationB;
    private TramStations stationC;
    private TramStations stationD;
    private TramStopCall stopA;
    private TramStopCall stopB;
    private TramStopCall stopC;
    private StopCalls stops;

    @BeforeEach
    void beforeEachTestRuns() throws TransformException {
//        stationA = TestStation.forTest("statA", "areaA", "nameA", new LatLong(-1,1), TransportMode.Bus);
//        stationB = TestStation.forTest("statB", "areaB", "nameB", new LatLong(-2,2), TransportMode.Bus);
//        stationC = TestStation.forTest("statC", "areaC", "nameC", new LatLong(-3,3), TransportMode.Bus);
//        stationD = TestStation.forTest("statD", "areaC", "nameC", new LatLong(-3,3), TransportMode.Bus);

        stationA = TramStations.Ashton;
        stationB = TramStations.Broadway;
        stationC = TramStations.Cornbrook;
        stationD = TramStations.Deansgate;

        stopA = TestEnv.createTramStopCall("tripid", "statA1", stationA, 3, ServiceTime.of(10, 10), ServiceTime.of(10, 11));
        stopB = TestEnv.createTramStopCall("tripid", "statB1", stationB, 2, ServiceTime.of(10, 3), ServiceTime.of(10, 4));
        stopC = TestEnv.createTramStopCall("tripid", "statC1", stationC, 1, ServiceTime.of(10, 0), ServiceTime.of(10, 1));


        stops = new StopCalls();

        stops.add(stopA);
        stops.add(stopB);
        stops.add(stopC);
    }

    @Test
    void shouldAddStops() {

        assertTrue(stops.callsAt(stationA));
        assertTrue(stops.callsAt(stationB));
        assertTrue(stops.callsAt(stationC));
        assertFalse(stops.callsAt(stationD));

        assertEquals(3, stops.numberOfCallingPoints());
    }

    @Test
    void shouldGetByStopSeq() {
        assertEquals(stopB, stops.getStopBySequenceNumber(2));
        assertEquals(stopC, stops.getStopBySequenceNumber(1));
    }

    @Test
    void shouldHaveExpectedLegs() {
        List<StopCalls.StopLeg> legs = stops.getLegs();

        assertEquals(2, legs.size());

        StopCalls.StopLeg firstLeg = legs.get(0);
        assertEquals(stopC, firstLeg.getFirst());
        assertEquals(stopB, firstLeg.getSecond());
        assertEquals(2, firstLeg.getCost());

        StopCalls.StopLeg secondLeg = legs.get(1);
        assertEquals(stopB, secondLeg.getFirst());
        assertEquals(stopA, secondLeg.getSecond());
        assertEquals(6, secondLeg.getCost());
    }
}
