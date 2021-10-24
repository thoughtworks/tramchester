package com.tramchester.unit.domain;

import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.PlatformStopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TripTest {

    private TramStations stationA;
    private TramStations stationB;
    private TramStations stationC;

    private Trip trip;

    @BeforeEach
    void beforeEachTestRuns() {
        Service service = new Service("svcId");

        trip = new Trip(StringIdFor.createId("tripId"),"headSign", service, TestEnv.getTramTestRoute());

        stationA = TramStations.Ashton;
        stationB = TramStations.Broadway;
        stationC = TramStations.Cornbrook;
    }

    @Test
    void shouldKnowIfTramTrip() {
        Service service = new Service("svcId");

        Trip tripA = new Trip(StringIdFor.createId("tripId"), "headSign", service, TestEnv.getTramTestRoute());
        assertTrue(TransportMode.isTram(tripA));
        Route busRoute = new Route(StringIdFor.createId("busRouteId"), "busRouteCode", "busRouteName",
                new Agency(DataSourceID.tfgm, StringIdFor.createId("BUS"), "agencyName"),
                TransportMode.Bus);
        Trip tripB = new Trip(StringIdFor.createId("tripId"), "headSign", service, busRoute);
        assertFalse(TransportMode.isTram(tripB));
    }

    @Test
    void shouldModelCircularTripsCorrectly() {

        PlatformStopCall firstStop = TestEnv.createTramStopCall(trip, "statA1", stationA, (byte) 1, TramTime.of(10, 0), TramTime.of(10, 1));
        PlatformStopCall secondStop = TestEnv.createTramStopCall(trip, "statB1", stationB, (byte) 2, TramTime.of(10, 5), TramTime.of(10, 6));
        PlatformStopCall thirdStop = TestEnv.createTramStopCall(trip, "statA1", stationA, (byte) 3, TramTime.of(10, 10), TramTime.of(10, 10));

        trip.addStop(firstStop);
        trip.addStop(secondStop);
        trip.addStop(thirdStop);

        assertEquals(TramTime.of(10, 1), trip.earliestDepartTime());

        // sequence respected
        List<Integer> seqNums = new LinkedList<>();
        trip.getStopCalls().stream().forEach(stop -> seqNums.add(stop.getGetSequenceNumber()));
        assertEquals(1, seqNums.get(0).intValue());
        assertEquals(2, seqNums.get(1).intValue());
        assertEquals(3, seqNums.get(2).intValue());
    }

    @Test
    void shouldFindEarliestDepartCorrectlyCrossingMidnight() {

        PlatformStopCall firstStop = TestEnv.createTramStopCall(trip, "stop1", stationA, (byte) 2, TramTime.of(23, 45), TramTime.of(23, 46));
        PlatformStopCall secondStop = TestEnv.createTramStopCall(trip, "stop2", stationB, (byte) 3, TramTime.of(23, 59), TramTime.of(0, 1));
        PlatformStopCall thirdStop = TestEnv.createTramStopCall(trip, "stop3", stationC, (byte) 4, TramTime.of(0, 10), TramTime.of(0, 11));
        PlatformStopCall fourthStop = TestEnv.createTramStopCall(trip, "stop4", stationC, (byte) 1, TramTime.of(6, 30), TramTime.of(6, 30));

        trip.addStop(firstStop);
        trip.addStop(secondStop);
        trip.addStop(thirdStop);
        trip.addStop(fourthStop);

        assertEquals(TramTime.of(6,30), trip.earliestDepartTime());
    }

    @Test
    void shouldFindEarliestDepartCorrectly() {

        PlatformStopCall thirdStop = TestEnv.createTramStopCall(trip, "stop3", stationC, (byte) 3, TramTime.of(0, 10), TramTime.of(0, 11));
        PlatformStopCall fourthStop = TestEnv.createTramStopCall(trip, "stop4", stationC, (byte) 1, TramTime.of(6, 30), TramTime.of(6, 31));

        trip.addStop(thirdStop);
        trip.addStop(fourthStop);

        assertEquals(TramTime.of(6,31), trip.earliestDepartTime());
    }

    @Test
    void shouldFindLatestDepartCorrectly() {
        trip.addStop(TestEnv.createTramStopCall(trip, "stopId3", TramStations.Deansgate, (byte) 3, TramTime.of(10, 25), TramTime.of(10, 26)));
        trip.addStop(TestEnv.createTramStopCall(trip, "stopId4", TramStations.Deansgate, (byte) 4, TramTime.of(0, 1), TramTime.of(0, 1)));

        assertEquals(TramTime.of(0,1), trip.latestDepartTime());

    }

}
