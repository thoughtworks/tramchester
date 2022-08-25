package com.tramchester.domain.dates;

import java.io.PrintStream;
import java.time.DayOfWeek;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class AggregateServiceCalendar implements ServiceCalendar {

    private final Collection<ServiceCalendar> calendars;

    private final EnumSet<DayOfWeek> aggregatedDays;
    private final TramDateSet additional;
    private final TramDateSet removed;
    private final boolean cancelled;
    private final DateRange aggregatedRange;
    private final boolean operatesNoDays;
    private final long numberOfDaysOperating;

    public AggregateServiceCalendar(Collection<ServiceCalendar> calendars) {
        this.calendars = calendars;

        aggregatedRange = calculateDateRange(calendars);
        cancelled = calendars.stream().allMatch(ServiceCalendar::isCancelled);

        additional = new TramDateSet();
        aggregatedDays = EnumSet.noneOf(DayOfWeek.class);
        TramDateSet allExcluded = new TramDateSet();
        final AtomicInteger noDaysCount = new AtomicInteger(0);
        calendars.forEach(calendar -> {
            aggregatedDays.addAll(calendar.getOperatingDays());
            additional.addAll(calendar.getAdditions());
            allExcluded.addAll(calendar.getRemoved());
            if (calendar.operatesNoDays()) {
                noDaysCount.incrementAndGet();
            }
        });

        // only keep an excluded date if it's not available via any of the other contained calendars
        removed = allExcluded.stream().filter(date -> !operatesForAny(calendars, date)).collect(TramDateSet.collector());

        operatesNoDays = noDaysCount.get() == calendars.size();

        numberOfDaysOperating = calcNumberOfDaysOperating();
    }

    private long calcNumberOfDaysOperating() {
        if (cancelled || operatesNoDays) {
            return 0;
        }
        return aggregatedRange.stream().filter(this::operatesOn).count();
    }

    private static DateRange calculateDateRange(Collection<ServiceCalendar> calendars) {
        final Optional<TramDate> begin = calendars.stream().map(calendar -> calendar.getDateRange().getStartDate()).
                reduce(AggregateServiceCalendar::earliest);

        final Optional<TramDate> end = calendars.stream().map(calendar -> calendar.getDateRange().getEndDate()).
                reduce(AggregateServiceCalendar::latest);

        if (begin.isPresent() && end.isPresent()) {
            return DateRange.of(begin.get(), end.get());
        } else {
            throw new RuntimeException("Unable to derive a valid date range from supplier calendars " + calendars);
        }
    }

    private static TramDate earliest(final TramDate a, final TramDate b) {
        if (a.isBefore(b)) {
            return a;
        } else {
            return b;
        }
    }

    private static TramDate latest(final TramDate a, final TramDate b) {
        if (a.isAfter(b)) {
            return a;
        } else {
            return b;
        }
    }

    private static boolean operatesForAny(final Collection<ServiceCalendar> calendars, final TramDate date) {
        return calendars.stream().anyMatch(calendar -> calendar.operatesOn(date));
    }

    @Override
    public boolean anyDateOverlaps(ServiceCalendar other) {
        return calendars.stream().anyMatch(calendar -> calendar.anyDateOverlaps(other));
    }

    @Override
    public boolean operatesOnAny(TramDateSet dates) {
        if (dates.isEmpty()) {
            return false;
        }
        if (additional.containsAny(dates)) {
            return true;
        }
        if (removed.containsAll(dates)) {
            return false;
        }
        for(TramDate date : dates) {
            if (dayAndRange(date)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean operatesNoneOf(TramDateSet dates) {
        if (dates.isEmpty()) {
            return false;
        }
        if (additional.containsAny(dates)) {
            return false;
        }
        if (removed.containsAll(dates)) {
            return true;
        }
        for(TramDate date : dates) {
            if (dayAndRange(date) && !removed.contains(date)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public long numberDaysOperating() {
        return numberOfDaysOperating;
    }

    private boolean dayAndRange(final TramDate date) {
        if (!aggregatedRange.contains(date)) {
            return false;
        }

        final DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (!aggregatedDays.contains(dayOfWeek)) {
            return false;
        }

        // see if specific calendar match on days && range for
        for(ServiceCalendar calendar : calendars) {
            if (calendar.getOperatingDays().contains(dayOfWeek) && calendar.getDateRange().contains(date)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean operatesOn(final TramDate date) {
        if (cancelled || operatesNoDays) {
            return false;
        }

        if (removed.contains(date)) {
            return false;
        }
        if (additional.contains(date)) {
            return true;
        }

        if (!aggregatedRange.contains(date)) {
            return false;
        }
        if (!aggregatedDays.contains(date.getDayOfWeek())) {
            return false;
        }

        // contained calendars will have own sets of days operating (days of week) which will not be valid for
        // whole duration here, so need to check each contained calendar for final answer

        return calendars.stream().anyMatch(calendar -> calendar.operatesOn(date));
    }

    @Override
    public DateRange getDateRange() {
        return aggregatedRange;
    }

    @Override
    public boolean operatesNoDays() {
       return operatesNoDays;
    }

    @Override
    public EnumSet<DayOfWeek> getOperatingDays() {
        return aggregatedDays;
    }

    @Override
    public TramDateSet getAdditions() {
        return additional;
    }

    @Override
    public TramDateSet getRemoved() {
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
        if (aggregatedDays.isEmpty()) {
            return "SPECIAL/NONE";
        }

        if (cancelled) {
            return "CANCELLED";
        }

        StringBuilder found = new StringBuilder();
        aggregatedDays.forEach(dayOfWeek -> {
            if (found.length() > 0) {
                found.append(",");
            }
            found.append(dayOfWeek.name());
        });
        return found.toString();
    }

}
