package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ServiceTime;
import org.joda.time.LocalTime;
import org.junit.Test;

import java.util.SortedSet;

import static org.junit.Assert.assertEquals;

public class TripTest {

    @Test
    public void shouldModelCircularTripsCorrectly() {
        Trip trip = new Trip("tripId","headSign", "svcId");

        Location stationA = new Station("statA","areaA", "stopNameA", new LatLong(1.0, -1.0), false);
        Location stationB = new Station("statB","areaA", "stopNameB", new LatLong(2.0, -2.0), false);

        Stop firstStop = new Stop(stationA, new LocalTime(10, 00), new LocalTime(10, 01));
        Stop secondStop = new Stop(stationB, new LocalTime(10, 05), new LocalTime(10, 06));
        Stop thirdStop = new Stop(stationA, new LocalTime(10, 10), new LocalTime(10, 10));

        trip.addStop(firstStop);
        trip.addStop(secondStop);
        trip.addStop(thirdStop);

        int am10Minutes = 10 * 60;
        SortedSet<ServiceTime> times = trip.getServiceTimes("statA", "statB", new TimeWindow(am10Minutes, 30));

        // service times
        assertEquals(1, times.size());
        ServiceTime time = times.first();
        assertEquals(new LocalTime(10, 01), time.getDepartureTime());
        assertEquals(new LocalTime(10, 05), time.getArrivalTime());
        assertEquals("svcId", time.getServiceId());
        assertEquals(am10Minutes+1, time.getFromMidnightLeaves());

        // services times
        times = trip.getServiceTimes("statB", "statA", new TimeWindow(am10Minutes, 30));
        assertEquals(1, times.size());
        time = times.first();
        assertEquals(new LocalTime(10, 06), time.getDepartureTime());
        assertEquals(new LocalTime(10, 10), time.getArrivalTime());
        assertEquals("svcId", time.getServiceId());
        assertEquals(am10Minutes+6, time.getFromMidnightLeaves());

        // earliest departs
        assertEquals(am10Minutes+1, trip.earliestDepartFor("statA","statB", new TimeWindow(am10Minutes, 30)).get().getFromMidnightLeaves());
        assertEquals(am10Minutes+6, trip.earliestDepartFor("statB","statA", new TimeWindow(am10Minutes, 30)).get().getFromMidnightLeaves());
        assertEquals(am10Minutes+1, trip.earliestDepartFor("statA","statA", new TimeWindow(am10Minutes, 30)).get().getFromMidnightLeaves());
    }
}
