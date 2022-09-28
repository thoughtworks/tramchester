package com.tramchester.domain.dates;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Stream;

public class TramDate implements Comparable<TramDate> {
    private final long epochDays;
    private final DayOfWeek dayOfWeek;

    private TramDate(long epochDays) {
        this.epochDays = epochDays;
        this.dayOfWeek = calcDayOfWeek(epochDays);
    }

    public static TramDate of(long epochDay) {
        return new TramDate(epochDay);
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

    // replicate LocalDate approach
    public DayOfWeek calcDayOfWeek(long epochDays) {
        int enumAsInt = Math.floorMod(epochDays + 3, 7);
        return DayOfWeek.of(enumAsInt + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TramDate tramDate = (TramDate) o;
        return epochDays == tramDate.epochDays;
    }

    public boolean isEquals(TramDate other) {
        return other.epochDays == epochDays;
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

    /***
     * format YYYYMMDD
     * @param text date text
     * @param offset offset to start of text
     * @return TramDate
     */
    public static TramDate parseSimple(String text, int offset) {
        int year = parseFullYear(text, offset);
        int month = parseTens(text, offset+4);
        int day = parseTens(text, offset+6);
        return TramDate.of(year, month, day);
    }

    /***
     *
     * @param text text to parse in form YYMMDD
     * @param century century to add to the year
     * @param offset offset to start of text to parse
     * @return the TramDate
     */
    public static TramDate parseSimple(String text, int century, int offset) {
        int year = parseTens(text, offset);
        int month = parseTens(text, offset+2);
        int day = parseTens(text, offset+4);
        return TramDate.of((century*100) + year, month, day);
    }

    private static int parseTens(String text, int offset) {
        char digit1 = text.charAt(offset);
        char digit2 = text.charAt(offset+1);

        int tens = Character.digit(digit1, 10);
        int unit = Character.digit(digit2, 10);

        return (tens*10) + unit;
    }

    private static int parseFullYear(String text, int offset) {
        char digit1 = text.charAt(offset);
        char digit2 = text.charAt(offset+1);
        char digit3 = text.charAt(offset+2);
        char digit4 = text.charAt(offset+3);

        int millenium = Character.digit(digit1, 10);
        int century = Character.digit(digit2, 10);
        int decade = Character.digit(digit3, 10);
        int year = Character.digit(digit4, 10);

        return (millenium*1000) + (century*100) + (decade*10) + year;
    }

    // supports deserialization
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
