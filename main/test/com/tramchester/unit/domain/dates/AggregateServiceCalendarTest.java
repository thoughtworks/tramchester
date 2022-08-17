package com.tramchester.unit.domain.dates;

import com.tramchester.domain.dates.AggregateServiceCalendar;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.MutableServiceCalendar;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AggregateServiceCalendarTest {

    private EnumSet<DayOfWeek> allDays;

    @BeforeEach
    void setUp() {
        allDays = TestEnv.allDays();
    }

    @Test
    void shouldHaveCorrectDateRange() {
        LocalDate startDate = LocalDate.of(2014, 10, 5);
        LocalDate endDate = LocalDate.of(2014, 10, 25);

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
}
