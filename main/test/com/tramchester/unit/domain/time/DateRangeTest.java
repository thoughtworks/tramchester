package com.tramchester.unit.domain.time;

import com.tramchester.domain.dates.DateRange;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class DateRangeTest {

    @Test
    void shouldCreateCorrectly() {
        LocalDate startDate = LocalDate.of(2014, 10, 5);
        LocalDate endDate = LocalDate.of(2014, 12, 25);

        DateRange range = DateRange.of(startDate, endDate);
        assertEquals(startDate, range.getStartDate());
        assertEquals(endDate, range.getEndDate());
    }

    @Test
    void shouldHaveBroadest() {
        LocalDate startDate = LocalDate.of(2014, 10, 5);
        LocalDate endDate = LocalDate.of(2014, 10, 25);

        DateRange rangeA = DateRange.of(startDate, endDate);
        DateRange rangeB = DateRange.of(startDate.plusDays(1), endDate.minusDays(1));

        assertEquals(rangeA, DateRange.broadest(rangeA, rangeB));

        DateRange rangeC = DateRange.of(startDate.minusDays(1), endDate.plusDays(1));
        assertEquals(rangeC, DateRange.broadest(rangeA, rangeC));

        DateRange rangeD = DateRange.of(startDate.minusDays(10), endDate.minusDays(1));
        assertEquals(DateRange.of(startDate.minusDays(10), endDate), DateRange.broadest(rangeA, rangeD));

        DateRange rangeE = DateRange.of(startDate, endDate.plusDays(1));
        assertEquals(DateRange.of(startDate, endDate.plusDays(1)), DateRange.broadest(rangeA, rangeE));

    }

    @Test
    void shouldCheckContainedCorrectly() {
        LocalDate startDate = LocalDate.of(2014, 10, 5);
        LocalDate endDate = LocalDate.of(2014, 12, 25);

        DateRange range = DateRange.of(startDate, endDate);

        assertTrue(range.contains(startDate));
        assertTrue(range.contains(endDate));

        LocalDate date = startDate;
        while (date.isBefore(endDate)) {
            assertTrue(range.contains(date), "contains " + date);
            date = date.plusDays(1);
        }

        assertFalse(range.contains(startDate.minusDays(1)));
        assertFalse(range.contains(endDate.plusDays(1)));

    }

    @Test
    void shouldHaveOverlapAllDays() {
        LocalDate startDate = LocalDate.of(2014, 10, 5);
        LocalDate endDate = LocalDate.of(2014, 12, 25);

        DateRange dateRange = DateRange.of(startDate, endDate);
        assertTrue(dateRange.overlapsWith(DateRange.of(startDate, endDate)));

        assertTrue(dateRange.overlapsWith(DateRange.of(startDate.minusDays(1), startDate.plusDays(1))));
        assertTrue(dateRange.overlapsWith(DateRange.of(endDate.minusDays(1), endDate.plusDays(1))));
        assertTrue(dateRange.overlapsWith(DateRange.of(startDate.minusDays(1), endDate.plusDays(1))));
        assertTrue(dateRange.overlapsWith(DateRange.of(startDate.minusDays(1), endDate.plusDays(1))));
        assertTrue(dateRange.overlapsWith(DateRange.of(startDate.plusDays(1), endDate.minusDays(1))));

        assertFalse(dateRange.overlapsWith(DateRange.of(endDate.plusDays(2), endDate.plusDays(3))));
        assertFalse(dateRange.overlapsWith(DateRange.of(startDate.minusDays(3), startDate.minusDays(2))));
    }


}
