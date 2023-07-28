package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.ProvidesDateInput;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.Locale;

public class ProvidesChromeDateInput implements ProvidesDateInput {

    // NOTE: Must use the locale for Chrome to get correct behaviours locally, on CI servers, etc.
    private final Locale locale = Locale.getDefault();

    @Override
    public String createDateInput(LocalDate localDate) {

        String originalPattern = getPatternForDate(localDate);
        String pattern = originalPattern.replaceAll("yy", "yyyy");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return formatter.format(localDate).replaceAll("/","");

//        String formatter = DateTimeFormat.patternForStyle("S-", locale);
//        org.joda.time.LocalDate jodaDate = new org.joda.time.LocalDate(localDate.getYear(), localDate.getMonthValue(),
//                localDate.getDayOfMonth());
//        String pattern = formatter.replaceAll("yy", "yyyy");
//        return jodaDate.toString(pattern).replaceAll("/","");
    }

    private String getPatternForDate(LocalDate localDate) {
        // null = no time part for  the pattern
        return DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.SHORT, null, Chronology.from(localDate),
                locale);
    }

    @Override
    public String createTimeFormat(LocalTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
        String text = formatter.format(time);
        return text.replaceAll(" ","");

//        String formatter = DateTimeFormat.patternForStyle("-S", locale);
//        org.joda.time.LocalTime jodaTime = new org.joda.time.LocalTime(time.getHour(), time.getMinute());
//        return jodaTime.toString(formatter).replaceAll(" ", "");
    }
}
