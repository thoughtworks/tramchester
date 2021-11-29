package com.tramchester.domain;

import java.io.PrintStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;

public interface ServiceCalendar {
    boolean operatesOn(LocalDate queryDate);

    void summariseDates(PrintStream printStream);

    LocalDate getEndDate();

    boolean operatesNoDays();

    boolean overlapsDatesWith(LocalDate startDate, LocalDate endDate);

    boolean overlapsDatesAndDaysWith(LocalDate startDate, LocalDate endDate, EnumSet<DayOfWeek> daysOfWeek);
}
