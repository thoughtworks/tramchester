package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.ProvidesDateInput;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.text.DateFormat;
import java.util.Locale;

public class ProvidesFirefoxDateInput implements ProvidesDateInput {

    @Override
    public String createDateInput(LocalDate localDate) {
        return localDate.toString("YYYY-MM-dd");
    }

    @Override
    public String createTimeFormat(LocalTime time) {
        DateTimeFormatter format = DateTimeFormat.shortTime();
        String output = format.print(time);
        return output;
//        element.sendKeys(output);
    }
}
