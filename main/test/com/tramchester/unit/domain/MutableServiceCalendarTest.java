package com.tramchester.unit.domain;

import com.tramchester.domain.MutableServiceCalendar;
import com.tramchester.domain.ServiceCalendar;
import com.tramchester.domain.time.DateRange;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;

import static java.time.DayOfWeek.*;
import static org.junit.jupiter.api.Assertions.*;

class MutableServiceCalendarTest {

    // TODO CalendarDates seen but no calendar??

    @Test
    void shouldSetStartDateAndEndDate() {

        LocalDate startDate = LocalDate.of(2014, 10, 5);
        LocalDate endDate = LocalDate.of(2014, 12, 25);

        ServiceCalendar serviceCalendar = new MutableServiceCalendar(new DateRange(startDate, endDate), TestEnv.allDays());

        assertTrue(serviceCalendar.operatesOn(startDate));
        assertTrue(serviceCalendar.operatesOn(endDate));
        assertTrue(serviceCalendar.operatesOn(LocalDate.of(2014, 11, 30)));

        assertFalse(serviceCalendar.operatesOn(LocalDate.of(2016, 11, 30)));
        assertFalse(serviceCalendar.operatesOn(startDate.minusDays(1)));
        assertFalse(serviceCalendar.operatesOn(endDate.plusDays(1)));
    }

    @Test
    void shouldHaveOverlapAllDays() {
        LocalDate startDate = LocalDate.of(2014, 10, 5);
        LocalDate endDate = LocalDate.of(2014, 12, 25);

        ServiceCalendar serviceCalendar = new MutableServiceCalendar(DateRange.of(startDate, endDate), TestEnv.allDays());
        assertTrue(serviceCalendar.overlapsDatesWith(DateRange.of(startDate, endDate)));
        assertFalse(serviceCalendar.overlapsDatesWith(DateRange.of(endDate.plusDays(2), endDate.plusDays(3))));
    }

    @Test
    void shouldHaveOverlapSomeDays() {
        LocalDate startDate = LocalDate.of(2014, 10, 5);
        LocalDate endDate = LocalDate.of(2014, 12, 25);

        ServiceCalendar serviceCalendar = new MutableServiceCalendar(DateRange.of(startDate, endDate), EnumSet.of(MONDAY));

        assertTrue(serviceCalendar.overlapsDatesAndDaysWith(DateRange.of(startDate.plusDays(1), endDate.minusDays(1)), EnumSet.of(MONDAY)));
        assertTrue(serviceCalendar.overlapsDatesAndDaysWith(DateRange.of(startDate.plusDays(1), endDate.minusDays(1)), EnumSet.of(SUNDAY,MONDAY)));

        assertFalse(serviceCalendar.overlapsDatesAndDaysWith(DateRange.of(startDate.plusDays(1), endDate.minusDays(1)), EnumSet.of(TUESDAY)));
        assertFalse(serviceCalendar.overlapsDatesAndDaysWith(DateRange.of(endDate.plusDays(5), endDate.plusDays(10)), EnumSet.of(MONDAY)));
    }

    @Test
    void shouldCancel() {

        LocalDate startDate = LocalDate.of(2014, 10, 5);
        LocalDate endDate = LocalDate.of(2014, 12, 25);

        MutableServiceCalendar serviceCalendar = new MutableServiceCalendar(new DateRange(startDate, endDate), TestEnv.allDays());

        assertTrue(serviceCalendar.operatesOn(LocalDate.of(2014, 11, 30)));

        serviceCalendar.cancel();

        startDate.datesUntil(endDate.plusDays(1)).forEach(date -> assertFalse(serviceCalendar.operatesOn(date), date.toString()));

        assertTrue(serviceCalendar.operatesNoDays());
    }

    @Test
    void shouldCheckIfServiceHasExceptionDatesRemoved() {
        LocalDate startDate = LocalDate.of(2020, 10, 5);
        LocalDate endDate = LocalDate.of(2020, 12, 10);

        MutableServiceCalendar serviceCalendar = new MutableServiceCalendar(new DateRange(startDate, endDate), TestEnv.allDays());

        LocalDate queryDate = LocalDate.of(2020, 12, 1);
        assertTrue(serviceCalendar.operatesOn(queryDate));
        serviceCalendar.excludeDate(queryDate);
        assertFalse(serviceCalendar.operatesOn(queryDate));
    }

    @Test
    void shouldCheckIfServiceHasExceptionDatesAdded() {

        LocalDate startDate = TestEnv.LocalNow().toLocalDate();
        LocalDate endDate = startDate.plusWeeks(4);

        LocalDate testDay = TestEnv.testDay();

        DayOfWeek dayOfWeek = testDay.getDayOfWeek();
        MutableServiceCalendar serviceCalendar = new MutableServiceCalendar(startDate, endDate, dayOfWeek);

        assertTrue(serviceCalendar.operatesOn(testDay));

        LocalDate outsidePeriod = testDay.plusWeeks(5);
        assertFalse(serviceCalendar.operatesOn(outsidePeriod));

        // same day
        serviceCalendar.includeExtraDate(outsidePeriod);
        assertTrue(serviceCalendar.operatesOn(outsidePeriod));

        // different day - TODO GTFS spec really not so clean on this, but assume we should allow as specifically included
        LocalDate outsidePeriodDiffDayOfWeek = outsidePeriod.plusDays(1);
        assertNotEquals(dayOfWeek, outsidePeriodDiffDayOfWeek.getDayOfWeek());

        serviceCalendar.includeExtraDate(outsidePeriodDiffDayOfWeek);
        assertTrue(serviceCalendar.operatesOn(outsidePeriodDiffDayOfWeek));
    }

    @Test
    void shouldSetWeekendDaysOnService() {

        ServiceCalendar serviceCalendar = new MutableServiceCalendar(TestEnv.LocalNow().toLocalDate(), TestEnv.testDay().plusWeeks(2),
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

        Assertions.assertFalse(serviceCalendar.operatesOn(TestEnv.testDay().plusWeeks(1)));
        assertTrue(serviceCalendar.operatesOn(TestEnv.nextSaturday()));
        assertTrue(serviceCalendar.operatesOn(TestEnv.nextSunday()));
    }



}
