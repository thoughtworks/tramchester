package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.ProvidesDateInput;
import org.joda.time.format.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;

public class ProvidesChromeDateInput implements ProvidesDateInput {
    Locale locale = Locale.getDefault();

    @Override
    public String createDateInput(LocalDate localDate) {
        String formatter = DateTimeFormat.patternForStyle("S-", locale);
        org.joda.time.LocalDate jodaDate = new org.joda.time.LocalDate(localDate.getYear(), localDate.getMonthValue(),
                localDate.getDayOfMonth());
        return jodaDate.toString(formatter.replaceAll("y","").replaceAll("Y",""));
    }

    @Override
    public String createTimeFormat(LocalTime time) {
        String formatter = DateTimeFormat.patternForStyle("-S", locale);
        org.joda.time.LocalTime jodaTime = new org.joda.time.LocalTime(time.getHour(), time.getMinute());
        return jodaTime.toString(formatter).replaceAll(" ", "");
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
