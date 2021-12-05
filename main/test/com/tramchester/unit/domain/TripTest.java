package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.PlatformStopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static com.tramchester.domain.id.StringIdFor.createId;
import static com.tramchester.domain.time.TramTime.nextDay;
import static com.tramchester.domain.time.TramTime.of;
import static org.junit.jupiter.api.Assertions.*;

class TripTest {

    private TramStations stationA;
    private TramStations stationB;
    private TramStations stationC;

    private MutableTrip trip;

    @BeforeEach
    void beforeEachTestRuns() {
        Service service = MutableService.build(createId("svcId"));

        trip = new MutableTrip(createId("tripId"),"headSign", service, TestEnv.getTramTestRoute());

        stationA = TramStations.Ashton;
        stationB = TramStations.Broadway;
        stationC = TramStations.Cornbrook;
    }

    @Test
    void shouldModelCircularTripsCorrectly() {

        PlatformStopCall firstStop = TestEnv.createTramStopCall(trip, "statA1", stationA, (byte) 1, of(10, 0), of(10, 1));
        PlatformStopCall secondStop = TestEnv.createTramStopCall(trip, "statB1", stationB, (byte) 2, of(10, 5), of(10, 6));
        PlatformStopCall thirdStop = TestEnv.createTramStopCall(trip, "statA1", stationA, (byte) 3, of(10, 10), of(10, 10));

        trip.addStop(firstStop);
        trip.addStop(secondStop);
        trip.addStop(thirdStop);

        assertEquals(of(10, 1), trip.earliestDepartTime());

        // sequence respected
        List<Integer> seqNums = new LinkedList<>();
        trip.getStopCalls().stream().forEach(stop -> seqNums.add(stop.getGetSequenceNumber()));
        assertEquals(1, seqNums.get(0).intValue());
        assertEquals(2, seqNums.get(1).intValue());
        assertEquals(3, seqNums.get(2).intValue());
    }

    @Test
    void shouldCheckIfNotCrossesIntoNextDay() {
        PlatformStopCall firstStop = TestEnv.createTramStopCall(trip, "stop1", stationA, (byte) 2, of(23, 45), of(23, 46));
        PlatformStopCall secondStop = TestEnv.createTramStopCall(trip, "stop2", stationB, (byte) 3, of(23, 59), of(0, 1));

        trip.addStop(firstStop);
        trip.addStop(secondStop);

        assertFalse(trip.intoNextDay());
    }

    @Test
    void shouldFindEarliestDepartCorrectlyCrossingMidnight() {

        PlatformStopCall firstStop = TestEnv.createTramStopCall(trip, "stop1", stationA, (byte) 2, of(23, 45), of(23, 46));
        PlatformStopCall secondStop = TestEnv.createTramStopCall(trip, "stop2", stationB, (byte) 3, of(23, 59), of(0, 1));
        PlatformStopCall thirdStop = TestEnv.createTramStopCall(trip, "stop3", stationC, (byte) 4, nextDay(0, 10), nextDay(0, 11));
        PlatformStopCall fourthStop = TestEnv.createTramStopCall(trip, "stop4", stationC, (byte) 1, of(22, 45), of(22, 46));

        trip.addStop(firstStop);
        trip.addStop(secondStop);
        trip.addStop(thirdStop);
        trip.addStop(fourthStop);

        // trip uses seq number for earliest depart i.e. lowest seq is assumed to be the first depart
        assertEquals(of(22,46), trip.earliestDepartTime());

        assertTrue(trip.intoNextDay());
    }

    @Test
    void shouldFindEarliestDepartCorrectly() {

        PlatformStopCall thirdStop = TestEnv.createTramStopCall(trip, "stop3", stationC, (byte) 3, of(0, 10), of(0, 11));
        PlatformStopCall fourthStop = TestEnv.createTramStopCall(trip, "stop4", stationC, (byte) 1, of(6, 30), of(6, 31));

        trip.addStop(thirdStop);
        trip.addStop(fourthStop);

        assertEquals(of(6,31), trip.earliestDepartTime());
    }

    @Test
    void shouldFindLatestDepartCorrectly() {
        trip.addStop(TestEnv.createTramStopCall(trip, "stopId3", TramStations.Deansgate, (byte) 3, of(10, 25), of(10, 26)));
        trip.addStop(TestEnv.createTramStopCall(trip, "stopId4", TramStations.Deansgate, (byte) 4, of(0, 1), of(0, 1)));

        assertEquals(of(0,1), trip.latestDepartTime());

    }

    @Test
    void shouldKnowIfTramTrip() {
        Service service = MutableService.build(createId("svcId"));

        Trip tripA = MutableTrip.build(createId("tripId"), "headSign", service, TestEnv.getTramTestRoute());
        assertTrue(TransportMode.isTram(tripA));
        final Agency agency = MutableAgency.build(DataSourceID.tfgm, createId("BUS"), "agencyName");
        Route busRoute = MutableRoute.getRoute(createId("busRouteId"), "busRouteCode", "busRouteName",
                agency, TransportMode.Bus);
        Trip tripB = MutableTrip.build(createId("tripId"), "headSign", service, busRoute);
        assertFalse(TransportMode.isTram(tripB));
    }

}
