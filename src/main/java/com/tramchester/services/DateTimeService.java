package com.tramchester.services;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class DateTimeService {
    public static final DateTimeFormatter formatter = DateTimeFormat.forPattern("HH:mm:ss");

    public int getMinutesFromMidnight(String time) {
        DateTime theTime = DateTime.parse(time, formatter);
        int hourOfDay = theTime.getHourOfDay();
        if(hourOfDay == 0){
            hourOfDay = 24;
        } else if(hourOfDay == 1){
            hourOfDay = 25;
        }

        return (hourOfDay * 60) + theTime.getMinuteOfHour();
    }
}
