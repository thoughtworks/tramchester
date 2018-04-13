package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.ProvidesDateInput;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.time.Duration;
import java.util.Locale;

public class ProvidesChromeDateInput implements ProvidesDateInput {
    Locale locale = Locale.getDefault();

    @Override
    public String createDateInput(LocalDate localDate) {
        String formatter = DateTimeFormat.patternForStyle("S-", locale);
        return localDate.toString(formatter.replaceAll("y","").replaceAll("Y",""));
    }

    @Override
    public void setTime(Actions builder, WebElement element, String input) throws InterruptedException {
        int chars = input.length();

        while (chars-- > 0) {
            builder.sendKeys(element, Keys.ARROW_LEFT);
        }
        builder.sendKeys(element, input);
        builder.pause(Duration.ofMillis(50));
        builder.build().perform();
        Thread.sleep(1000); // yuck, but chrome seems to have a big lag on AM/PM changes....
    }

}
