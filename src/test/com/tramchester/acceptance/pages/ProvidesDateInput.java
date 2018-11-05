package com.tramchester.acceptance.pages;


import java.time.LocalDate;
import java.time.LocalTime;

public interface ProvidesDateInput {

    String createDateInput(LocalDate localDate);
    String createTimeFormat(LocalTime input);
}
