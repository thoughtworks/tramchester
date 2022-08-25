package com.tramchester.domain.dates;

import java.io.PrintStream;
import java.time.DayOfWeek;
import java.util.EnumSet;

public interface ServiceCalendar {

    boolean operatesOn(TramDate queryDate);

    void summariseDates(PrintStream printStream);

    /***
     * Range of dates (from data source) given for the this service. NOTE: service might not actually operate on
     * any of these dates depending on removed, additional and operatingdays
     * @return
     */
    DateRange getDateRange();

    /***
     * True iff does not operate on any days whatsoever, takes account of additional days
     * @return true if service NEVER operates
     */
    boolean operatesNoDays();

    /***
     * Returns days this service operates, use with care since does not include exclusions/additional dates,
     * use operatesOn() for that
     * @return Set of days service operates ignoring additional and removed days
     */
    EnumSet<DayOfWeek> getOperatingDays();

    TramDateSet getAdditions();

    TramDateSet getRemoved();

    boolean isCancelled();

    boolean anyDateOverlaps(ServiceCalendar other);

    boolean operatesOnAny(TramDateSet dates);

    boolean operatesNoneOf(TramDateSet dates);

    long numberDaysOperating();
}
