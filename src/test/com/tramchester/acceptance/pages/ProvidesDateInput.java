package com.tramchester.acceptance.pages;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

public interface ProvidesDateInput {

    String createDateInput(LocalDate localDate);
    String createTimeFormat(LocalTime input);
}
