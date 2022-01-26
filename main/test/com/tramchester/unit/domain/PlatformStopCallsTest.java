package com.tramchester.unit.domain;


import com.tramchester.domain.MutablePlatform;
import com.tramchester.domain.MutableService;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.PlatformStopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tramchester.domain.id.StringIdFor.createId;
import static com.tramchester.domain.reference.GTFSPickupDropoffType.None;
import static com.tramchester.domain.reference.GTFSPickupDropoffType.Regular;
import static com.tramchester.domain.time.TramTime.of;
import static org.junit.jupiter.api.Assertions.*;

class PlatformStopCallsTest {
    private TramStations stationA;
    private TramStations stationB;
    private TramStations stationC;
    private TramStations stationD;
    private PlatformStopCall stopA;
    private PlatformStopCall stopB;
    private PlatformStopCall stopC;
    private StopCalls stops;
    private Trip trip;
    private Platform platformD;

    @BeforeEach
    void beforeEachTestRuns() {

        stationA = TramStations.Ashton;

        stationB = TramStations.Broadway;
        stationC = TramStations.Cornbrook;
        stationD = TramStations.Deansgate;
        platformD = MutablePlatform.buildForTFGMTram("statD1", "name:" + "statD1", stationD.getLatLong());

        Service service = MutableService.build(createId("svc1"));
        trip = MutableTrip.build(createId("tripId"), "headSign", service,
                TestEnv.getTramTestRoute());

        stopA = TestEnv.createTramStopCall(trip, "statA1", stationA, 3, of(10, 10), of(10, 11));
        stopB = TestEnv.createTramStopCall(trip, "statB1", stationB, 2, of(10, 3), of(10, 4));
        stopC = TestEnv.createTramStopCall(trip, "statC1", stationC, 1, of(10, 0), of(10, 1));

        stops = new StopCalls(createId("parentTripId"));

        stops.add(stopA);
        stops.add(stopB);
        stops.add(stopC);
    }

    @Test
    void shouldHaveFirstAndLast() {
        assertEquals(stopC, stops.getFirstStop());
        assertEquals(stopA, stops.getLastStop());
    }

    @Test
    void shouldCreateStopCall() {

        PlatformStopCall platformStopCall = new PlatformStopCall(platformD, TramStations.of(stationD),
                of(11,14), of (11, 15), 5, None, Regular, trip);

        assertEquals(of(11,14), platformStopCall.getArrivalTime());
        assertEquals(of (11,15), platformStopCall.getDepartureTime());
        assertEquals(5, platformStopCall.getGetSequenceNumber());
        assertEquals(None, platformStopCall.getPickupType());
        assertEquals(Regular, platformStopCall.getDropoffType());

        assertEquals(stationD.getId(), platformStopCall.getStationId());
        assertEquals(platformD, platformStopCall.getPlatform());
        assertEquals(trip.getTransportMode(), platformStopCall.getTransportMode());
    }

    @Test
    void shouldRecordIfIntoNextDay() {
        assertFalse(stopA.intoNextDay());

        TramTime nextDay = TramTime.nextDay(0,14);

        PlatformStopCall stopE = new PlatformStopCall(platformD, TramStations.of(stationD), nextDay, nextDay.plusMinutes(5), 4,
                Regular, Regular, trip);
        assertTrue(stopE.intoNextDay());

        PlatformStopCall stopD = new PlatformStopCall(platformD, TramStations.of(stationD), of(23, 59), nextDay, 5,
                Regular, Regular, trip);
        assertTrue(stopD.intoNextDay());

        assertFalse(stops.intoNextDay());
        stops.add(stopD);
        assertTrue(stops.intoNextDay());
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

    @Test
    void shouldHaveExpectedLegsNoDropoffOrPickup() {

        PlatformStopCall stopD = new PlatformStopCall(platformD, TramStations.of(stationD),
                of(11, 15), of(11, 16), 4, None, None, trip);

        PlatformStopCall stopE = new PlatformStopCall(platformD, TramStations.of(stationD),
                of(11, 25), of(11, 26), 5,
                None, Regular, trip);

        stops.add(stopD);
        stops.add(stopE);

        List<StopCalls.StopLeg> legs = stops.getLegs();

        assertEquals(3, legs.size());

        StopCalls.StopLeg firstLeg = legs.get(0);
        assertEquals(stopC, firstLeg.getFirst());
        assertEquals(stopB, firstLeg.getSecond());
        assertEquals(2, firstLeg.getCost());

        StopCalls.StopLeg secondLeg = legs.get(1);
        assertEquals(stopB, secondLeg.getFirst());
        assertEquals(stopA, secondLeg.getSecond());
        assertEquals(6, secondLeg.getCost());

        int expected = TramTime.diffenceAsMinutes(stopA.getDepartureTime(), stopE.getArrivalTime());
        StopCalls.StopLeg thirdLeg = legs.get(2);
        assertEquals(stopA, thirdLeg.getFirst());
        assertEquals(stopE, thirdLeg.getSecond()); // not D, no pick-up or drop-off
        assertEquals(expected, thirdLeg.getCost());
    }
}
