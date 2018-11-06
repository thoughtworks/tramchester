package com.tramchester.domain;

import com.tramchester.domain.exceptions.TramchesterException;

import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;

public class  TramTime implements Comparable<TramTime> {

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

    public static TramTime of(LocalTime time) {
        return tramTimes[time.getHour()][time.getMinute()];
    }

    public static TramTime midnight() {
        return tramTimes[0][0];
    }

    public static Optional<TramTime> parse(String text) {
        String[] split = text.split(":",3);

        Integer hour = Integer.parseInt(split[0]);
        // Received Tram data contains 24 and 25 as an hour
        if (hour==24) {
            hour = 0;
        }
        if (hour==25) {
            hour = 1;
        }
        Integer minutes = Integer.parseInt(split[1]);
        if (hour>23 || minutes>59) {
            return Optional.empty();
        }
        return Optional.of(TramTime.of(hour,minutes));
    }

    private TramTime(int hour, int minute) {
        this.hour = hour;
        this.minute = minute;
    }

    public static TramTime of(int hours, int minutes) {
        return tramTimes[hours][minutes];
    }

    public static int diffenceAsMinutes(TramTime first, TramTime second) {
        if (first.isAtOrAfter(second)) {
            return diffenceAsMinutesOverMidnight(second, first);
        } else {
            return diffenceAsMinutesOverMidnight(first, second);
        }
    }

    private static int diffenceAsMinutesOverMidnight(TramTime earlier, TramTime later) {
        if (earlier.isEarlyMorning() && later.isLateNight()) {
            int untilMidnight = (24*60)-later.minutesOfDay();
            return untilMidnight+earlier.minutesOfDay();
        } else {
            return later.minutesOfDay()-earlier.minutesOfDay();
        }
    }

    private boolean isLateNight() {
        return hour==23 || hour==22;
    }

    public boolean isEarlyMorning() {
        return hour==0 || hour==1;
    }

//    @Deprecated
//    public static TramTime now() {
//        return of(LocalTime.now());
//    }

    public int minutesOfDay() {
        return (hour * 60) + minute;
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

    private boolean isAtOrAfter(TramTime other) {
        if (hour>other.hour) {
            return true;
        }
        if (hour==other.hour) {
            return minute>other.minute || minute==other.minute;
        }
        return false;
    }

    @Override
    public int compareTo(TramTime other) {
        if (this.isLateNight() && other.isEarlyMorning()) {
            return -1;
        }
        return this.minutesOfDay()-other.minutesOfDay();
    }

    public LocalTime asLocalTime() {
        return LocalTime.of(hour, minute);
    }

    public boolean departsAfter(TramTime other) {
        if (this.isEarlyMorning() && other.isLateNight()) {
            return true;
        }
        return this.isAtOrAfter(other);
    }
}
