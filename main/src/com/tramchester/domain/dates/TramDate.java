package com.tramchester.domain.dates;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Stream;

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

    public static TramDate from(LocalDateTime localDateTime) {
        return of(localDateTime.toLocalDate());
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

    public String format(DateTimeFormatter dateFormatter) {
        return LocalDate.ofEpochDay(epochDays).format(dateFormatter);
    }

    public static TramDate parse(String text, DateTimeFormatter formatter) {
        LocalDate date = LocalDate.parse(text, formatter);
        return new TramDate(date.toEpochDay());
    }

    public static TramDate parse(String text) {
        LocalDate date = LocalDate.parse(text);
        return new TramDate(date.toEpochDay());
    }

    @Override
    public String toString() {
        LocalDate date = LocalDate.ofEpochDay(epochDays);
        return "TramDate{" +
                "epochDays=" + epochDays +
                ", dayOfWeek=" + dayOfWeek +
                ", date=" + date +
                '}';
    }

    public int compareTo(TramDate other) {
        return Long.compare(this.epochDays, other.epochDays);
    }

    public TramDate minusWeeks(int weeks) {
        return of(toLocalDate().minusWeeks(weeks));
    }

    public TramDate plusWeeks(int weeks) {
        return of (toLocalDate().plusWeeks(weeks));
    }

    public Stream<TramDate> datesUntil(TramDate endDate) {
        return toLocalDate().datesUntil(endDate.toLocalDate()).map(date -> new TramDate(date.toEpochDay()));
    }

    public Month getMonth() {
        return toLocalDate().getMonth();
    }

    public int getDayOfMonth() {
        return toLocalDate().getDayOfMonth();
    }

    public boolean isEqual(TramDate other) {
        return this.epochDays == other.epochDays;
    }

    public int getYear() {
        return toLocalDate().getYear();
    }
}
