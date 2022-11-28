package com.tramchester.domain.dates;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class DateRange {
    private final TramDate startDate;
    private final TramDate endDate;

    public DateRange(TramDate startDate, TramDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public static DateRange of(TramDate startDate, TramDate endDate) {
        return new DateRange(startDate, endDate);
    }

    public boolean contains(final TramDate queryDate) {
        if (queryDate.isAfter(endDate) || queryDate.isBefore(startDate)) {
            return false;
        }
        return true;
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

    public boolean overlapsWith(DateRange other) {
        return between(other, startDate) ||
                between(other, endDate) ||
                between(this, other.startDate) ||
                between(this, other.endDate);
    }

    private static boolean between(DateRange dateRange, TramDate date) {
        if (date.equals(dateRange.startDate) || date.equals(dateRange.endDate)) {
            return true;
        }
        return (date.isAfter(dateRange.startDate)  && date.isBefore(dateRange.endDate));
    }

    public TramDate getEndDate() {
        return endDate;
    }

    public TramDate getStartDate() {
        return startDate;
    }

    /***
     * In order stream from start to end date, inclusive
     * @return stream of dates
     */
    public Stream<TramDate> stream() {
        long start = startDate.toEpochDay();
        long end = endDate.toEpochDay();
        int range = 1 + Math.toIntExact(Math.subtractExact(end, start));

        return IntStream.range(0, range).boxed().
                map(startDate::plusDays).sorted();

    }

    public long numberOfDays() {
        long diff = Math.subtractExact(endDate.toEpochDay(), startDate.toEpochDay());
        // inclusive, so add one
        return Math.abs(diff+1);
    }
}
