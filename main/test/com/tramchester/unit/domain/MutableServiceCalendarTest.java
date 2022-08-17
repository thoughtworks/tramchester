package com.tramchester.unit.domain;

import com.tramchester.domain.dates.MutableServiceCalendar;
import com.tramchester.domain.dates.ServiceCalendar;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramServiceDate;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

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

        LocalDate testDay = TestEnv.testDay();

        LocalDate startDate = testDay.minusWeeks(1);
        LocalDate endDate = startDate.plusWeeks(4);


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

        final LocalDate startDate = TestEnv.LocalNow().toLocalDate();
        final LocalDate endDate = TestEnv.testDay().plusWeeks(4);

        ServiceCalendar serviceCalendar = new MutableServiceCalendar(startDate, endDate,
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

        LocalDate localDate = startDate;
        int offset = 0;
        while (new TramServiceDate(localDate).isChristmasPeriod()) {
            localDate = startDate.plusWeeks(offset);
        }

        Assertions.assertFalse(serviceCalendar.operatesOn(TestEnv.testDay().plusWeeks(offset)));
        assertTrue(serviceCalendar.operatesOn(TestEnv.nextSaturday().plusWeeks(offset)));
        assertTrue(serviceCalendar.operatesOn(TestEnv.nextSunday().plusWeeks(offset)));
    }



}
