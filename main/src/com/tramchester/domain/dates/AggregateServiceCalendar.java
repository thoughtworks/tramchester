package com.tramchester.domain.dates;

import com.tramchester.domain.time.DateRange;

import java.io.PrintStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class AggregateServiceCalendar implements ServiceCalendar {

    private final Set<ServiceCalendar> calendars;

    private final EnumSet<DayOfWeek> days;
    private final Set<LocalDate> additional;
    private final Set<LocalDate> removed;
    private final boolean cancelled;
    private final DateRange dateRange;

    public AggregateServiceCalendar(Set<ServiceCalendar> calendars) {
        this.calendars = calendars;

        dateRange = calculateDateRange(calendars);
        cancelled = calendars.stream().allMatch(ServiceCalendar::isCancelled);

        additional = new HashSet<>();
        days = EnumSet.noneOf(DayOfWeek.class);
        HashSet<LocalDate> allExcluded = new HashSet<>();
        calendars.forEach(calendar -> {
            days.addAll(calendar.getOperatingDays());
            additional.addAll(calendar.getAdditions());
            allExcluded.addAll(calendar.getRemoved());
        });

        // only keep an excluded date if it's not available via any of the other contained calendars
        removed = allExcluded.stream().filter(date -> !operatesForAny(calendars, date)).collect(Collectors.toSet());

    }

    private static DateRange calculateDateRange(Set<ServiceCalendar> calendars) {
        Optional<LocalDate> begin = calendars.stream().map(calendar -> calendar.getDateRange().getStartDate()).reduce(AggregateServiceCalendar::earliest);
        Optional<LocalDate> end = calendars.stream().map(calendar -> calendar.getDateRange().getEndDate()).reduce(AggregateServiceCalendar::latest);

        if (begin.isPresent() && end.isPresent()) {
            return DateRange.of(begin.get(), end.get());
        } else {
            throw new RuntimeException("Unable to derive a valid date range from supplier calendars " + calendars);
        }
    }

    private static LocalDate earliest(LocalDate a, LocalDate b) {
        if (a.isBefore(b)) {
            return a;
        } else {
            return b;
        }
    }

    private static LocalDate latest(LocalDate a, LocalDate b) {
        if (a.isAfter(b)) {
            return a;
        } else {
            return b;
        }
    }

    private static boolean operatesForAny(Set<ServiceCalendar> calendars, LocalDate date) {
        return calendars.stream().anyMatch(calendar -> calendar.operatesOn(date));
    }

    @Override
    public boolean operatesOn(LocalDate queryDate) {
        return calendars.stream().anyMatch(calendar -> calendar.operatesOn(queryDate));
    }

    @Override
    public DateRange getDateRange() {
        return dateRange;
    }

    @Override
    public boolean operatesNoDays() {
       return calendars.stream().allMatch(ServiceCalendar::operatesNoDays);
    }

    @Override
    public EnumSet<DayOfWeek> getOperatingDays() {
        return days;
    }

    @Override
    public Set<LocalDate> getAdditions() {
        return additional;
    }

    @Override
    public Set<LocalDate> getRemoved() {
        return removed;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void summariseDates(PrintStream printStream) {
        if (cancelled) {
            printStream.print("CANCELLED: ");
        }
        printStream.printf("%s days %s%n", getDateRange(), reportDays());
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

}
