package com.tramchester.domain;

import org.joda.time.LocalTime;

public class TimeAsMinutes {
    public static final int SECONDS_IN_DAY = 24*60*60;

    public static int timeDiffMinutes(LocalTime arrivalTime, LocalTime departureTime) {
        int depSecs = toSecondOfDay(departureTime);
        int seconds;
        if (arrivalTime.isBefore(departureTime)) { // crosses midnight
            int secsBeforeMid = SECONDS_IN_DAY - depSecs;
            int secsAfterMid = toSecondOfDay(arrivalTime);
            seconds = secsBeforeMid + secsAfterMid;
        } else {
            seconds = toSecondOfDay(arrivalTime) - depSecs;
        }
        return seconds / 60;
    }

    private static int toSecondOfDay(LocalTime localTime) {
        return localTime.getMillisOfDay()/1000;
    }

    public static int getMinutes(LocalTime time) {
        int hour = time.getHourOfDay();
        int minute = time.getMinuteOfHour();
        if(hour == 0){
            hour = 24;
        } else if(hour == 1){
            hour = 25;
        }
        return (hour * 60) + minute;
    }

    public static int compare(LocalTime first, LocalTime second) {
        return getMinutes(first)-getMinutes(second);
    }
}
