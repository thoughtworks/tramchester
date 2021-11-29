package com.tramchester.domain;

import com.google.common.collect.Sets;
import com.tramchester.dataimport.data.CalendarData;

import java.io.PrintStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class MutableServiceCalendar implements ServiceCalendar {
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final EnumSet<DayOfWeek> days;
    private final Set<LocalDate> additional;
    private final Set<LocalDate> removed;
    private boolean cancelled;

    public MutableServiceCalendar(CalendarData calendarData) {
        this(calendarData.getStartDate(), calendarData.getEndDate(), daysOfWeekFrom(calendarData.isMonday(),
                calendarData.isTuesday(),
                calendarData.isWednesday(),
                calendarData.isThursday(),
                calendarData.isFriday(),
                calendarData.isSaturday(),
                calendarData.isSunday()));
    }

    public MutableServiceCalendar(LocalDate startDate, LocalDate endDate, DayOfWeek... operatingDays) {
        this(startDate, endDate, enumFrom(operatingDays));
    }

    public MutableServiceCalendar(LocalDate startDate, LocalDate endDate, EnumSet<DayOfWeek> operatingDays) {
        this.startDate = startDate;
        this.endDate = endDate;
        days = operatingDays;
        additional = new HashSet<>();
        removed = new HashSet<>();
        cancelled = false;
    }

    private static EnumSet<DayOfWeek> enumFrom(DayOfWeek[] operatingDays) {
        return EnumSet.copyOf(Arrays.asList(operatingDays));
    }

    public void includeExtraDate(LocalDate date) {
        additional.add(date);
    }

    public void excludeDate(LocalDate date) {
        removed.add(date);
    }

    @Override
    public boolean operatesOn(LocalDate queryDate) {
        if (cancelled || isExcluded(queryDate)) {
            return false;
        }

        if (additional.contains(queryDate)) {
            return true;
        }

        return operatesOnIgnoringExcpetionDates(queryDate);
    }

    private boolean isExcluded(LocalDate queryDate) {
        return removed.contains(queryDate);
    }

    private boolean operatesOnIgnoringExcpetionDates(LocalDate queryDate) {
        if  (queryDate.isAfter(startDate) && queryDate.isBefore(endDate)) {
            return days.contains(queryDate.getDayOfWeek());
        }
        if (queryDate.equals(startDate) || queryDate.equals(endDate)) {
            return days.contains(queryDate.getDayOfWeek());
        }
        return false;
    }

    @Override
    public void summariseDates(PrintStream printStream) {
        if (cancelled) {
            printStream.print("CANCELLED: ");
        }
        printStream.printf("starts %s ends %s days %s%n",
                startDate, endDate, reportDays());
        if (!additional.isEmpty()) {
            printStream.println("Additional on: " + additional);
        }
        if (!removed.isEmpty()) {
            printStream.println("Not running on: " + removed);
        }
    }

    private String reportDays() {
        if (days.isEmpty()) {
            return "SPECIAL/NONE";
        }
        if (cancelled) {
            return "CANCELLED";
        }

        StringBuilder found = new StringBuilder();
        days.forEach(dayOfWeek -> {
            if (found.length() > 0) {
                found.append(",");
            }
            found.append(dayOfWeek.name());
        });
        return found.toString();
    }

    private static EnumSet<DayOfWeek> daysOfWeekFrom(boolean monday, boolean tuesday,
                                                 boolean wednesday, boolean thursday, boolean friday, boolean saturday, boolean sunday)
    {
        HashSet<DayOfWeek> result = new HashSet<>();
        addIf(monday, DayOfWeek.MONDAY, result);
        addIf(tuesday, DayOfWeek.TUESDAY, result);
        addIf(wednesday, DayOfWeek.WEDNESDAY, result);
        addIf(thursday, DayOfWeek.THURSDAY, result);
        addIf(friday, DayOfWeek.FRIDAY, result);
        addIf(saturday, DayOfWeek.SATURDAY, result);
        addIf(sunday, DayOfWeek.SUNDAY, result);
        return EnumSet.copyOf(result);
    }

    private static void addIf(boolean flag, DayOfWeek dayOfWeek, HashSet<DayOfWeek> accumulator) {
        if (flag) {
            accumulator.add(dayOfWeek);
        }
    }

    @Override
    public boolean operatesNoDays() {
        return cancelled || (days.isEmpty() && additional.isEmpty());
    }

    @Override
    public boolean overlapsDatesWith(LocalDate rangeBegin, LocalDate rangeEnd) {
        return between(rangeBegin, rangeEnd, startDate) ||
                between(rangeBegin, rangeEnd, endDate) ||
                between(startDate, endDate, rangeBegin) ||
                between(startDate, endDate, rangeEnd);
    }

    @Override
    public boolean overlapsDatesAndDaysWith(LocalDate startDate, LocalDate endDate, EnumSet<DayOfWeek> daysOfWeek) {
        Sets.SetView<DayOfWeek> intersection = Sets.intersection(days, daysOfWeek);
        if (intersection.isEmpty()) {
            return false;
        }
        return overlapsDatesWith(startDate, endDate);
    }

    private boolean between(LocalDate rangeBegin, LocalDate rangeEnd, LocalDate date) {
        if (date.equals(rangeBegin) || date.equals(rangeEnd)) {
            return true;
        }
        return (date.isAfter(rangeBegin)  && date.isBefore(rangeEnd));
    }

    @Override
    public String toString() {
        return "MutableServiceCalendar{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                ", days=" + days +
                ", additional=" + additional +
                ", removed=" + removed +
                ", cancelled=" + cancelled +
                '}';
    }

    @Override
    public LocalDate getEndDate() {
        return endDate;
    }

    public void cancel() {
        cancelled = true;
    }
}
