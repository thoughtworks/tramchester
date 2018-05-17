package com.tramchester.domain;

import org.joda.time.LocalTime;

public class TimeAsMinutes {
    public static final int SECONDS_IN_DAY = 24*60*60;

    @Deprecated
    public static int timeDiffMinutes(LocalTime first, LocalTime second) {
        int depSecs = toSecondOfDay(second);
        int seconds;
        if (first.isBefore(second)) { // crosses midnight
            int secsBeforeMid = SECONDS_IN_DAY - depSecs;
            int secsAfterMid = toSecondOfDay(first);
            seconds = secsBeforeMid + secsAfterMid;
        } else {
            seconds = toSecondOfDay(first) - depSecs;
        }
        return seconds / 60;
    }

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
    private static int toSecondOfDay(LocalTime localTime) {
        return localTime.getMillisOfDay()/1000;
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

    @Deprecated
    public static int getMinutes(TramTime time) {
        int hour = time.getHourOfDay();
        int minute = time.getMinuteOfHour();
        if(hour == 0){
            hour = 24;
        } else if(hour == 1){
            hour = 25;
        }
        return (hour * 60) + minute;
    }

    @Deprecated
    public static int compare(LocalTime first, LocalTime second) {
        return getMinutes(first)-getMinutes(second);
    }

    public static int compare(TramTime first, TramTime second) {
        return first.minutesOfDay()-second.minutesOfDay();
    }
}
