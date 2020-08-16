package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.ProvidesDateInput;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.time.LocalDate;
import java.time.LocalTime;

import static com.tramchester.testSupport.TestEnv.dateFormatSimple;

public class ProvidesFirefoxDateInput implements ProvidesDateInput {

    @Override
    public String createDateInput(LocalDate localDate) {
        // firefox - day/month/year = dd/mm/yyyy

        return localDate.format(dateFormatSimple);
    }

    // local java localdatetime
    @Deprecated
    @Override
    public String createTimeFormat(LocalTime time) {


        DateTimeFormatter format = DateTimeFormat.shortTime();
        org.joda.time.LocalTime jodaTime = new org.joda.time.LocalTime(time.getHour(), time.getMinute());
        return format.print(jodaTime);
//        element.sendKeys(output);
    }
}
