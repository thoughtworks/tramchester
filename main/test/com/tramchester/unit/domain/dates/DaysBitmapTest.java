package com.tramchester.unit.domain.dates;

import com.tramchester.domain.dates.*;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.EnumSet;

import static java.time.DayOfWeek.*;
import static org.junit.jupiter.api.Assertions.*;

public class DaysBitmapTest {

    private TramDate when;
    private long epochDay;

    @BeforeEach
    void beforeEachTestRuns() {
        when = TestEnv.testDay();
        epochDay = when.toEpochDay();
    }

    @Test
    void shouldSetADay() {
        DaysBitmap days = new DaysBitmap(epochDay, 100);

        assertEquals(epochDay, days.getBeginningEpochDay());

        assertEquals(0, days.numberSet());
        assertTrue(days.noneSet());

        TramDate date = when.plusDays(5);
        assertFalse(days.isSet(date));

        days.set(date);
        assertTrue(days.isSet(date));

        days.clear(date);
        assertFalse(days.isSet(date));
        assertTrue(days.noneSet());

        for (int i = 0; i < 100; i++) {
            days.set(when.plusDays(i));
        }
        assertEquals(100, days.numberSet());

        days.clearAll();
        assertTrue(days.noneSet());

    }

    @Test
    void shouldSetDaysOfWeek() {
        DaysBitmap days = new DaysBitmap(epochDay, 100);

        EnumSet<DayOfWeek> daysOfWeek = EnumSet.of(MONDAY, WEDNESDAY, SATURDAY);
        days.setDaysOfWeek(daysOfWeek);

        for (int i = 0; i < 100; i++) {
            TramDate date = when.plusDays(i);
            if (daysOfWeek.contains(date.getDayOfWeek())) {
                assertTrue(days.isSet(date), date.toString());
            } else {
                assertFalse(days.isSet(date), date.toString());
            }
        }
    }

    @Test
    void shouldSetAllDaysAsExpected() {

        TramDate startDate = when;
        int days = 5;
        TramDate endDate = when.plusDays(days);

        DaysBitmap daysBitmap = new DaysBitmap(epochDay, 5);

        daysBitmap.setDaysOfWeek(TestEnv.allDays());

        assertTrue(daysBitmap.isSet(startDate));
        assertTrue(daysBitmap.isSet(endDate));
    }

    @Test
    void shouldGiveExpectedResultsWhenMatchingDateAndSize() {
        DaysBitmap first = new DaysBitmap(epochDay,14);
        DaysBitmap second = new DaysBitmap(epochDay, 14);

        assertFalse(first.anyOverlap(second));
        assertFalse(second.anyOverlap(first));

        TramDate date = when.plusDays(1);

        first.set(date);
        assertFalse(first.anyOverlap(second));
        second.set(date);
        assertTrue(first.anyOverlap(second));
    }

    @Test
    void shouldGiveExpectedResultsWhenOverlapDateAtEnd() {
        DaysBitmap first = new DaysBitmap(epochDay,14);
        DaysBitmap second = new DaysBitmap(epochDay+5, 14);

        assertFalse(first.anyOverlap(second));
        assertFalse(second.anyOverlap(first));

        TramDate date = when.plusDays(1 + 5);

        first.set(date);
        assertFalse(first.anyOverlap(second));
        second.set(date);
        assertTrue(first.anyOverlap(second));
    }

    @Test
    void shouldGiveExpectedResultsWhenOverlapDateAtStart() {
        DaysBitmap first = new DaysBitmap(epochDay,14);
        DaysBitmap second = new DaysBitmap(epochDay-5, 14);

        assertFalse(first.anyOverlap(second));
        assertFalse(second.anyOverlap(first));

        TramDate date = when.plusDays(1);

        first.set(date);
        assertFalse(first.anyOverlap(second));
        second.set(date);
        assertTrue(first.anyOverlap(second));
    }

    @Test
    void shouldInsertAnotherOverlapsBeforeSamller() {
        DaysBitmap first = new DaysBitmap(epochDay,14);
        DaysBitmap second = new DaysBitmap(epochDay-5, 10);

        TramDate date = when.plusDays(1);
        second.set(date);

        first.insert(second);
        assertTrue(first.isSet(date));
    }

    @Test
    void shouldInsertAnotherOverlapsBeforeSLarger() {
        DaysBitmap first = new DaysBitmap(epochDay,14);
        DaysBitmap second = new DaysBitmap(epochDay-5, 28);

        TramDate date = when.plusDays(1);
        second.set(date);
        second.set(when.plusDays(28-5)); // set one bit that does not overlap

        first.insert(second);
        assertTrue(first.isSet(date));
    }

    @Test
    void shouldInsertAnotherOverlapsAfter() {
        DaysBitmap first = new DaysBitmap(epochDay,14);
        DaysBitmap second = new DaysBitmap(epochDay+5, 18);

        TramDate date = when.plusDays(1 + 5);
        second.set(date);

        first.insert(second);
        assertTrue(first.isSet(date));
    }

    @Test
    void shouldInsertAnotherOverlapsAfterSmaller() {
        DaysBitmap first = new DaysBitmap(epochDay,14);
        DaysBitmap second = new DaysBitmap(epochDay+5, 5);

        TramDate date = when.plusDays(1 + 5);
        second.set(date);

        first.insert(second);
        assertTrue(first.isSet(date));
    }

    @Test
    void shouldHaveExpectedOverlaps() {
        DaysBitmap first = new DaysBitmap(16485,269);
        DaysBitmap second = new DaysBitmap(16612, 54);

        first.setDaysOfWeek(TestEnv.allDays());
        second.setDaysOfWeek(TestEnv.allDays());

        assertTrue(first.anyOverlap(second));
    }

    @Test
    void shouldInsertAnotherOverlapsExact() {
        DaysBitmap first = new DaysBitmap(epochDay,14);
        DaysBitmap second = new DaysBitmap(epochDay, 14);

        second.set(when);
        TramDate lastDay = when.plusDays(14);
        second.set(lastDay);

        first.insert(second);
        assertTrue(first.isSet(when));
        assertTrue(first.isSet(lastDay));

    }

    @Test
    void shouldHaveExpectedOverlap() {
        TramDate startDate = TramDate.of(2020, 11, 5);
        TramDate endDate = TramDate.of(2020, 11, 25);

        EnumSet<DayOfWeek> monday = EnumSet.of(MONDAY);

        DaysBitmap serviceA = createDaysBitmap(startDate, endDate, monday);
        DaysBitmap serviceB = createDaysBitmap(startDate, endDate, EnumSet.of(DayOfWeek.SUNDAY));
        DaysBitmap serviceC = createDaysBitmap(startDate.minusDays(10), startDate.minusDays(5), monday);
        DaysBitmap serviceD = createDaysBitmap(startDate, endDate, monday);

        assertTrue(serviceA.anyOverlap(serviceA));

        // wrong operating days
        assertFalse(serviceA.anyOverlap(serviceB));

        // before dates
        assertFalse(serviceA.anyOverlap(serviceC));

        // should match
        assertTrue(serviceA.anyOverlap(serviceD));
    }

    private DaysBitmap createDaysBitmap(TramDate startDate, TramDate endDate, EnumSet<DayOfWeek> operatingDays) {
        long begin = startDate.toEpochDay();
        int size = Math.toIntExact(Math.subtractExact(endDate.toEpochDay(), begin));
        DaysBitmap daysBitmap = new DaysBitmap(begin, size);
        daysBitmap.setDaysOfWeek(operatingDays);
        return daysBitmap;
    }
}
