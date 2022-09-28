package com.tramchester.domain.dates;

import java.time.DayOfWeek;
import java.util.BitSet;
import java.util.EnumSet;

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
        for (int i = 0; i <= size; i++) {
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
        long endDay = beginningDay + size;
        long otherEndDay = other.beginningDay + other.size;

        if ((other.beginningDay <beginningDay || other.beginningDay >endDay) &&
                (otherEndDay<beginningDay || otherEndDay>endDay)) {
            return false;
        }

        BitSet firstOverlap = getoverlapBitset(other.beginningDay, other.size);
        BitSet secondOverlap = other.getoverlapBitset(this.beginningDay, this.size);

        return firstOverlap.intersects(secondOverlap);
    }

    private BitSet getoverlapBitset(long otherStartDay, int otherSize) {
        long beginIndex = 0;

        if (otherStartDay>beginningDay) {
            beginIndex = Math.subtractExact(otherStartDay, beginningDay);
        }

        long otherEndDay = otherStartDay + otherSize;
        long thisEndDay = this.beginningDay + this.size;

        long endIndex = this.size-1;
        if (otherEndDay < thisEndDay) {
            long diff = Math.subtractExact(thisEndDay, otherEndDay);
            endIndex = this.size - diff;
        }

        int fromIndex = Math.toIntExact(beginIndex);
        int toIndex = Math.toIntExact(endIndex);
        if (fromIndex>toIndex) {
            throw new RuntimeException(format("fromIndex %s > toIndex: %s", fromIndex, toIndex));
        }
        return days.get(fromIndex, toIndex);
    }

    public long getBeginningEpochDay() {
        return beginningDay;
    }

    public void insert(DaysBitmap other) {
        int offset = Math.toIntExact(Math.subtractExact(other.getBeginningEpochDay(), beginningDay));
        other.days.stream().map(setBit -> setBit+offset).forEach(days::set);
    }
}
