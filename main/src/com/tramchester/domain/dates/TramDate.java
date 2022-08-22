package com.tramchester.domain.dates;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;

public class TramDate {
    private final long epochDays;
    private final DayOfWeek dayOfWeek;

    private TramDate(long epochDays) {
        this.epochDays = epochDays;
        this.dayOfWeek = calcDayOfWeek(epochDays);
    }

    // replicate LocalDate approach
    public DayOfWeek calcDayOfWeek(long epochDays) {
        int enumAsInt = Math.floorMod(epochDays + 3, 7);
        return DayOfWeek.of(enumAsInt + 1);
    }

    @Deprecated
    public static TramDate of(LocalDate date) {
        return new TramDate(date.toEpochDay());
    }

    public static TramDate of(int year, int month, int day) {
        LocalDate date = LocalDate.of(year, month, day);
        return new TramDate(date.toEpochDay());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TramDate tramDate = (TramDate) o;
        return epochDays == tramDate.epochDays;
    }

    @Override
    public int hashCode() {
        return Objects.hash(epochDays);
    }

    public boolean isAfter(TramDate other) {
        return this.epochDays>other.epochDays;
    }

    public boolean isBefore(TramDate other) {
        return this.epochDays<other.epochDays;
    }

    public TramDate plusDays(int days) {
        long newDay = days + epochDays;
        return new TramDate(newDay);
    }

    public LocalDate toLocalDate() {
        return LocalDate.ofEpochDay(epochDays);
    }

    public TramDate minusDays(int days) {
        long newDay = epochDays - days;
        return new TramDate(newDay);
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public long toEpochDay() {
        return epochDays;
    }
}
