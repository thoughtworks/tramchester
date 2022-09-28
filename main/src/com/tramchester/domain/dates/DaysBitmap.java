package com.tramchester.domain.dates;

import java.time.DayOfWeek;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.Objects;

import static java.lang.String.format;

public class DaysBitmap {
    private final long beginningDay;
    private final BitSet days;
    private final int size;

    public DaysBitmap(long beginningEpochDay, int size) {
        this.beginningDay = beginningEpochDay;
        this.days = new BitSet(size);
        this.size = size;
    }

    public void setDaysOfWeek(EnumSet<DayOfWeek> operatingDays) {
        for (int i = 0; i < size; i++) {
            TramDate date = TramDate.of(beginningDay + i);
            if (operatingDays.contains(date.getDayOfWeek())) {
                days.set(i);
            }
        }
    }

    public void clearAll() {
        days.clear();
    }

    public long numberSet() {
        return days.cardinality();
    }

    private int offsetFor(TramDate date) {
        long day = date.toEpochDay();
        if ((day< beginningDay) || (day>(beginningDay+size))) {
            throw new RuntimeException(format("Date %s (day %s) is out of range for %s", date, day, beginningDay));
        }
        long diff = Math.subtractExact(day, beginningDay);
        return Math.toIntExact(diff);
    }

    public void set(TramDate date) {
        int offset = offsetFor(date);
        if (offset>=size) {
            throw new RuntimeException(format("Attempt to set date out of range %s for %s", date, this));
        }
        days.set(offset);
    }

    public void clear(TramDate date) {
        int offset = offsetFor(date);
        days.clear(offset);
    }

    public boolean isSet(TramDate date) {
        int offset = offsetFor(date);
        return days.get(offset);
    }

    public boolean noneSet() {
        return days.cardinality()==0;
    }

    public boolean anyOverlap(DaysBitmap other) {

        if ( ! (this.contains(other) || other.contains(this)) ) {
            return false;
        }

        BitSet firstOverlap = other.getOverlapWith(this);
        BitSet secondOverlap = this.getOverlapWith(other);

        return firstOverlap.intersects(secondOverlap);
    }

    private boolean contains(DaysBitmap other) {
        if (dayWithin(other.beginningDay)) {
            return true;
        }

        long otherEndDay = other.beginningDay + other.size;
        return dayWithin(otherEndDay);
    }

    private boolean dayWithin(long epochDay) {
        long endDay = beginningDay + size;
        return epochDay>=beginningDay && epochDay<=endDay;
    }

    private BitSet getOverlapWith(DaysBitmap other) {
        long startOfOverlap = other.beginningDay < this.beginningDay ? 0 : other.beginningDay-this.beginningDay;

        long endOfThis = this.beginningDay + size;
        long endOfOther = other.beginningDay + other.size;

        long size = endOfOther > endOfThis ? this.size : this.size - Math.subtractExact(endOfThis, endOfOther);

        int start = Math.toIntExact(startOfOverlap);
        int end = Math.toIntExact(startOfOverlap+size);

        return days.get(start, end);
    }

    public long getBeginningEpochDay() {
        return beginningDay;
    }

    public void insert(DaysBitmap other) {
        int offset = Math.toIntExact(Math.subtractExact(other.getBeginningEpochDay(), beginningDay));

        // todo not safe if runs past end, bit will silently extent the length
        other.days.stream().map(setBit -> setBit+offset).forEach(days::set);
    }

    @Override
    public String toString() {
        return "DaysBitmap{" +
                "beginningDay=" + beginningDay +
                ", size=" + size +
                ", days=" + days +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DaysBitmap that = (DaysBitmap) o;
        return beginningDay == that.beginningDay && size == that.size && days.equals(that.days);
    }

    @Override
    public int hashCode() {
        return Objects.hash(beginningDay, days, size);
    }
}
