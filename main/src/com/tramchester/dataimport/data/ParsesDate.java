package com.tramchester.dataimport.data;

import com.tramchester.domain.dates.TramDate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ParsesDate {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    protected LocalDate parseDate(String text) {
        return LocalDate.parse(text, formatter);
    }

    protected TramDate parseTramDate(String text) {
        return TramDate.parse(text, formatter);
    }

}
