package com.tramchester.domain.presentation;

import org.junit.Test;

import java.time.LocalTime;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ServiceTimeTest {
    ServiceTime timeA = new ServiceTime(LocalTime.of(11, 00), LocalTime.of(11,10), "svcId", "headSign", "tripId");
    ServiceTime timeC = new ServiceTime(LocalTime.of(11, 00), LocalTime.of(11,10), "svcId", "headSign", "tripId");
    ServiceTime timeB = new ServiceTime(LocalTime.of(11, 01), LocalTime.of(11,9), "svcId", "headSign", "tripId");

    @Test
    public void shouldSetValuesCorrectly() {
        assertEquals(LocalTime.of(11, 00), timeA.getDepartureTime());
        assertEquals(LocalTime.of(11,10), timeA.getArrivalTime());
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
}
