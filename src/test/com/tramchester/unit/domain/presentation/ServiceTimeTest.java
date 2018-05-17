package com.tramchester.unit.domain.presentation;

import com.tramchester.domain.TramTime;
import com.tramchester.domain.presentation.ServiceTime;
import org.joda.time.LocalTime;
import org.junit.Test;

import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ServiceTimeTest {
    ServiceTime timeA = new ServiceTime(TramTime.create(11, 00), TramTime.create(11,10), "svcId", "headSign", "tripId");
    ServiceTime timeC = new ServiceTime(TramTime.create(11, 00), TramTime.create(11,10), "svcId", "headSign", "tripId");
    ServiceTime timeB = new ServiceTime(TramTime.create(11, 01), TramTime.create(11,9), "svcId", "headSign", "tripId");

    @Test
    public void shouldSetValuesCorrectly() {
        assertEquals(new LocalTime(11, 00), timeA.getDepartureTime());
        assertEquals(new LocalTime(11,10), timeA.getArrivalTime());
        assertEquals("svcId", timeA.getServiceId());
        assertEquals("headSign", timeA.getHeadSign());
    }
    @Test
    public void shouldHaveEquality() {
        assertEquals(timeA, timeC);
        assertEquals(timeC, timeA);
        assertNotEquals(timeA, timeB);
        assertNotEquals(timeC, timeB);
    }

    @Test
    public void shouldCompareByEarliestArriveTime() {
        assertEquals(-1, timeB.compareTo(timeA));
        assertEquals(1, timeA.compareTo(timeB));
        assertEquals(0, timeA.compareTo(timeC));
    }

    @Test
    public void correctOrderingInSortedSet() {
        SortedSet<ServiceTime> set = new TreeSet<>();
        set.add(timeA);
        set.add(timeB);

        assertEquals(timeB, set.first());
        assertEquals(timeA, set.last());
    }

    @Test
    public void correctOrderingInSortedSetAccrossMidnight() {
        SortedSet<ServiceTime> set = new TreeSet<>();
        ServiceTime timeBeforeMid = new ServiceTime(TramTime.create(23, 50), TramTime.create(23, 55), "svcId", "headSign", "tripId");
        ServiceTime timeAfterMid = new ServiceTime(TramTime.create(00, 10), TramTime.create(00, 15), "svcId", "headSign", "tripId");

        set.add(timeAfterMid);
        set.add(timeBeforeMid);

        assertEquals(timeBeforeMid, set.first());
        assertEquals(timeAfterMid, set.last());
    }
}
