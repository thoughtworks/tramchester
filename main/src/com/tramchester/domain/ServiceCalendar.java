package com.tramchester.domain;

import java.io.PrintStream;
import java.time.LocalDate;

public interface ServiceCalendar {
    boolean operatesOn(LocalDate queryDate);

    boolean isExcluded(LocalDate queryDate);

    void summariseDates(PrintStream printStream);

    LocalDate getEndDate();

    boolean operatesNoDays();
}
