package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.ProvidesDateInput;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import static com.tramchester.testSupport.TestEnv.dateFormatDashes;
import static com.tramchester.testSupport.TestEnv.dateFormatSimple;

public class ProvidesFirefoxDateInput implements ProvidesDateInput {

    @Override
    public String createDateInput(LocalDate localDate) {
        // for sendKeys has to be 1972-12-24 even though this does match the actual keystrokes a user would input

        return localDate.format(dateFormatDashes);
    }

    // local java localdatetime
    @Deprecated
    @Override
    public String createTimeFormat(LocalTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
        return formatter.format(time);

//        DateTimeFormatter format = DateTimeFormat.shortTime();
//        org.joda.time.LocalTime jodaTime = new org.joda.time.LocalTime(time.getHour(), time.getMinute());
//        return format.print(jodaTime);
    }
}
