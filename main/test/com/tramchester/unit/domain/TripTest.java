package com.tramchester.unit.domain;

import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.reference.RouteDirection;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.input.TramStopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.operation.TransformException;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TripTest {

    private TramStations stationA;
    private TramStations stationB;
    private TramStations stationC;

    private Trip trip;

    @BeforeEach
    void beforeEachTestRuns() throws TransformException {
        Service service = new Service("svcId", TestEnv.getTestRoute());

        trip = new Trip("tripId","headSign", service, TestEnv.getTestRoute());

        stationA = TramStations.Ashton;
        stationB = TramStations.Broadway;
        stationC = TramStations.Cornbrook;
    }

    @Test
    void shouldKnowIfTramTrip() {
        Service service = new Service("svcId", TestEnv.getTestRoute());

        Trip tripA = new Trip("tripId", "headSign", service, TestEnv.getTestRoute());
        assertTrue(TransportMode.isTram(tripA));
        Route busRoute = new Route("busRouteId", "busRouteCode", "busRouteName", new Agency("BUS", "agencyName"),
                TransportMode.Bus, RouteDirection.Circular);
        Trip tripB = new Trip("tripId", "headSign", service, busRoute);
        assertFalse(TransportMode.isTram(tripB));
    }

    @Test
    void shouldModelCircularTripsCorrectly() {

        TramStopCall firstStop = TestEnv.createTramStopCall(trip.getId(), "statA1", stationA, (byte) 1, ServiceTime.of(10, 0), ServiceTime.of(10, 1));
        TramStopCall secondStop = TestEnv.createTramStopCall(trip.getId(), "statB1", stationB, (byte) 2, ServiceTime.of(10, 5), ServiceTime.of(10, 6));
        TramStopCall thirdStop = TestEnv.createTramStopCall(trip.getId(), "statA1", stationA, (byte) 3, ServiceTime.of(10, 10), ServiceTime.of(10, 10));

        trip.addStop(firstStop);
        trip.addStop(secondStop);
        trip.addStop(thirdStop);

        assertEquals(TramTime.of(10, 1), trip.earliestDepartTime());

        // sequence respected
        List<Integer> seqNums = new LinkedList<>();
        trip.getStops().stream().forEach(stop -> seqNums.add(stop.getGetSequenceNumber()));
        assertEquals(1, seqNums.get(0).intValue());
        assertEquals(2, seqNums.get(1).intValue());
        assertEquals(3, seqNums.get(2).intValue());
    }

    @Test
    void shouldFindEarliestDepartCorrectlyCrossingMidnight() {

        TramStopCall firstStop = TestEnv.createTramStopCall(trip.getId(), "stop1", stationA, (byte) 2, ServiceTime.of(23, 45), ServiceTime.of(23, 46));
        TramStopCall secondStop = TestEnv.createTramStopCall(trip.getId(), "stop2", stationB, (byte) 3, ServiceTime.of(23, 59), ServiceTime.of(0, 1));
        TramStopCall thirdStop = TestEnv.createTramStopCall(trip.getId(), "stop3", stationC, (byte) 4, ServiceTime.of(0,10), ServiceTime.of(0, 11));
        TramStopCall fourthStop = TestEnv.createTramStopCall(trip.getId(), "stop4", stationC, (byte) 1, ServiceTime.of(6,30), ServiceTime.of(6, 30));

        trip.addStop(firstStop);
        trip.addStop(secondStop);
        trip.addStop(thirdStop);
        trip.addStop(fourthStop);

        assertEquals(TramTime.of(6,30), trip.earliestDepartTime());
    }

    @Test
    void shouldFindEarliestDepartCorrectly() {

        TramStopCall thirdStop = TestEnv.createTramStopCall(trip.getId(), "stop3", stationC, (byte) 3, ServiceTime.of(0,10), ServiceTime.of(0, 11));
        TramStopCall fourthStop = TestEnv.createTramStopCall(trip.getId(), "stop4", stationC, (byte) 1, ServiceTime.of(6,30), ServiceTime.of(6, 31));

        trip.addStop(thirdStop);
        trip.addStop(fourthStop);

        assertEquals(TramTime.of(6,31), trip.earliestDepartTime());
    }

    @Test
    void shouldFindLatestDepartCorrectly() {
        trip.addStop(TestEnv.createTramStopCall(trip.getId(), "stopId3", TramStations.Deansgate, (byte) 3, ServiceTime.of(10,25), ServiceTime.of(10,26)));
        trip.addStop(TestEnv.createTramStopCall(trip.getId(), "stopId4", TramStations.Deansgate, (byte) 4, ServiceTime.of(0,1), ServiceTime.of(0,1)));

        assertEquals(TramTime.of(0,1), trip.latestDepartTime());

    }

}
