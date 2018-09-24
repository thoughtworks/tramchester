package com.tramchester.domain;

import com.tramchester.domain.exceptions.TramchesterException;
import org.joda.time.LocalTime;

import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;

public class TramTime implements Comparable<TramTime> {
    // NOTE:
    // Midnight and 1AM are mapped to >24*60 minutes to preserve meaning from tfgm data where a
    // 00:14 tram (for example) by convention runs on the day *after* the date for the trip
    // i.e. the 00:14 on 14th June 2018 actually runs 00:14 15th June 2018 and hence after rest of
    // trips that day
    private int hour;
    private int minute;

    private static TramTime[][] tramTimes = new TramTime[24][60];

    static {
        for(int hour=0; hour<24; hour++) {
            for(int minute=0; minute<60; minute++) {
                tramTimes[hour][minute] = new TramTime(hour,minute);
            }
        }
    }

    public static TramTime create(int hours, int minutes) throws TramchesterException {
        if (hours>23|| minutes>59) {
            throw new TramchesterException(format("Unable to create time from hour:%s and minutes:%s", hours,minutes));
        }
        return tramTimes[hours][minutes];
    }

    public static TramTime create(LocalTime time) {
        return tramTimes[time.getHourOfDay()][time.getMinuteOfHour()];
    }

    public static TramTime midnight() {
        return tramTimes[0][0];
    }

    public static Optional<TramTime> parse(String text) throws TramchesterException {
        String[] split = text.split(":",3);

        Integer hour = Integer.parseInt(split[0]);
        if (hour==24 || hour==25) {
            hour = 0;
        }
        Integer minutes = Integer.parseInt(split[1]);
        if (hour>23 || minutes>59) {
            return Optional.empty();
        }

        return Optional.of(TramTime.create(hour,minutes));
    }

    private TramTime(int hour, int minute) {
        this.hour = hour;
        this.minute = minute;
    }

    public static TramTime fromMinutes(int minutesOfDays) throws TramchesterException {
        int hour = minutesOfDays / 60;
        int minutes = minutesOfDays - (hour*60);
        if (hour==24) {
            hour = 0;
        }
        if (hour==25) {
            hour = 1;
        }
        return create(hour,minutes);
    }

    public TramTime minusMinutes(int delta) throws TramchesterException {
        int mins = minutesOfDay() - delta;
        return fromMinutes(mins);
    }

    public TramTime plusMinutes(int delta) throws TramchesterException {
        int mins = minutesOfDay() + delta;
        return fromMinutes(mins);
    }

    public static int diffenceAsMinutes(TramTime first, TramTime second) {
        int secondMins = second.minutesOfDay();
        int diff;
        if (first.isBefore(second)) { // crosses midnight
            int minsBeforeMidnight = 24*60 - secondMins;
            int minsAfterMidnight = first.minutesOfDay();
            diff = minsBeforeMidnight + minsAfterMidnight;
        } else {
            diff = first.minutesOfDay() - secondMins;
        }
        return diff;
    }

    public static TramTime now() {
        return create(LocalTime.now());
    }

    public int minutesOfDay() {
        int theHour = hour;
        if(hour == 0){
            theHour = 24;
        } else if(hour == 1){
            theHour = 25;
        }
        return (theHour * 60) + minute;
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

    // "HH:mm:ss"
    public String tramDataFormat() {
        return String.format("%s:00",toPattern());
    }

    public boolean isBefore(TramTime other) {
        if (hour<other.hour) {
            return true;
        }
        if (hour==other.hour) {
            return minute<other.minute;
        }
        return false;
    }

    public boolean isAfter(TramTime other) {
        if (hour>other.hour) {
            return true;
        }
        if (hour==other.hour) {
            return minute>other.minute;
        }
        return false;
    }

    @Override
    public int compareTo(TramTime other) {
        return this.minutesOfDay()-other.minutesOfDay();
    }

}
