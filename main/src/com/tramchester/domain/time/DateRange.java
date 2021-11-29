package com.tramchester.domain.time;

import java.time.LocalDate;
import java.util.Objects;

public class DateRange {
    private final LocalDate startDate;
    private final LocalDate endDate;

    public DateRange(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public static DateRange of(LocalDate startDate, LocalDate endDate) {
        return new DateRange(startDate, endDate);
    }

    public static DateRange broadest(DateRange rangeA, DateRange rangeB) {
        LocalDate earliest = rangeA.startDate.isBefore(rangeB.startDate) ? rangeA.startDate : rangeB.startDate;
        LocalDate latest = rangeA.endDate.isAfter(rangeB.endDate) ? rangeA.endDate : rangeB.endDate;
        return new DateRange(earliest, latest);
    }

    public boolean contains(LocalDate queryDate) {
        if (queryDate.equals(startDate) || queryDate.equals(endDate)) {
            return true;
        }
        return (queryDate.isAfter(startDate) && queryDate.isBefore(endDate));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DateRange dateRange = (DateRange) o;
        return startDate.equals(dateRange.startDate) && endDate.equals(dateRange.endDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startDate, endDate);
    }

    @Override
    public String toString() {
        return "DateRange{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }

    public boolean overlapsWith(DateRange dateRange) {
        return between(dateRange, startDate) ||
                between(dateRange, endDate) ||
                between(this, dateRange.startDate) ||
                between(this, dateRange.endDate);
    }

    private static boolean between(DateRange dateRange, LocalDate date) {
        if (date.equals(dateRange.startDate) || date.equals(dateRange.endDate)) {
            return true;
        }
        return (date.isAfter(dateRange.startDate)  && date.isBefore(dateRange.endDate));
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }
}
