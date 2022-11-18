package com.tramchester.domain.dates;

import java.io.PrintStream;
import java.time.DayOfWeek;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;

public class AggregateServiceCalendar implements ServiceCalendar, HasDaysBitmap {

    private final EnumSet<DayOfWeek> aggregatedDays;
    private final TramDateSet additional;
    private final TramDateSet removed;
    private final boolean cancelled;
    private final DateRange aggregatedRange;

    private final DaysBitmap days;

    public AggregateServiceCalendar(Collection<ServiceCalendar> calendars) {

        aggregatedRange = calculateDateRange(calendars);
        cancelled = calendars.stream().allMatch(ServiceCalendar::isCancelled);

        additional = new TramDateSet();
        aggregatedDays = EnumSet.noneOf(DayOfWeek.class);

        days = createDaysBitset(aggregatedRange);

        TramDateSet allExcluded = new TramDateSet();
        calendars.forEach(calendar -> {
            setDaysFor(calendar);
            aggregatedDays.addAll(calendar.getOperatingDays());
            additional.addAll(calendar.getAdditions());
            allExcluded.addAll(calendar.getRemoved());
        });

        // only keep an excluded date if it's not available via any of the other contained calendars
        removed = allExcluded.stream().filter(date -> !days.isSet(date)).collect(TramDateSet.collector());
    }

    private DaysBitmap createDaysBitset(DateRange dateRange) {
        long earliest = dateRange.getStartDate().toEpochDay();
        long latest = dateRange.getEndDate().toEpochDay();

        int size = Math.toIntExact(Math.subtractExact(latest, earliest));

        return new DaysBitmap(earliest, size);
    }

    private void setDaysFor(ServiceCalendar calendar) {
        HasDaysBitmap other = (HasDaysBitmap) calendar;
        days.insert(other.getDays());
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

    @Override
    public boolean anyDateOverlaps(ServiceCalendar other) {
        HasDaysBitmap otherDays = (HasDaysBitmap) other;
        return this.days.anyOverlap(otherDays.getDays());
    }

    @Override
    public long numberDaysOperating() {
        return days.numberSet();
    }

    @Override
    public boolean operatesOn(final TramDate date) {
        if (aggregatedRange.contains(date)) {
            return days.isSet(date);
        }
        return false;
    }

    @Override
    public DateRange getDateRange() {
        return aggregatedRange;
    }

    @Override
    public boolean operatesNoDays() {
       return days.numberSet()==0;
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

    @Override
    public DaysBitmap getDays() {
        return days;
    }
}
