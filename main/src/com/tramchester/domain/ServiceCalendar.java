package com.tramchester.domain;

import java.io.PrintStream;
import java.time.LocalDate;

public interface ServiceCalendar {
    boolean operatesOn(LocalDate queryDate);

    void summariseDates(PrintStream printStream);

    LocalDate getEndDate();

    boolean operatesNoDays();

    boolean overlapsWith(LocalDate startDate, LocalDate endDate);
}
