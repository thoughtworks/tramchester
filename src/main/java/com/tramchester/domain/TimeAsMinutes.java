package com.tramchester.domain;

import org.joda.time.LocalTime;

public class TimeAsMinutes {

    public static int timeDiffMinutes(TramTime first, TramTime second) {
        int secondMins = second.minutesOfDay();
        int minutes;
        if (first.isBefore(second)) { // crosses midnight
            int minsBeforeMidnight = 24*60 - secondMins;
            int minsAfterMidnight = first.minutesOfDay();
            minutes = minsBeforeMidnight + minsAfterMidnight;
        } else {
            minutes = first.minutesOfDay() - secondMins;
        }
        return minutes;
    }

    @Deprecated
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

}
