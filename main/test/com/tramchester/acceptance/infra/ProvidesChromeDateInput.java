package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.ProvidesDateInput;
import org.joda.time.format.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;

public class ProvidesChromeDateInput implements ProvidesDateInput {
    private final Locale locale = Locale.getDefault();

    @Override
    public String createDateInput(LocalDate localDate) {
        String formatter = DateTimeFormat.patternForStyle("S-", locale);
        org.joda.time.LocalDate jodaDate = new org.joda.time.LocalDate(localDate.getYear(), localDate.getMonthValue(),
                localDate.getDayOfMonth());
        String pattern = formatter.replaceAll("yy", "yyyy");
        return jodaDate.toString(pattern).replaceAll("/","");
    }

    @Override
    public String createTimeFormat(LocalTime time) {
        String formatter = DateTimeFormat.patternForStyle("-S", locale);
        org.joda.time.LocalTime jodaTime = new org.joda.time.LocalTime(time.getHour(), time.getMinute());
        return jodaTime.toString(formatter).replaceAll(" ", "");
    }
}
