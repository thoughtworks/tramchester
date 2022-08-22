package com.tramchester.unit.domain;

import com.tramchester.domain.dates.*;
import com.tramchester.testSupport.TestEnv;
import org.jetbrains.annotations.NotNull;
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

        TramDate startDate = TramDate.of(2014, 10, 5);
        TramDate endDate = TramDate.of(2014, 12, 25);

        ServiceCalendar serviceCalendar = new MutableServiceCalendar(new DateRange(startDate, endDate), TestEnv.allDays());

        assertTrue(serviceCalendar.operatesOn(startDate));
        assertTrue(serviceCalendar.operatesOn(endDate));
        assertTrue(serviceCalendar.operatesOn(TramDate.of(2014, 11, 30)));

        assertFalse(serviceCalendar.operatesOn(TramDate.of(2016, 11, 30)));
        assertFalse(serviceCalendar.operatesOn(startDate.minusDays(1)));
        assertFalse(serviceCalendar.operatesOn(endDate.plusDays(1)));
    }

    @Test
    void shouldCancel() {

        TramDate startDate = TramDate.of(2014, 10, 5);
        TramDate endDate = TramDate.of(2014, 12, 25);

        MutableServiceCalendar serviceCalendar = new MutableServiceCalendar(new DateRange(startDate, endDate), TestEnv.allDays());

        assertTrue(serviceCalendar.operatesOn(TramDate.of(2014, 11, 30)));

        serviceCalendar.cancel();

        startDate.datesUntil(endDate.plusDays(1)).forEach(date -> assertFalse(serviceCalendar.operatesOn(date), date.toString()));

        assertTrue(serviceCalendar.operatesNoDays());
    }

    @Test
    void shouldCheckIfServiceHasExceptionDatesRemoved() {
        TramDate startDate = TramDate.of(2020, 10, 5);
        TramDate endDate = TramDate.of(2020, 12, 10);

        MutableServiceCalendar serviceCalendar = new MutableServiceCalendar(new DateRange(startDate, endDate), TestEnv.allDays());

        TramDate queryDate = TramDate.of(2020, 12, 1);
        assertTrue(serviceCalendar.operatesOn(queryDate));
        serviceCalendar.excludeDate(queryDate);
        assertFalse(serviceCalendar.operatesOn(queryDate));
    }

    @Test
    void shouldCheckIfServiceHasExceptionDatesAdded() {

        TramDate testDay = TestEnv.testTramDay();

        TramDate startDate = testDay.minusWeeks(1);
        TramDate endDate = startDate.plusWeeks(4);


        DayOfWeek dayOfWeek = testDay.getDayOfWeek();
        MutableServiceCalendar serviceCalendar = new MutableServiceCalendar(startDate, endDate, dayOfWeek);

        assertTrue(serviceCalendar.operatesOn(testDay));

        TramDate outsidePeriod = testDay.plusWeeks(5);
        assertFalse(serviceCalendar.operatesOn(outsidePeriod));

        // same day
        serviceCalendar.includeExtraDate(outsidePeriod);
        assertTrue(serviceCalendar.operatesOn(outsidePeriod));

        // different day - TODO GTFS spec really not so clean on this, but assume we should allow as specifically included
        TramDate outsidePeriodDiffDayOfWeek = outsidePeriod.plusDays(1);
        assertNotEquals(dayOfWeek, outsidePeriodDiffDayOfWeek.getDayOfWeek());

        serviceCalendar.includeExtraDate(outsidePeriodDiffDayOfWeek);
        assertTrue(serviceCalendar.operatesOn(outsidePeriodDiffDayOfWeek));
    }

    @Test
    void shouldSetWeekendDaysOnService() {

        TramDate testDay = TestEnv.testTramDay();

        final TramDate startDate = TramDate.of(TestEnv.LocalNow().toLocalDate());
        final TramDate endDate = testDay.plusWeeks(4);

        ServiceCalendar serviceCalendar = new MutableServiceCalendar(startDate, endDate,
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

        TramDate localDate = startDate;
        int offset = 0;
        while (new TramServiceDate(localDate).isChristmasPeriod()) {
            localDate = startDate.plusWeeks(offset);
        }

        Assertions.assertFalse(serviceCalendar.operatesOn(testDay.plusWeeks(offset)));
        assertTrue(serviceCalendar.operatesOn(TramDate.of(TestEnv.nextSaturday().plusWeeks(offset))));
        assertTrue(serviceCalendar.operatesOn(TramDate.of(TestEnv.nextSunday().plusWeeks(offset))));
    }

    @Test
    void shouldRespectDateRangesOverlapSameWeekdays() {
        final LocalDate startDateA = LocalDate.of(2020, 11, 5);
        final LocalDate endDateA = LocalDate.of(2020, 11, 25);

        DateRange dateRange = DateRange.of(startDateA, endDateA);

        MutableServiceCalendar calendarA = new MutableServiceCalendar(dateRange, EnumSet.of(MONDAY, TUESDAY, WEDNESDAY));

        MutableServiceCalendar calendarB = new MutableServiceCalendar(dateRange, EnumSet.of(WEDNESDAY, THURSDAY));

        assertTrue(calendarA.anyDateOverlaps(calendarB));
        assertTrue(calendarB.anyDateOverlaps(calendarA));
    }

    @Test
    void shouldHaveSimpleDateOverlap() {

        LocalDate startDate = LocalDate.of(2020, 11, 5);
        LocalDate endDate = LocalDate.of(2020, 11, 25);

        EnumSet<DayOfWeek> monday = EnumSet.of(MONDAY);

        ServiceCalendar calendarA = createCalendar(startDate, endDate, monday);

        ServiceCalendar calendarC = createCalendar(startDate, endDate, monday);

        // should match
        assertTrue(calendarA.anyDateOverlaps(calendarC));
        assertTrue(calendarC.anyDateOverlaps(calendarC));

    }

    @Test
    void shouldHaveDateOverlap() {

        LocalDate startDate = LocalDate.of(2020, 11, 5);
        LocalDate endDate = LocalDate.of(2020, 11, 25);

        EnumSet<DayOfWeek> monday = EnumSet.of(MONDAY);

        ServiceCalendar calA = createCalendar(startDate, endDate, monday);
        ServiceCalendar calB = createCalendar(startDate, endDate, EnumSet.of(DayOfWeek.SUNDAY));
        ServiceCalendar calC = createCalendar(startDate.minusDays(10), startDate.minusDays(5),  monday);
        ServiceCalendar calD = createCalendar(startDate, endDate, monday);

        assertTrue(calA.anyDateOverlaps(calA));

        // wrong operating days
        assertFalse(calA.anyDateOverlaps(calB));

        // before dates
        assertFalse(calA.anyDateOverlaps(calC));

        // should match
        assertTrue(calA.anyDateOverlaps(calD));
    }

    @NotNull
    private MutableServiceCalendar createCalendar(LocalDate startDate, LocalDate endDate, EnumSet<DayOfWeek> daysOfWeek) {
        return new MutableServiceCalendar(DateRange.of(startDate, endDate), daysOfWeek);
    }


}
