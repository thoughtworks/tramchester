package com.tramchester.unit.domain.dates;

import com.tramchester.domain.dates.*;
import com.tramchester.testSupport.TestEnv;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

import static java.time.DayOfWeek.*;
import static org.junit.jupiter.api.Assertions.*;

public class AggregateServiceCalendarTest {

    private EnumSet<DayOfWeek> allDays;
    private final EnumSet<DayOfWeek> noDays = EnumSet.noneOf(DayOfWeek.class);

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

    @Test
    void shouldRespectDateRangesOnServicesNoOverlap() {
        final TramDate startDateA = TramDate.of(2020, 11, 5);
        final TramDate endDateA = TramDate.of(2020, 11, 25);

        final TramDate startDateB = TramDate.of(2020, 12, 5);
        final TramDate endDateB = TramDate.of(2020, 12, 25);

        MutableServiceCalendar calendarA = new MutableServiceCalendar(DateRange.of(startDateA, endDateA), allDays);

        MutableServiceCalendar calendarB = new MutableServiceCalendar(DateRange.of(startDateB, endDateB), allDays);

        AggregateServiceCalendar aggregateServiceCalendar = new AggregateServiceCalendar(Arrays.asList(calendarA, calendarB));

        assertFalse(aggregateServiceCalendar.operatesOn(startDateA.minusDays(1)));
        assertFalse(aggregateServiceCalendar.operatesOn(endDateA.plusDays(1)));

        assertFalse(aggregateServiceCalendar.operatesOn(startDateB.minusDays(1)));
        assertFalse(aggregateServiceCalendar.operatesOn(endDateB.plusDays(1)));

        TramDate date = startDateA;
        while (date.isBefore(endDateA)) {
            assertTrue(aggregateServiceCalendar.operatesOn(date), "should be available on " + date);
            date = date.plusDays(1);
        }

        date = startDateB;
        while (date.isBefore(endDateB)) {
            assertTrue(aggregateServiceCalendar.operatesOn(date), "should be available on " + date);
            date = date.plusDays(1);
        }
    }

    @Test
    void shouldRespectDateRangesOnServicesWithOverlap() {
        final TramDate startDateA = TramDate.of(2020, 11, 5);
        final TramDate endDateA = TramDate.of(2020, 11, 25);

        final TramDate startDateB = endDateA.minusDays(1);
        final TramDate endDateB = TramDate.of(2020, 12, 25);

        MutableServiceCalendar calendarA = new MutableServiceCalendar(DateRange.of(startDateA, endDateA), allDays);
        MutableServiceCalendar calendarB = new MutableServiceCalendar(DateRange.of(startDateB, endDateB), allDays);

        AggregateServiceCalendar route = new AggregateServiceCalendar(Arrays.asList(calendarA, calendarB));

        assertFalse(route.operatesOn(startDateA.minusDays(1)));
        assertFalse(route.operatesOn(endDateB.plusDays(1)));

        TramDate date = startDateA;
        while (date.isBefore(endDateB)) {
            assertTrue(route.operatesOn(date), "should be available on " + date);
            date = date.plusDays(1);
        }

    }

    @Test
    void shouldRespectDateRangesOnServicesWithNoAdditionalDayOverlap() {
        final TramDate startDate = TramDate.of(2020, 11, 5);
        final TramDate endDate = TramDate.of(2020, 11, 25);

        MutableServiceCalendar calendarA = new MutableServiceCalendar(DateRange.of(startDate, endDate), allDays);
        MutableServiceCalendar calendarB = new MutableServiceCalendar(DateRange.of(startDate, endDate), noDays);

        assertFalse(calendarA.anyDateOverlaps(calendarB));
        assertFalse(calendarB.anyDateOverlaps(calendarA));

        AggregateServiceCalendar aggregateServiceCalendarA = new AggregateServiceCalendar(Collections.singleton(calendarA));
        AggregateServiceCalendar aggregateServiceCalendarB = new AggregateServiceCalendar(Collections.singleton(calendarB));

        assertFalse(aggregateServiceCalendarA.anyDateOverlaps(aggregateServiceCalendarB));
        assertFalse(aggregateServiceCalendarB.anyDateOverlaps(aggregateServiceCalendarA));
    }

    @Test
    void shouldRespectDateRangesOnServicesWithAdditionalDayOverlap() {
        final TramDate startDate = TramDate.of(2020, 11, 5);
        final TramDate endDate = TramDate.of(2020, 11, 25);

        MutableServiceCalendar calendarA = new MutableServiceCalendar(DateRange.of(startDate, endDate), allDays);

        MutableServiceCalendar calendarB = new MutableServiceCalendar(DateRange.of(startDate, endDate), noDays);
        calendarB.includeExtraDate(startDate.plusDays(1));

        assertTrue(calendarA.anyDateOverlaps(calendarB));
        assertTrue(calendarB.anyDateOverlaps(calendarA));

        AggregateServiceCalendar aggregateServiceCalendarA = new AggregateServiceCalendar(Collections.singleton(calendarA));
        AggregateServiceCalendar aggregateServiceCalendarB = new AggregateServiceCalendar(Collections.singleton(calendarB));

        assertTrue(aggregateServiceCalendarA.anyDateOverlaps(aggregateServiceCalendarB));
        assertTrue(aggregateServiceCalendarB.anyDateOverlaps(aggregateServiceCalendarB));

    }

    @Test
    void shouldRespectDateRangesOnServicesWithAdditionalAndExcludeNegatingAnyOverlap() {
        final TramDate startDate = TramDate.of(2020, 11, 5);
        final TramDate endDate = TramDate.of(2020, 11, 25);

        TramDate specialDay = TramDate.of(2020,11,15);

        DateRange dateRange = DateRange.of(startDate, endDate);

        MutableServiceCalendar calendarA = new MutableServiceCalendar(dateRange, allDays);
        calendarA.excludeDate(specialDay); // exclude

        MutableServiceCalendar calendarB = new MutableServiceCalendar(dateRange, noDays);
        calendarB.includeExtraDate(specialDay); // include

        assertFalse(calendarA.anyDateOverlaps(calendarB));
        assertFalse(calendarB.anyDateOverlaps(calendarA));

        AggregateServiceCalendar aggregateServiceCalendarA = new AggregateServiceCalendar(Collections.singleton(calendarA));
        AggregateServiceCalendar aggregateServiceCalendarB = new AggregateServiceCalendar(Collections.singleton(calendarB));

        assertFalse(aggregateServiceCalendarA.anyDateOverlaps(aggregateServiceCalendarB));
        assertFalse(aggregateServiceCalendarB.anyDateOverlaps(aggregateServiceCalendarA));

    }

    @Test
    void shouldRespectDateRangesOnServicesNoOverlapExcludedDays() {
        final TramDate startDateA = TramDate.of(2020, 11, 5);
        final TramDate endDateA = TramDate.of(2020, 11, 25);

        final TramDate startDateB = TramDate.of(2020, 12, 5);
        final TramDate endDateB = TramDate.of(2020, 12, 25);

        MutableServiceCalendar calendarA = new MutableServiceCalendar(DateRange.of(startDateA, endDateA), allDays);
        TramDate exclusionOne = TramDate.of(2020, 11, 10);
        calendarA.excludeDate(exclusionOne);

        MutableServiceCalendar calendarB = new MutableServiceCalendar(DateRange.of(startDateB, endDateB), allDays);
        TramDate exclusionTwo = TramDate.of(2020, 12, 10);
        calendarB.excludeDate(exclusionTwo);

        AggregateServiceCalendar aggregateServiceCalendar = new AggregateServiceCalendar(Arrays.asList(calendarA, calendarB));

        assertFalse(aggregateServiceCalendar.operatesOn(exclusionOne));
        assertFalse(aggregateServiceCalendar.operatesOn(exclusionTwo));
    }

    @Test
    void shouldRespectDateRangesOnServicesIncludedDays() {
        final TramDate startDate = TramDate.of(2020, 11, 5);
        final TramDate endDate = TramDate.of(2020, 11, 25);

        DateRange dateRange = DateRange.of(startDate, endDate);

        MutableServiceCalendar calendarA = new MutableServiceCalendar(dateRange, noDays);
        TramDate additionOne = TramDate.of(2020, 11, 10);
        calendarA.includeExtraDate(additionOne);

        MutableServiceCalendar calendarB = new MutableServiceCalendar(dateRange, noDays);
        TramDate additionTwo = TramDate.of(2020, 11, 12);
        calendarB.includeExtraDate(additionTwo);

        AggregateServiceCalendar aggregateServiceCalendar = new AggregateServiceCalendar(Arrays.asList(calendarA, calendarB));

        assertTrue(aggregateServiceCalendar.operatesOn(additionOne));
        assertTrue(aggregateServiceCalendar.operatesOn(additionTwo));

        dateRange.stream().filter(date -> !(date.equals(additionOne) || date.equals(additionTwo))).
                forEach(date -> assertFalse(aggregateServiceCalendar.operatesOn(date), "should not be availble on " + date));

    }

    @Test
    void shouldRespectDateRangesOverlapDifferentAdditionalDays() {
        final TramDate startDate = TramDate.of(2020, 11, 5);
        final TramDate endDate = TramDate.of(2020, 11, 25);

        DateRange dateRange = DateRange.of(startDate, endDate);

        MutableServiceCalendar calendarA = new MutableServiceCalendar(dateRange, noDays);
        TramDate additionOne = TramDate.of(2020, 11, 10);
        calendarA.includeExtraDate(additionOne);

        MutableServiceCalendar calendarB = new MutableServiceCalendar(dateRange, noDays);
        TramDate additionTwo = TramDate.of(2020, 11, 12);
        calendarB.includeExtraDate(additionTwo);

        assertFalse(calendarA.anyDateOverlaps(calendarB));
        assertFalse(calendarB.anyDateOverlaps(calendarA));

        AggregateServiceCalendar aggregateServiceCalendarA = new AggregateServiceCalendar(Collections.singleton(calendarA));
        AggregateServiceCalendar aggregateServiceCalendarB = new AggregateServiceCalendar(Collections.singleton(calendarB));

        assertFalse(aggregateServiceCalendarA.anyDateOverlaps(aggregateServiceCalendarB));
        assertFalse(aggregateServiceCalendarB.anyDateOverlaps(aggregateServiceCalendarA));

    }

    @Test
    void shouldRespectDateRangesOverlapSameAdditionalDays() {
        final TramDate startDate = TramDate.of(2020, 11, 5);
        final TramDate endDate = TramDate.of(2020, 11, 25);

        DateRange dateRange = DateRange.of(startDate, endDate);

        MutableServiceCalendar calendarA = new MutableServiceCalendar(dateRange, noDays);
        TramDate addition = TramDate.of(2020, 11, 10);
        calendarA.includeExtraDate(addition);

        MutableServiceCalendar calendarB = new MutableServiceCalendar(dateRange, noDays);
        calendarB.includeExtraDate(addition);

        assertTrue(calendarA.anyDateOverlaps(calendarB));
        assertTrue(calendarB.anyDateOverlaps(calendarA));

        AggregateServiceCalendar aggregateServiceCalendarA = new AggregateServiceCalendar(Collections.singleton(calendarA));
        AggregateServiceCalendar aggregateServiceCalendarB = new AggregateServiceCalendar(Collections.singleton(calendarB));

        assertTrue(aggregateServiceCalendarA.anyDateOverlaps(aggregateServiceCalendarB));
        assertTrue(aggregateServiceCalendarB.anyDateOverlaps(aggregateServiceCalendarA));
    }

    @Test
    void shouldRespectDateRangesOverlapNotSameWeekdaysWithAddition() {
        final TramDate startDateA = TramDate.of(2020, 11, 5);
        final TramDate endDateA = TramDate.of(2020, 11, 25);

        DateRange dateRange = DateRange.of(startDateA, endDateA);


        MutableServiceCalendar calendarA = new MutableServiceCalendar(dateRange, EnumSet.of(MONDAY, TUESDAY));
        TramDate addition = TramDate.of(2020, 11, 10);
        calendarA.includeExtraDate(addition);

        MutableServiceCalendar calendarB = new MutableServiceCalendar(dateRange, EnumSet.of(WEDNESDAY, THURSDAY));
        calendarB.includeExtraDate(addition);

        assertTrue(calendarA.anyDateOverlaps(calendarB));
        assertTrue(calendarB.anyDateOverlaps(calendarA));

        AggregateServiceCalendar aggregateServiceCalendarA = new AggregateServiceCalendar(Collections.singleton(calendarA));
        AggregateServiceCalendar aggregateServiceCalendarB = new AggregateServiceCalendar(Collections.singleton(calendarB));

        assertTrue(aggregateServiceCalendarA.anyDateOverlaps(aggregateServiceCalendarB));
        assertTrue(aggregateServiceCalendarB.anyDateOverlaps(aggregateServiceCalendarA));
    }

    @Test
    void shouldRespectDateRangesOverlapNotSameWeekdaysWithOutAddition() {
        final TramDate startDateA = TramDate.of(2020, 11, 5);
        final TramDate endDateA = TramDate.of(2020, 11, 25);

        DateRange dateRange = DateRange.of(startDateA, endDateA);

        MutableServiceCalendar calendarA = new MutableServiceCalendar(dateRange, EnumSet.of(MONDAY, TUESDAY));

        MutableServiceCalendar calendarB = new MutableServiceCalendar(dateRange, EnumSet.of(WEDNESDAY, THURSDAY));

        assertFalse(calendarA.anyDateOverlaps(calendarB));
        assertFalse(calendarB.anyDateOverlaps(calendarA));

        AggregateServiceCalendar aggregateServiceCalendarA = new AggregateServiceCalendar(Collections.singleton(calendarA));
        AggregateServiceCalendar aggregateServiceCalendarB = new AggregateServiceCalendar(Collections.singleton(calendarB));

        assertFalse(aggregateServiceCalendarA.anyDateOverlaps(aggregateServiceCalendarB));
        assertFalse(aggregateServiceCalendarB.anyDateOverlaps(aggregateServiceCalendarA));
    }

    @Test
    void shouldRespectDateRangesOnServicesInclusionOverridesExclusion() {
        final TramDate startDateA = TramDate.of(2020, 11, 5);
        final TramDate endDateA = TramDate.of(2020, 11, 25);

        TramDate specialDay = TramDate.of(2020, 11, 10);

        // does not run on special day
        MutableServiceCalendar calendarA = new MutableServiceCalendar(DateRange.of(startDateA, endDateA), allDays);
        calendarA.excludeDate(specialDay);

        // runs ONLY on special day
        MutableServiceCalendar calendarB = new MutableServiceCalendar(DateRange.of(startDateA, endDateA), noDays);
        calendarB.includeExtraDate(specialDay);

        AggregateServiceCalendar aggregateServiceCalendar = new AggregateServiceCalendar(Arrays.asList(calendarA, calendarB));

        assertTrue(aggregateServiceCalendar.operatesOn(specialDay));

    }

    @Test
    void shouldRespectAdditionalDaysOnService() {
        TramDate startDate = TramDate.of(2020, 11, 5);
        TramDate endDate = TramDate.of(2020, 11, 25);

        MutableServiceCalendar calendar = new MutableServiceCalendar(DateRange.of(startDate, endDate), noDays);
        TramDate extraRunningDate = TramDate.of(2020, 11, 10);
        calendar.includeExtraDate(extraRunningDate);

        assertTrue(calendar.operatesOn(extraRunningDate));

        AggregateServiceCalendar aggregateServiceCalendar = new AggregateServiceCalendar(Collections.singleton(calendar));

        assertTrue(aggregateServiceCalendar.operatesOn(extraRunningDate));
    }

    @Test
    void shouldHaveDateOverlap() {

        TramDate startDate = TramDate.of(2020, 11, 5);
        TramDate endDate = TramDate.of(2020, 11, 25);

        EnumSet<DayOfWeek> monday = EnumSet.of(MONDAY);

        ServiceCalendar serviceA = createServiceCal(startDate, endDate, monday);
        ServiceCalendar serviceB = createServiceCal(startDate, endDate, EnumSet.of(DayOfWeek.SUNDAY));
        ServiceCalendar serviceC = createServiceCal(startDate.minusDays(10), startDate.minusDays(5), monday);
        ServiceCalendar serviceD = createServiceCal(startDate, endDate, monday);

        assertTrue(serviceA.anyDateOverlaps(serviceA));

        // wrong operating days
        assertFalse(serviceA.anyDateOverlaps(serviceB));

        // before dates
        assertFalse(serviceA.anyDateOverlaps(serviceC));

        // should match
        assertTrue(serviceA.anyDateOverlaps(serviceD));
    }

    @Test
    void shouldHaveNumberOfDaysOperating() {
        TramDate startDate = TramDate.of(2022, 8, 1); // a monday
        TramDate endDate = TramDate.of(2022, 8, 14);

        ServiceCalendar calendarA = new MutableServiceCalendar(DateRange.of(startDate, endDate), EnumSet.of(MONDAY));
        assertEquals(2, calendarA.numberDaysOperating());

        ServiceCalendar calendarB = new MutableServiceCalendar(DateRange.of(startDate, endDate), EnumSet.of(FRIDAY));
        assertEquals(2, calendarB.numberDaysOperating());

        AggregateServiceCalendar aggregateServiceCalendar = new AggregateServiceCalendar(Arrays.asList(calendarA, calendarB));

        assertEquals(4, aggregateServiceCalendar.numberDaysOperating());

    }

    @Test
    void shouldHaveNumberOfDaysOperatingAddditions() {
        TramDate startDate = TramDate.of(2022, 8, 1); // a monday
        TramDate endDate = TramDate.of(2022, 8, 14);

        MutableServiceCalendar calendarA = new MutableServiceCalendar(DateRange.of(startDate, endDate), EnumSet.of(MONDAY));
        assertEquals(2, calendarA.numberDaysOperating());

        MutableServiceCalendar calendarB = new MutableServiceCalendar(DateRange.of(startDate, endDate), EnumSet.of(FRIDAY));
        assertEquals(2, calendarB.numberDaysOperating());

        assertEquals(4, createAggregrate(calendarA, calendarB).numberDaysOperating());

        calendarA.includeExtraDate(startDate.plusDays(1));
        assertEquals(5, createAggregrate(calendarA, calendarB).numberDaysOperating());

        calendarB.includeExtraDate(startDate.plusDays(1));
        assertEquals(5, createAggregrate(calendarA, calendarB).numberDaysOperating());

        calendarB.includeExtraDate(startDate.plusDays(2));
        assertEquals(6, createAggregrate(calendarA, calendarB).numberDaysOperating());

    }

    @Test
    void shouldHaveNumberOfDaysOperatingRemovables() {
        TramDate startDate = TramDate.of(2022, 8, 1); // a monday
        TramDate endDate = TramDate.of(2022, 8, 14);

        MutableServiceCalendar calendarA = new MutableServiceCalendar(DateRange.of(startDate, endDate), EnumSet.of(MONDAY));
        assertEquals(2, calendarA.numberDaysOperating());

        MutableServiceCalendar calendarB = new MutableServiceCalendar(DateRange.of(startDate, endDate), EnumSet.of(MONDAY));
        assertEquals(2, calendarB.numberDaysOperating());

        assertEquals(2, createAggregrate(calendarA, calendarB).numberDaysOperating());

        calendarA.excludeDate(startDate); // only one
        assertEquals(2, createAggregrate(calendarA, calendarB).numberDaysOperating());

        calendarB.excludeDate(startDate); // now both
        assertEquals(1, createAggregrate(calendarA, calendarB).numberDaysOperating());

    }

    @NotNull
    private AggregateServiceCalendar createAggregrate(MutableServiceCalendar calendarA, ServiceCalendar calendarB) {
        return new AggregateServiceCalendar(Arrays.asList(calendarA, calendarB));
    }

    private AggregateServiceCalendar createCalendar(TramDate startDate, TramDate endDate, EnumSet<DayOfWeek> days) {
        ServiceCalendar calendar = new MutableServiceCalendar(DateRange.of(startDate, endDate), days);
        return new AggregateServiceCalendar(Collections.singleton(calendar));
    }

    private MutableServiceCalendar createServiceCal(TramDate startDate, TramDate endDate, EnumSet<DayOfWeek> dayOfWeeks) {
        return new MutableServiceCalendar(DateRange.of(startDate, endDate), dayOfWeeks);
    }


}
