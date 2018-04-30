package com.tramchester.domain;

import org.joda.time.LocalTime;

import java.util.Objects;

import static java.lang.String.format;

public class TramTime {
    private static TramTime[][] tramTimes = new TramTime[24][60];

    static {
        for(int hour=0; hour<24; hour++) {
            for(int minute=0; minute<60; minute++) {
                tramTimes[hour][minute] = new TramTime(hour,minute);
            }
        }
    }

    private int hour;
    private int minute;

    public static TramTime create(LocalTime localTime) {
        return tramTimes[localTime.getHourOfDay()][localTime.getMinuteOfHour()];
    }

    private TramTime(int hour, int minute) {
        this.hour = hour;
        this.minute = minute;
    }

    public int getHourOfDay() {
        return hour;
    }

    public int getMinuteOfHour() {
        return minute;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TramTime tramTime = (TramTime) o;
        return hour == tramTime.hour &&
                minute == tramTime.minute;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hour, minute);
    }

    @Override
    public String toString() {
        return "TramTime{" +
                "hour=" + hour +
                ", minute=" + minute +
                '}';
    }

    public String toPattern() {
        return format("%02d:%02d",hour,minute);
    }
}
