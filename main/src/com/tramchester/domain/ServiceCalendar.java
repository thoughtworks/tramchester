package com.tramchester.domain;

import com.tramchester.domain.time.DateRange;

import java.io.PrintStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;

public interface ServiceCalendar {
    boolean operatesOn(LocalDate queryDate);

    void summariseDates(PrintStream printStream);

    LocalDate getEndDate();

    boolean operatesNoDays();

    boolean overlapsDatesWith(DateRange dateRange);

    boolean overlapsDatesAndDaysWith(DateRange dateRange, EnumSet<DayOfWeek> daysOfWeek);

}
