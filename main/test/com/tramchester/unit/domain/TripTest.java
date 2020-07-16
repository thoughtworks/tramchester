package com.tramchester.unit.domain;

import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.input.TramStopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static com.tramchester.domain.Platform.from;
import static org.junit.jupiter.api.Assertions.*;

class TripTest {

    private Station stationA;
    private Station stationB;
    private Trip trip;
    private Station stationC;

    @BeforeEach
    void beforeEachTestRuns() {
        Service service = new Service("svcId", TestEnv.getTestRoute());

        trip = new Trip("tripId","headSign", service, TestEnv.getTestRoute());
        stationA = new Station("statA","areaA", "stopNameA", new LatLong(1.0, -1.0), false);
        stationB = new Station("statB","areaA", "stopNameB", new LatLong(2.0, -2.0), false);
        stationC = new Station("statC","areaA", "stopNameB", new LatLong(2.0, -2.0), false);
    }

    @Test
    void shouldKnowIfTramTrip() {
        Service service = new Service("svcId", TestEnv.getTestRoute());

        Trip tripA = new Trip("tripId", "headSign", service, TestEnv.getTestRoute());
        assertTrue(tripA.getTram());
        Route busRoute = new Route("busRouteId", "busRouteCode", "busRouteName", new Agency("BUS", "agencyName"), TransportMode.Bus);
        Trip tripB = new Trip("tripId", "headSign", service, busRoute);
        assertFalse(tripB.getTram());
    }

    @Test
    void shouldModelCircularTripsCorrectly() {

        TramStopCall firstStop = new TramStopCall(from("statA1"), stationA, (byte) 1, ServiceTime.of(10, 0), ServiceTime.of(10, 1));
        TramStopCall secondStop = new TramStopCall(from("statB1"), stationB, (byte) 2, ServiceTime.of(10, 5), ServiceTime.of(10, 6));
        TramStopCall thirdStop = new TramStopCall(from("statA1"), stationA, (byte) 3, ServiceTime.of(10, 10), ServiceTime.of(10, 10));

        trip.addStop(firstStop);
        trip.addStop(secondStop);
        trip.addStop(thirdStop);

        assertEquals(ServiceTime.of(10, 1), trip.earliestDepartTime());

        // sequence respected
        List<Byte> seqNums = new LinkedList<>();
        trip.getStops().forEach(stop -> seqNums.add(stop.getGetSequenceNumber()));
        assertEquals(1, seqNums.get(0).intValue());
        assertEquals(2, seqNums.get(1).intValue());
        assertEquals(3, seqNums.get(2).intValue());
    }

    @Test
    void shouldFindEarliestDepartCorrectlyCrossingMidnight() {

        TramStopCall firstStop = new TramStopCall(from("stop1"), stationA, (byte) 2, ServiceTime.of(23, 45), ServiceTime.of(23, 46));
        TramStopCall secondStop = new TramStopCall(from("stop2"), stationB, (byte) 3, ServiceTime.of(23, 59), ServiceTime.of(0, 1));
        TramStopCall thirdStop = new TramStopCall(from("stop3"), stationC, (byte) 4, ServiceTime.of(0,10), ServiceTime.of(0, 11));
        TramStopCall fourthStop = new TramStopCall(from("stop4"), stationC, (byte) 1, ServiceTime.of(6,30), ServiceTime.of(6, 30));

        trip.addStop(firstStop);
        trip.addStop(secondStop);
        trip.addStop(thirdStop);
        trip.addStop(fourthStop);

        assertEquals(ServiceTime.of(6,30), trip.earliestDepartTime());
    }

    @Test
    void shouldFindEarliestDepartCorrectly() {

        TramStopCall thirdStop = new TramStopCall(from("stop3"), stationC, (byte) 3, ServiceTime.of(0,10), ServiceTime.of(0, 11));
        TramStopCall fourthStop = new TramStopCall(from("stop4"), stationC, (byte) 1, ServiceTime.of(6,30), ServiceTime.of(6, 31));

        trip.addStop(thirdStop);
        trip.addStop(fourthStop);

        assertEquals(ServiceTime.of(6,31), trip.earliestDepartTime());
    }

    @Test
    void shouldFindLatestDepartCorrectly() {
        trip.addStop(new TramStopCall(from("stopId3"), Stations.Deansgate, (byte) 3, ServiceTime.of(10,25), ServiceTime.of(10,26)));
        trip.addStop(new TramStopCall(from("stopId4"), Stations.Deansgate, (byte) 4, ServiceTime.of(0,1), ServiceTime.of(0,1)));

        assertEquals(ServiceTime.of(0,1), trip.latestDepartTime());

    }

}
