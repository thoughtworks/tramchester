package com.tramchester.domain.presentation;

import org.junit.Test;

import org.joda.time.LocalTime;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ServiceTimeTest {
    ServiceTime timeA = new ServiceTime(new LocalTime(11, 00), new LocalTime(11,10), "svcId", "headSign", "tripId");
    ServiceTime timeC = new ServiceTime(new LocalTime(11, 00), new LocalTime(11,10), "svcId", "headSign", "tripId");
    ServiceTime timeB = new ServiceTime(new LocalTime(11, 01), new LocalTime(11,9), "svcId", "headSign", "tripId");

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
        ServiceTime timeBeforeMid = new ServiceTime(new LocalTime(23, 50), new LocalTime(23, 55), "svcId", "headSign", "tripId");
        ServiceTime timeAfterMid = new ServiceTime(new LocalTime(00, 10), new LocalTime(00, 15), "svcId", "headSign", "tripId");

        set.add(timeAfterMid);
        set.add(timeBeforeMid);

        assertEquals(timeBeforeMid, set.first());
        assertEquals(timeAfterMid, set.last());
    }
}
