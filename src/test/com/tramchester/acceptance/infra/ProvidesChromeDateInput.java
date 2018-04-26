package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.ProvidesDateInput;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
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
    public String createTimeFormat(LocalTime time) {
        String formatter = DateTimeFormat.patternForStyle("-S", locale);
        return time.toString(formatter).replaceAll(" ", "");
    }

//    @Override
//    public void createTimeFormat(Actions builder, WebElement element, LocalTime localTime) throws InterruptedException {
//
////       String formatter = DateTimeFormat.patternForStyle("-S", locale);
////        String input = time.toString(formatter).replaceAll(" ", "");
//
//        DateTimeFormatter format = DateTimeFormat.shortTime();
//        String output = format.print(localTime);
//
//        int chars = output.length();
//
//        while (chars-- > 0) {
//            builder.sendKeys(element, Keys.ARROW_LEFT);
//        }
//        builder.sendKeys(element, output);
//        builder.pause(Duration.ofMillis(50));
//        builder.build().perform();
//        Thread.sleep(1000); // yuck, but chrome seems to have a big lag on AM/PM changes....
//    }

}
