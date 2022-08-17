package com.tramchester.unit.domain.dates;

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
