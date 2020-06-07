package com.tramchester.unit.domain.presentation;

import com.tramchester.domain.presentation.ServiceTime;
import com.tramchester.domain.time.TramTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.SortedSet;
import java.util.TreeSet;

class ServiceTimeTest {
    private ServiceTime timeA;
    private ServiceTime timeC;
    private ServiceTime timeB;

    @BeforeEach
    void beforeEachTestRuns() {
        timeA = new ServiceTime(TramTime.of(11, 0), TramTime.of(11,10), "svcId", "headSign", "tripId");
        timeC = new ServiceTime(TramTime.of(11, 0), TramTime.of(11,10), "svcId", "headSign", "tripId");
        timeB = new ServiceTime(TramTime.of(11, 1), TramTime.of(11,9), "svcId", "headSign", "tripId");
    }

    @Test
    void shouldSetValuesCorrectly() {
        Assertions.assertEquals(TramTime.of(11, 0), timeA.getDepartureTime());
        Assertions.assertEquals(TramTime.of(11,10), timeA.getArrivalTime());
        Assertions.assertEquals("svcId", timeA.getServiceId());
        Assertions.assertEquals("headSign", timeA.getHeadSign());
    }
    @Test
    void shouldHaveEquality() {
        Assertions.assertEquals(timeA, timeC);
        Assertions.assertEquals(timeC, timeA);
        Assertions.assertNotEquals(timeA, timeB);
        Assertions.assertNotEquals(timeC, timeB);
    }

    @Test
    void shouldCompareByEarliestArriveTime() {
        Assertions.assertEquals(-1, timeB.compareTo(timeA));
        Assertions.assertEquals(1, timeA.compareTo(timeB));
        Assertions.assertEquals(0, timeA.compareTo(timeC));
    }

    @Test
    void correctOrderingInSortedSet() {
        SortedSet<ServiceTime> set = new TreeSet<>();
        set.add(timeA);
        set.add(timeB);

        Assertions.assertEquals(timeB, set.first());
        Assertions.assertEquals(timeA, set.last());
    }

    @Test
    void correctOrderingInSortedSetAccrossMidnight() {
        SortedSet<ServiceTime> set = new TreeSet<>();
        ServiceTime timeBeforeMid = new ServiceTime(TramTime.of(23, 50), TramTime.of(23, 55), "svcId", "headSign", "tripId");
        ServiceTime timeAfterMid = new ServiceTime(TramTime.of(0, 10), TramTime.of(0, 15), "svcId", "headSign", "tripId");

        set.add(timeAfterMid);
        set.add(timeBeforeMid);

        Assertions.assertEquals(timeBeforeMid, set.first());
        Assertions.assertEquals(timeAfterMid, set.last());
    }
}
