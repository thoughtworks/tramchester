package com.tramchester.domain;

import java.time.LocalTime;

public class TimeAsMinutes {

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
