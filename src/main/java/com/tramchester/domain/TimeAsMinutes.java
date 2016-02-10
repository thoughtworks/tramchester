package com.tramchester.domain;

import java.time.LocalTime;

public class TimeAsMinutes {
    public static final int SECONDS_IN_DAY = 24*60*60;

    public static int timeDiffMinutes(LocalTime arrivalTime, LocalTime departureTime) {
        int depSecs = departureTime.toSecondOfDay();
        int seconds;
        if (arrivalTime.isBefore(departureTime)) { // crosses midnight
            int secsBeforeMid = SECONDS_IN_DAY - depSecs;
            int secsAfterMid = arrivalTime.toSecondOfDay();
            seconds = secsBeforeMid + secsAfterMid;
        } else {
            seconds = arrivalTime.toSecondOfDay() - depSecs;
        }
        return seconds / 60;
    }

    protected int getMinutes(LocalTime time) {
        int hour = time.getHour();
        int minute = time.getMinute();
        if(hour == 0){
            hour = 24;
        } else if(hour == 1){
            hour = 25;
        }
        return (hour * 60) + minute;
    }
}
