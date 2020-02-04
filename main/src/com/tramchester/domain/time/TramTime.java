package com.tramchester.domain.time;

import com.tramchester.domain.exceptions.TramchesterException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;

public class  TramTime implements Comparable<TramTime> {

    private int hour;
    private int minute;
    private int hash;

    private static TramTime[][] tramTimes = new TramTime[24][60];

    static {
        for(int hour=0; hour<24; hour++) {
            for(int minute=0; minute<60; minute++) {
                tramTimes[hour][minute] = new TramTime(hour,minute);
            }
        }
    }

    // method only used during cut over from minutes past minute refactoring
    @Deprecated
    public static TramTime create(int hours, int minutes) throws TramchesterException {
        if (hours>23|| minutes>59) {
            throw new TramchesterException(format("Unable to create time from hour:%s and minutes:%s", hours,minutes));
        }
        return tramTimes[hours][minutes];
    }

    public static TramTime of(LocalTime time) {
        return tramTimes[time.getHour()][time.getMinute()];
    }


    public static TramTime of(LocalDateTime dateAndTime) {
        return of(dateAndTime.toLocalTime());
    }

    public static TramTime midnight() {
        return tramTimes[0][0];
    }

    public static Optional<TramTime> parse(String text) {
        String[] split = text.split(":",3);

        int hour = Integer.parseInt(split[0]);
        // Received Tram data contains 24 and 25 as an hour
        if (hour==24) {
            hour = 0;
        }
        if (hour==25) {
            hour = 1;
        }
        int minutes = Integer.parseInt(split[1]);
        if (hour>23 || minutes>59) {
            return Optional.empty();
        }
        return Optional.of(TramTime.of(hour,minutes));
    }

    private TramTime(int hour, int minute) {
        this.hour = hour;
        this.minute = minute;
        this.hash = Objects.hash(hour, minute);
    }

    public static TramTime of(int hours, int minutes) {
        return tramTimes[hours][minutes];
    }

    public static int diffenceAsMinutes(TramTime first, TramTime second) {
        if (first.isAfterBasic(second)) {
            return diffenceAsMinutesOverMidnight(second, first);
        } else {
            return diffenceAsMinutesOverMidnight(first, second);
        }
    }

    private static int diffenceAsMinutesOverMidnight(TramTime earlier, TramTime later) {
        if (isEarlyMorning(earlier.hour) && isLateNight(later.hour)) {
            int untilMidnight = (24*60)-later.minutesOfDay();
            return untilMidnight+earlier.minutesOfDay();
        } else {
            return later.minutesOfDay()-earlier.minutesOfDay();
        }
    }

    public static boolean checkTimingOfStops(TimeWindow timeWindow, TramTime firstStopDepartureTime, TramTime secondStopArriveTime) {
        TramTime queryTime = timeWindow.queryTime();
        int window = timeWindow.withinMins();

        // In the past
        if (firstStopDepartureTime.isBefore(timeWindow.queryTime())) {
            return false;
        }

        if (secondStopArriveTime.asLocalTime().isBefore(firstStopDepartureTime.asLocalTime())) {
            if (!TramTime.isEarlyMorning(secondStopArriveTime.getHourOfDay())) {
                return false;
            }
        }

        // too long to wait
        if (TramTime.diffenceAsMinutes(firstStopDepartureTime,  queryTime) >= window) {
            return false;
        }

        return true;
    }

    public static boolean isLateNight(int hour) {
        return hour==23 || hour==22;
    }

    public static boolean isEarlyMorning(int hour) {
        return hour==0 || hour==1;
    }

    // try to avoid this method due to ambiguity in early hours - is it today or tomorrow?
    @Deprecated
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
        // can just use this
        return this == o;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return "TramTime{" +
                "h=" + hour +
                ", m=" + minute +
                '}';
    }

    public String toPattern() {
        return format("%02d:%02d",hour,minute);
    }

    // "HH:mm:ss"
    public String tramDataFormat() {
        return String.format("%s:00", toPattern());
    }

    // is after with compensation for late nights
    public boolean isAfterBasic(TramTime other) {
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
        if (isLateNight(hour) && isEarlyMorning(other.hour)) {
            return -1; // is less than
        }
        if (isEarlyMorning(hour) && isLateNight(other.hour)) {
            return 1; // is more than
        }
        // otherwise, same hour then use minutes
        if (this.hour==other.hour) {
            return this.minute-other.minute;
        }
        // just use hour
        return this.hour-other.hour;
    }

    public LocalTime asLocalTime() {
        return LocalTime.of(hour, minute);
    }

    public boolean departsAfter(TramTime other) {
        if (isEarlyMorning(hour) && isLateNight(other.hour)) {
            return true;
        }
        return this.isAfterBasic(other);
    }

    // inclusive
    public boolean between(TramTime start, TramTime end) {
        boolean startFlag = (this.equals(start)) || isAfter(start);
        if (!startFlag) {
            return false;
        }
        return (this.equals(end) || isBefore(end));
    }

    public boolean isBefore(TramTime other) {
        if (this.equals(other)) {
            return false;
        }
        if (isEarlyMorning(other.getHourOfDay())) {
            if (isLateNight(hour)) {
                return true;
            }
            if (!isEarlyMorning(hour))
            {
                return true;
            }
        } else {
            if (isEarlyMorning(hour)) {
                return false;
            }
        }

        return other.isAfterBasic(this);

    }

    public boolean isAfter(TramTime other) {
        if (this.equals(other)) {
            return false;
        }
        if (isEarlyMorning(hour)) {
            if (isLateNight(other.hour)) {
                return true;
            }
            if (isEarlyMorning(other.hour)) {
                return this.isAfterBasic(other);
            } else {
                return true;
            }
        } else if (isEarlyMorning(other.hour)) {
            if (isEarlyMorning(hour)) {
                return this.isAfterBasic(other);
            }
            return false;
        }

        return this.isAfterBasic(other);
    }

    public TramTime minusMinutes(int amount) {
        if (amount>3600) {
            throw new RuntimeException("Cannot subtract more than a hour");
        }
        int hoursToSubtract = Integer.divideUnsigned(amount, 60);
        int minutesToSubtract = amount - ( hoursToSubtract * 60);

        int newMins = minute - minutesToSubtract;
        if (newMins<0) {
            hoursToSubtract=hoursToSubtract+1;
            newMins = 60 + newMins;
        }
        int newHours = hour - hoursToSubtract;
        if (newHours<0) {
            newHours = 24 + newHours;
        }
        return TramTime.of(newHours, newMins);
    }

    public TramTime plusMinutes(int amount) {
        int hoursToAdd = Integer.divideUnsigned(amount, 60);
        int minutesToAdd = amount - (hoursToAdd*60);

        int newMins = minute + minutesToAdd;
        if (newMins>=60) {
            hoursToAdd = hoursToAdd + 1;
            newMins = newMins - 60;
        }
        int newHours = (hour + hoursToAdd) % 24;
        return TramTime.of(newHours, newMins);
    }
}
