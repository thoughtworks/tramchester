package com.tramchester.unit.domain.dates;

import com.tramchester.domain.dates.*;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.neo4j.cypher.internal.expressions.functions.E;

import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static org.junit.jupiter.api.Assertions.*;

public class AggregateServiceCalendarTest {

    private EnumSet<DayOfWeek> allDays;

    @BeforeEach
    void setUp() {
        allDays = TestEnv.allDays();
    }

    // TODO Move more route tests into here

    // TODO Scenarios where individual services are cancelled meaning cannot just check daterange at end of operatesOn()

    @Test
    void shouldHaveCorrectDateRange() {
        TramDate startDate = TramDate.of(2014, 10, 5);
        TramDate endDate = TramDate.of(2014, 10, 25);

        DateRange rangeA = DateRange.of(startDate, endDate);
        DateRange rangeB = DateRange.of(startDate.plusDays(1), endDate.minusDays(1));

        MutableServiceCalendar calendarA = new MutableServiceCalendar(rangeA, allDays);
        MutableServiceCalendar calendarB = new MutableServiceCalendar(rangeB, allDays);

        AggregateServiceCalendar aggregate1 = new AggregateServiceCalendar(Arrays.asList(calendarA, calendarB));

        assertEquals(rangeA, aggregate1.getDateRange());

        DateRange rangeC = DateRange.of(startDate.minusDays(1), endDate.plusDays(1));
        MutableServiceCalendar calendarC = new MutableServiceCalendar(rangeC, allDays);

        AggregateServiceCalendar aggregate2 = new AggregateServiceCalendar(Arrays.asList(calendarA, calendarC));
        assertEquals(rangeC, aggregate2.getDateRange());

        DateRange rangeD = DateRange.of(startDate.minusDays(10), endDate.minusDays(1));
        MutableServiceCalendar calendarD = new MutableServiceCalendar(rangeD, allDays);

        AggregateServiceCalendar aggregate3 = new AggregateServiceCalendar(Arrays.asList(calendarA, calendarD));
        assertEquals(DateRange.of(startDate.minusDays(10), endDate), aggregate3.getDateRange());

        DateRange rangeE = DateRange.of(startDate, endDate.plusDays(1));
        MutableServiceCalendar calendarE = new MutableServiceCalendar(rangeE, allDays);

        AggregateServiceCalendar aggregate4 = new AggregateServiceCalendar(Arrays.asList(calendarA, calendarE));
        assertEquals(DateRange.of(startDate, endDate.plusDays(1)), aggregate4.getDateRange());

    }

    @Test
    void shouldRespectDateRangesOnServicesWithDaysOfWeek() {
        final TramDate startDateA = TramDate.of(2020, 11, 5);
        final TramDate endDateA = TramDate.of(2020, 11, 25);

        final TramDate startDateB = endDateA.plusDays(1);
        final TramDate endDateB = TramDate.of(2020, 12, 25);

        MutableServiceCalendar calendarA = new MutableServiceCalendar(DateRange.of(startDateA, endDateA), EnumSet.of(TUESDAY));

        MutableServiceCalendar calendarB = new MutableServiceCalendar(DateRange.of(startDateB, endDateB), EnumSet.of(THURSDAY));

        AggregateServiceCalendar serviceCalendar = new AggregateServiceCalendar(Arrays.asList(calendarA, calendarB));

        assertFalse(serviceCalendar.operatesOn(startDateA.minusDays(1)));
        assertFalse(serviceCalendar.operatesOn(endDateB.plusDays(1)));

        TramDate date = startDateA;
        while (date.isBefore(endDateA)) {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            if (dayOfWeek == TUESDAY) {
                assertTrue(serviceCalendar.operatesOn(date), "should be available on " + date + " " + dayOfWeek);
            } else {
                assertFalse(serviceCalendar.operatesOn(date), "should NOT be available on " + date+ " " + dayOfWeek);
            }
            date = date.plusDays(1);
        }

        date = startDateB;
        while (date.isBefore(endDateB)) {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            if (date.getDayOfWeek()==THURSDAY) {
                assertTrue(serviceCalendar.operatesOn(date), "should be available on " + date+ " " + dayOfWeek);
            } else {
                assertFalse(serviceCalendar.operatesOn(date), "should NOT be available on " + date+ " " + dayOfWeek);
            }
            date = date.plusDays(1);
        }

    }

}