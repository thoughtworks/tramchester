package com.tramchester.dataimport.data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ParsesDate {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    protected LocalDate parseDate(String text) {
        return LocalDate.parse(text, formatter);
    }

}
