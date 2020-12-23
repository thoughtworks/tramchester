package com.tramchester.unit.domain;

import com.tramchester.domain.ServiceCalendar;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ServiceCalendarTest {

    // TODO CalendarDates seen but no calendar??

//    @Test
//    void shouldReportNoDatesSetIncludingExceptions() {
//        Service service = new Service("svcXXX", TestEnv.getTestRoute(IdFor.createId("ROUTE66")));
//
//        service.setDays(true, false, false, false, false, false, false);
//
//        assertTrue(service.HasMissingDates()); // missing dates
//
//        service.addExceptionDate(TestEnv.testDay(), CalendarDateData.ADDED);
//        Assertions.assertFalse(service.HasMissingDates());
//    }

    @Test
    void shouldSetStartDateAndEndDate() {

        LocalDate startDate = LocalDate.of(2014, 10, 5);
        LocalDate endDate = LocalDate.of(2014, 12, 25);

        ServiceCalendar serviceCalendar = new ServiceCalendar(startDate, endDate, TestEnv.allDays());

        assertTrue(serviceCalendar.operatesOn(startDate));
        assertTrue(serviceCalendar.operatesOn(endDate));
        assertTrue(serviceCalendar.operatesOn(LocalDate.of(2014,11,30)));

        assertFalse(serviceCalendar.operatesOn(LocalDate.of(2016,11,30)));
        assertFalse(serviceCalendar.operatesOn(startDate.minusDays(1)));
        assertFalse(serviceCalendar.operatesOn(endDate.plusDays(1)));
    }

    @Test
    void shouldCheckIfServiceHasExceptionDatesRemoved() {
        LocalDate startDate = LocalDate.of(2020, 10, 5);
        LocalDate endDate = LocalDate.of(2020, 12, 10);

        ServiceCalendar serviceCalendar = new ServiceCalendar(startDate, endDate, TestEnv.allDays());

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
        ServiceCalendar serviceCalendar = new ServiceCalendar(startDate, endDate, dayOfWeek);

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

        ServiceCalendar serviceCalendar = new ServiceCalendar(TestEnv.LocalNow().toLocalDate(), TestEnv.testDay().plusWeeks(2),
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

        Assertions.assertFalse(serviceCalendar.operatesOn(TestEnv.testDay().plusWeeks(1)));
        assertTrue(serviceCalendar.operatesOn(TestEnv.nextSaturday()));
        assertTrue(serviceCalendar.operatesOn(TestEnv.nextSunday()));
    }



}
