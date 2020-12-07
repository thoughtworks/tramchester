package com.tramchester.domain.time;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;


public class  TramTime implements Comparable<TramTime> {
    private static final String nextDaySuffix = "+24";

    // TODO Need to handle hours>24 flag as next day

    private final int hour;
    private final int minute;
    private final int offsetDays;
    private final int hash;
    private final String toPattern;

    private static final TramTime[][][] tramTimes = new TramTime[2][24][60];

    static {
        for (int day = 0; day < 2; day++) {
            for(int hour=0; hour<24; hour++) {
                for(int minute=0; minute<60; minute++) {
                    tramTimes[day][hour][minute] = new TramTime(hour, minute, day);
                }
            }
        }
    }

    public static TramTime of(LocalTime time) {
        return tramTimes[0][time.getHour()][time.getMinute()];
    }

    public static TramTime of(LocalDateTime dateAndTime) {
        return of(dateAndTime.toLocalTime());
    }

    public static TramTime midnight() {
        return tramTimes[1][0][0];
    }

    public static Optional<TramTime> parse(String text) {
        int offsetDays = 0;

        if (text.endsWith(nextDaySuffix)) {
            offsetDays = 1;
            text = text.replace(nextDaySuffix,"");
        }

        // Note: indexed parse faster than using String.split

        int hour = Integer.parseUnsignedInt(text,0,2 ,10);
        // gtfs standard represents service next day by time > 24:00:00
        if (hour>=24) {
            hour = hour - 24;
            offsetDays = offsetDays + 1;
        }
        if (hour>23) {
            // spanning 2 days, cannot handle yet
            return Optional.empty();
        }

        int minutes = Integer.parseUnsignedInt(text, 3, 5, 10);
        if (minutes > 59) {
            return Optional.empty();
        }
        return Optional.of(TramTime.of(hour,minutes, offsetDays));
    }

    private TramTime(int hour, int minute, int offsetDays) {
        this.hour = hour;
        this.minute = minute;
        this.offsetDays = offsetDays;
        this.hash = Objects.hash(hour, minute, offsetDays);
        toPattern = format("%02d:%02d",hour,minute); // expensive
    }

    public static TramTime nextDay(int hour, int minute) {
        return of(hour, minute, 1);
    }

    private static TramTime of(int hours, int minutes, int offsetDays) {
        return tramTimes[offsetDays][hours][minutes];
    }

    public static TramTime of(int hours, int minutes) {
        return tramTimes[0][hours][minutes];
    }

    public static int diffenceAsMinutes(TramTime first, TramTime second) {
        if (first.isAfterBasic(second)) {
            return diffenceAsMinutesOverMidnight(second, first);
        } else {
            return diffenceAsMinutesOverMidnight(first, second);
        }
    }

    private static int diffenceAsMinutesOverMidnight(TramTime earlier, TramTime later) {
        if (nextday(earlier) && today(later)) {
            int untilMidnight = (24*60)-later.minutesOfDay();
            return untilMidnight+earlier.minutesOfDay();
        } else {
            return later.minutesOfDay()-earlier.minutesOfDay();
        }
    }

    private static boolean today(TramTime tramTime) {
        return tramTime.offsetDays==0;
    }

    private static boolean nextday(TramTime tramTime) {
        return tramTime.offsetDays==1;
    }

    @Deprecated
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

    @Deprecated
    private static boolean isEarlyMorning(int hour) {
        return hour==0 || hour==1;
    }

    private int minutesOfDay() {
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
        String daysString = offsetDays>0 ?"d=" + offsetDays + " " : "";
        return "TramTime{" +
                daysString +
                "h=" + hour +
                ", m=" + minute +
                '}';
    }

    public String toPattern() {
        return toPattern;
    }

    // is after with compensation for late nights
    private boolean isAfterBasic(TramTime other) {
        if (hour>other.hour) {
            return true;
        }
        if (hour==other.hour) {
            return minute>other.minute || minute==other.minute;
        }
        return false;
    }

    @Override
    public int compareTo(@NotNull TramTime other) {
        if (this.offsetDays==other.offsetDays) {
            if (this.hour==other.hour) {
                return Integer.compare(this.minute, other.minute);
            }
            return Integer.compare(this.hour, other.hour);
        }
        return Integer.compare(this.offsetDays, other.offsetDays);
    }

    public LocalTime asLocalTime() {
        return LocalTime.of(hour, minute);
    }

    public boolean departsAfter(TramTime other) {
        if (this.offsetDays==other.offsetDays) {
            return this.isAfterBasic(other);
        }
        return this.offsetDays>other.offsetDays;
    }

    // inclusive
    public boolean between(TramTime start, TramTime end) {
        boolean startFlag = (this.equals(start)) || isAfter(start);
        if (!startFlag) {
            return false;
        }
        return (this.equals(end) || isBefore(end));
    }

    public boolean between(ServiceTime start, ServiceTime end) {
        return between(start.asTramTime(), end.asTramTime());
    }

    public boolean isBefore(TramTime other) {
        if (this.equals(other)) {
            return false;
        }
        if (this.offsetDays==other.offsetDays) {
            return other.isAfterBasic(this);
        }
        return other.offsetDays>this.offsetDays;
    }

    public boolean isAfter(TramTime other) {
        if (this.equals(other)) {
            return false;
        }
        if (this.offsetDays==other.offsetDays) {
            return this.isAfterBasic(other);
        }
        return this.offsetDays>other.offsetDays;
    }

    // TODO Sort out this mess
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
        int newOffsetDays = this.offsetDays;
        int newHours = hour - hoursToSubtract;
        if (newHours<0) {
            newOffsetDays = offsetDays-1;
            newHours = 24 + newHours;
        }
        if (newOffsetDays<0) {
            throw new RuntimeException("Result is in past. amount="+amount+ " current="+toString());
        }
        return TramTime.of(newHours, newMins, newOffsetDays);
    }

    public TramTime plusMinutes(int amount) {
        int hoursToAdd = Integer.divideUnsigned(amount, 60);
        int minutesToAdd = amount - (hoursToAdd*60);

        int newMins = minute + minutesToAdd;
        if (newMins>=60) {
            hoursToAdd = hoursToAdd + 1;
            newMins = newMins - 60;
        }
        int newHours = (hour + hoursToAdd);
        int newOffsetDays = offsetDays + newHours / 24;
        return TramTime.of(newHours % 24, newMins, newOffsetDays);
    }

    public boolean isNextDay() {
        return offsetDays>0;
    }

    public int getMinuteOfDay() {
        return (getHourOfDay()*60) + getMinuteOfHour();
    }

    public String serialize() {
        String result = toPattern;
        if (isNextDay()) {
            result = result + nextDaySuffix;
        }
        return result;
    }

    // to date, respecting day offset
    public LocalDateTime toDate(LocalDate startDate) {
        LocalDateTime base = LocalDateTime.of(startDate, asLocalTime());
        return base.plusDays(offsetDays);
    }
}
