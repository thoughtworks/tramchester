package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.input.Stop;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ServiceTime;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalTime;
import java.util.SortedSet;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

public class TripTest {

    private Location stationA;
    private Location stationB;
    private Trip trip;
    private Location stationC;
    private String routeId;
    private String serviceId;

    @Before
    public void beforeEachTestRuns() {
        trip = new Trip("tripId","headSign", "svcId", "routeId");
        stationA = new Station("statA","areaA", "stopNameA", new LatLong(1.0, -1.0), false);
        stationB = new Station("statB","areaA", "stopNameB", new LatLong(2.0, -2.0), false);
        stationC = new Station("statC","areaA", "stopNameB", new LatLong(2.0, -2.0), false);
        routeId = "routeId";
        serviceId = "serviceId";
    }

    @Test
    public void shouldModelCircularTripsCorrectly() throws TramchesterException {

        Stop firstStop = new Stop("statA1", stationA, TramTime.create(10, 00), TramTime.create(10, 01), routeId, serviceId);
        Stop secondStop = new Stop("statB1", stationB, TramTime.create(10, 05), TramTime.create(10, 06), routeId, serviceId);
        Stop thirdStop = new Stop("statA1", stationA, TramTime.create(10, 10), TramTime.create(10, 10), routeId, serviceId);

        trip.addStop(firstStop);
        trip.addStop(secondStop);
        trip.addStop(thirdStop);

        LocalTime am10Minutes = LocalTime.of(10,00);
        SortedSet<ServiceTime> times = trip.getServiceTimes("statA", "statB", new TimeWindow(am10Minutes, 30));

        // service times
        assertEquals(1, times.size());
        ServiceTime time = times.first();
        assertEquals(TramTime.create(10, 01), time.getDepartureTime());
        assertEquals(TramTime.create(10, 05), time.getArrivalTime());
        assertEquals("svcId", time.getServiceId());
        assertEquals(am10Minutes.plusMinutes(1), time.getLeaves());

        // services times
        times = trip.getServiceTimes("statB", "statA", new TimeWindow(am10Minutes, 30));
        assertEquals(1, times.size());
        time = times.first();
        assertEquals(TramTime.create(10, 06), time.getDepartureTime());
        assertEquals(TramTime.create(10, 10), time.getArrivalTime());
        assertEquals("svcId", time.getServiceId());
        assertEquals(am10Minutes.plusMinutes(6), time.getLeaves());

        // earliest departs
        assertEquals(am10Minutes.plusMinutes(1), trip.earliestDepartFor("statA","statB", new TimeWindow(am10Minutes, 30)).get().getLeaves());
        assertEquals(am10Minutes.plusMinutes(6), trip.earliestDepartFor("statB","statA", new TimeWindow(am10Minutes, 30)).get().getLeaves());
        assertEquals(am10Minutes.plusMinutes(1), trip.earliestDepartFor("statA","statA", new TimeWindow(am10Minutes, 30)).get().getLeaves());
    }

    @Test
    public void shouldFindEarliestDepartCorrectlyCrossingMidnight() throws TramchesterException {

        Stop firstStop = new Stop("stop1", stationA, TramTime.create(23, 45), TramTime.create(23, 46), routeId, serviceId);
        Stop secondStop = new Stop("stop2", stationB, TramTime.create(23, 59), TramTime.create(0, 1), routeId, serviceId);
        Stop thirdStop = new Stop("stop3", stationC, TramTime.create(0,10), TramTime.create(00, 11), routeId, serviceId);

        trip.addStop(firstStop);
        trip.addStop(secondStop);
        trip.addStop(thirdStop);

        SortedSet<ServiceTime> times = trip.getServiceTimes("statA", "statB", new TimeWindow(LocalTime.of(23,40), 30));
        assertEquals(1, times.size());
        assertEquals(TramTime.create(23,46), times.first().getDepartureTime());

        times = trip.getServiceTimes("statA", "statB", new TimeWindow(LocalTime.of(00,10), 30));
        assertEquals(1, times.size());
        assertEquals(TramTime.create(23,46), times.first().getDepartureTime());
    }

    @Test
    public void shouldFindEarliestDepartAfterCrossingMidnight() throws TramchesterException {

        Stop firstStop = new Stop("stop1", stationA, TramTime.create(00, 10), TramTime.create(00, 11), routeId, serviceId);
        Stop secondStop = new Stop("stop2", stationB, TramTime.create(00, 15), TramTime.create(0, 16), routeId, serviceId);
        Stop thirdStop = new Stop("stop3", stationC, TramTime.create(0,24), TramTime.create(00, 25), routeId, serviceId);

        trip.addStop(firstStop);
        trip.addStop(secondStop);
        trip.addStop(thirdStop);

        SortedSet<ServiceTime> times = trip.getServiceTimes("statA", "statB", new TimeWindow(LocalTime.of(23,40), 37));
        assertEquals(1, times.size());
        assertEquals(TramTime.create(00,11), times.first().getDepartureTime());

        times = trip.getServiceTimes("statA", "statB", new TimeWindow(LocalTime.of(00,10), 30));
        assertEquals(1, times.size());
        assertEquals(TramTime.create(00,11), times.first().getDepartureTime());
    }
}
