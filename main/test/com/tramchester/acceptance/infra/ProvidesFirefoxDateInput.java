package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.ProvidesDateInput;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.time.LocalDate;
import java.time.LocalTime;

import static com.tramchester.TestConfig.dateFormatDashes;

public class ProvidesFirefoxDateInput implements ProvidesDateInput {

    @Override
    public String createDateInput(LocalDate localDate) {
        return localDate.format(dateFormatDashes);
    }

    @Override
    public String createTimeFormat(LocalTime time) {
        DateTimeFormatter format = DateTimeFormat.shortTime();
        org.joda.time.LocalTime jodaTime = new org.joda.time.LocalTime(time.getHour(), time.getMinute());
        String output = format.print(jodaTime);
        return output;
//        element.sendKeys(output);
    }
}
