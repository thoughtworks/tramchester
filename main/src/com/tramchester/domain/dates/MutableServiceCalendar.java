package com.tramchester.domain.dates;

import com.tramchester.dataimport.data.CalendarData;

import java.io.PrintStream;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;

import static java.lang.String.format;

public class MutableServiceCalendar implements ServiceCalendar {
    private final DateRange dateRange;
    private final EnumSet<DayOfWeek> days;
    private final TramDateSet additional;
    private final TramDateSet removed;
    private boolean cancelled;
    private long numberOfDays;

    public MutableServiceCalendar(CalendarData calendarData) {
        this(calendarData.getDateRange(),
                daysOfWeekFrom(calendarData.isMonday(),
                calendarData.isTuesday(),
                calendarData.isWednesday(),
                calendarData.isThursday(),
                calendarData.isFriday(),
                calendarData.isSaturday(),
                calendarData.isSunday()));
    }

    public MutableServiceCalendar(TramDate startDate, TramDate endDate, DayOfWeek... operatingDays) {
        this(new DateRange(startDate, endDate), enumFrom(operatingDays));
    }

    public MutableServiceCalendar(DateRange dateRange, EnumSet<DayOfWeek> operatingDays) {
        this.dateRange = dateRange;
        days = operatingDays;
        additional = new TramDateSet();
        removed = new TramDateSet();
        cancelled = false;
        calculateNumberOfDays();
    }

    private void calculateNumberOfDays() {
        numberOfDays = dateRange.stream().
                filter(date -> !removed.contains(date)).
                filter(date -> days.contains(date.getDayOfWeek()) || additional.contains(date)).
                count();
    }

    private static EnumSet<DayOfWeek> enumFrom(DayOfWeek[] operatingDays) {
        return EnumSet.copyOf(Arrays.asList(operatingDays));
    }

    @Override
    public long numberDaysOperating() {
        if (cancelled) {
            return 0;
        }
        return numberOfDays;
    }

    public void includeExtraDate(TramDate date) {
        if (!dateRange.contains(date)) {
            throw new RuntimeException(format("Additional date %s is outside of the range for %s", date, dateRange));
        }
        additional.add(date);
        calculateNumberOfDays();
    }

    public void excludeDate(TramDate date) {
        if (!dateRange.contains(date)) {
            throw new RuntimeException(format("Excluded date %s is outside of the range for %s", date, dateRange));
        }
        removed.add(date);
        calculateNumberOfDays();
    }

    @Override
    public boolean operatesOn(final TramDate queryDate) {
        if (cancelled || isExcluded(queryDate)) {
            return false;
        }

        if (additional.contains(queryDate)) {
            return true;
        }

        return operatesOnIgnoringExceptionDates(queryDate);
    }

    @Override
    public boolean operatesOnAny(final TramDateSet queryDates) {
        if (operatesNoDays() || queryDates.isEmpty()) {
            return false;
        }

        if (removed.containsAll(queryDates)) {
            return false;
        }

        if (additional.containsAny(queryDates)) {
            return true;
        }

        return queryDates.stream().
                filter(date -> days.contains(date.getDayOfWeek())).
                anyMatch(dateRange::contains);
    }

    @Override
    public boolean operatesNoneOf(final TramDateSet dates) {
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

    private boolean dayAndRange(final TramDate date) {
        return dateRange.contains(date) && days.contains(date.getDayOfWeek());
    }

    private boolean isExcluded(final TramDate queryDate) {
        return removed.contains(queryDate);
    }

    private boolean operatesOnIgnoringExceptionDates(final TramDate queryDate) {
        if (days.contains(queryDate.getDayOfWeek())) {
            return dateRange.contains(queryDate);
        }
        return false;
    }

    @Override
    public void summariseDates(PrintStream printStream) {
        if (cancelled) {
            printStream.print("CANCELLED: ");
        }
        printStream.printf("%s days %s%n", dateRange, reportDays());
        if (!additional.isEmpty()) {
            printStream.println("Additional on: " + additional);
        }
        if (!removed.isEmpty()) {
            printStream.println("Not running on: " + removed);
        }
    }

    @Override
    public DateRange getDateRange() {
        return dateRange;
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
        if (result.isEmpty()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }
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
    public EnumSet<DayOfWeek> getOperatingDays() {
        return days;
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
    public boolean anyDateOverlaps(ServiceCalendar otherCalendar) {
        // no overlaps if either doesn't operate at all
        if (otherCalendar.operatesNoDays() || operatesNoDays()) {
            return false;
        }

        // working assumption, any additional dates are within the overall specified range for a service
        if (!otherCalendar.getDateRange().overlapsWith(getDateRange())) {
            return false;
        }

        // additions
        if (otherCalendar.operatesOnAny(getAdditions())) {
            return true;
        }
        if (this.operatesOnAny(otherCalendar.getAdditions())) {
            return true;
        }

        // removed
        // logic here: if the other calendar also operates none of the removed dates
        TramDateSet removed = this.getRemoved();
        TramDateSet otherRemoved = otherCalendar.getRemoved();

        if ((!removed.isEmpty() && !otherRemoved.isEmpty()) && !removed.equals(otherRemoved)) {

            if (otherCalendar.operatesNoneOf(removed)) {
                return false;
            }
            if (this.operatesNoneOf(otherRemoved)) {
                return false;
            }
        }

        // operating days, any overlap?
        final EnumSet<DayOfWeek> otherDays = EnumSet.copyOf(otherCalendar.getOperatingDays());
        return otherDays.removeAll(getOperatingDays());

    }

    @Override
    public String toString() {
        return "MutableServiceCalendar{" +
                "dateRange=" + dateRange +
                ", days=" + days +
                ", additional=" + additional +
                ", removed=" + removed +
                ", cancelled=" + cancelled +
                '}';
    }

    public void cancel() {
        cancelled = true;
    }
}
