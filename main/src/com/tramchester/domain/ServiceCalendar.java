package com.tramchester.domain;

import com.tramchester.dataimport.data.CalendarData;

import java.io.PrintStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ServiceCalendar {
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Set<DayOfWeek> days;
    private final Set<LocalDate> additional;
    private final Set<LocalDate> removed;

    public ServiceCalendar(CalendarData calendarData) {
        this(calendarData.getStartDate(), calendarData.getEndDate(), daysOfWeekFrom(calendarData.isMonday(),
                calendarData.isTuesday(),
                calendarData.isWednesday(),
                calendarData.isThursday(),
                calendarData.isFriday(),
                calendarData.isSaturday(),
                calendarData.isSunday()));
    }

    public ServiceCalendar(LocalDate startDate, LocalDate endDate, DayOfWeek... operatingDays) {
        this(startDate, endDate, new HashSet<>(Arrays.asList(operatingDays)));
    }

    public ServiceCalendar(LocalDate startDate, LocalDate endDate, Set<DayOfWeek> operatingDays) {
        this.startDate = startDate;
        this.endDate = endDate;
        days = operatingDays;
        additional = new HashSet<>();
        removed = new HashSet<>();
    }

    public void includeExtraDate(LocalDate date) {
        additional.add(date);
    }

    public void excludeDate(LocalDate date) {
        removed.add(date);
    }

    public boolean operatesOn(LocalDate queryDate) {
        if (isExcluded(queryDate)) {
            return false;
        }

        if (additional.contains(queryDate)) {
            return true;
        }

        return operatesOnIgnoringExcpetionDates(queryDate);
    }

    public boolean isExcluded(LocalDate queryDate) {
        return removed.contains(queryDate);
    }

    public boolean operatesOnIgnoringExcpetionDates(LocalDate queryDate) {
        if  (queryDate.isAfter(startDate) && queryDate.isBefore(endDate)) {
            return days.contains(queryDate.getDayOfWeek());
        }
        if (queryDate.equals(startDate) || queryDate.equals(endDate)) {
            return days.contains(queryDate.getDayOfWeek());
        }
        return false;
    }

    public void summariseDates(PrintStream printStream) {
        printStream.printf("starts %s ends %s days %s%n",
                startDate, endDate, reportDays());
        if (!additional.isEmpty()) {
            printStream.println("Additional on: " + additional.toString());
        }
        if (!removed.isEmpty()) {
            printStream.println("Not running on: " + removed.toString());
        }
    }

    private String reportDays() {
        if (days.isEmpty()) {
            return "SPECIAL/NONE";
        }

        StringBuilder found = new StringBuilder();
        days.forEach(dayOfWeek -> {
            if (found.length()>0) {
                found.append(",");
            }
            found.append(dayOfWeek.name());
        });
        return found.toString();
    }

    private static Set<DayOfWeek> daysOfWeekFrom(boolean monday, boolean tuesday,
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
        return result;
    }

    private static void addIf(boolean flag, DayOfWeek dayOfWeek, HashSet<DayOfWeek> accumulator) {
        if (flag) {
            accumulator.add(dayOfWeek);
        }
    }

    public boolean operatesNoDays() {
        return days.isEmpty();
    }

    @Override
    public String toString() {
        return "ServiceCalendar{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                ", days=" + days +
                ", additional=" + additional +
                ", removed=" + removed +
                '}';
    }
}
