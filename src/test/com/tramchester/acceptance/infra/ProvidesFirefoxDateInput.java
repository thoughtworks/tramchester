package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.ProvidesDateInput;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.Locale;

public class ProvidesFirefoxDateInput implements ProvidesDateInput {

    @Override
    public String createDateInput(LocalDate localDate) {
        return localDate.toString("YYYY-MM-dd");
    }

    @Override
    public void setTime(Actions builder, WebElement element, String input) throws InterruptedException {
        element.sendKeys(input);
    }
}
