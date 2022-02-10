package com.tramchester.domain.time;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.Objects;

import static java.lang.String.format;


public class TramTime implements Comparable<TramTime> {
    private static final String nextDaySuffix = "+24";

    // TODO Need to handle hours>24 flag as next day

    private final int hour;
    private final int minute;
    private final int offsetDays;
    private final int hash;
    private final String toPattern;

    private static final Factory factory = new Factory();

    private TramTime(int hour, int minute, int offsetDays) {
        this.hour = hour;
        this.minute = minute;
        this.offsetDays = offsetDays;
        this.hash = Objects.hash(hour, minute, offsetDays);
        toPattern = format("%02d:%02d",hour,minute); // expensive
    }

    private TramTime() {
        hour = Integer.MAX_VALUE;
        minute = Integer.MAX_VALUE;
        offsetDays = Integer.MAX_VALUE;
        hash = Integer.MAX_VALUE;
        toPattern = "invalid";
    }

    /***
     * A new tram time using only the hours and minutes from the local time
     * @see com.tramchester.domain.time.TramTime::ofHourMins
     * @param time local time
     * @return Tram time
     */
    @Deprecated
    public static TramTime of(LocalTime time) {
        return factory.of(time.getHour(), time.getMinute(), 0);
    }

    /***
     * A new tram time using only the hours and minutes from the local time
     * @param time local time
     * @return Tram time
     */
    public static TramTime ofHourMins(LocalTime time) {
        return factory.of(time.getHour(), time.getMinute(), 0);
    }

    private static TramTime of(int hours, int minutes, int offsetDays) {
        return factory.of(hours, minutes, offsetDays);
    }

    public static TramTime invalid() {
        return Factory.Invalid();
    }

    public boolean isValid() {
        return this != Factory.Invalid();
    }

    public static TramTime of(int hours, int minutes) {
        return factory.of(hours, minutes, 0);
    }

    public static TramTime midnight() {
        return factory.midnight();
    }

    public static TramTime parse(String text) {
        return factory.parse(text);
    }

    public static TramTime nextDay(int hour, int minute) {
        return factory.of(hour, minute, 1);
    }

    public static TramTime nextDay(TramTime other) {
        return factory.of(other.hour, other.minute, 1);
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

    public static <T> Comparator<T> comparing(ToTramTimeFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return (Comparator<T> & Serializable)
                (c1, c2) -> TramTime.compare(keyExtractor.applyAsTramTime(c1), keyExtractor.applyAsTramTime(c2));
    }

    private static int compare(TramTime a, TramTime b) {
        return a.compareTo(b);
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
        if ((this.equals(start)) || isAfter(start)) {
            return (this.equals(end) || isBefore(end));
        }
        return false;
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

    public boolean isAfterOrSame(TramTime other) {
        if (other.equals(this)) {
            return true;
        }
        return isAfter(other);
    }

    public TramTime minusMinutes(int amount) {

        if (amount<0) {
            throw new RuntimeException("Subtract negative number " + amount);
        }

        int daysToSubtract = Integer.divideUnsigned(amount, 24*60);

        int hoursToSubtract = Integer.divideUnsigned(amount, 60);
        int minutesToSubtract = amount - ( hoursToSubtract * 60);

        int newMins = minute - minutesToSubtract;
        if (newMins<0) {
            hoursToSubtract = hoursToSubtract+1;
            newMins = 60 + newMins;
        }

        int newHours = hour - hoursToSubtract;
        if (newHours<0) {
            daysToSubtract = daysToSubtract + 1;
            newHours = 24 + newHours;
        }

        int newOffsetDays = offsetDays - daysToSubtract;
        if (newOffsetDays<0) {
            throw new RuntimeException("Result is in the past for " + this + " minus " + amount + " minutes");
        }

        return TramTime.of(newHours, newMins, newOffsetDays);
    }

    public TramTime plus(Duration duration) {
        final int minutes = getMinutesSafe(duration);
        return plusMinutes(minutes);
    }

    public TramTime minus(Duration duration) {
        final int minutes = getMinutesSafe(duration);
        return minusMinutes(minutes);
    }

    // TODO Store seconds in tram time
    private int getMinutesSafe(Duration duration) {
        long seconds = duration.getSeconds();
        int mod = Math.floorMod(seconds, 60);
        if (mod!=0) {
            throw new RuntimeException("Accuracy lost attempting to convert " + duration + " to minutes");
        }
        return (int) Math.floorDiv(seconds, 60);
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

    /***
     * Is time within the preceeding minutes, or equal to, time.
     * OR between Midnight and time iff proceedingMinutes > minutes in day
     * @param proceedingMinutes interval to consider prior to time
     * @param time end of period
     * @return true if current time between (time - minutesBeforeTime) and time, inclusive
     */
    public boolean withinInterval(int proceedingMinutes, TramTime time) {
        TramTime startOfInterval;
        if (time.isNextDay()) {
            startOfInterval = time.minusMinutes(proceedingMinutes);
        } else {
            startOfInterval = (time.minutesOfDay() > proceedingMinutes) ? time.minusMinutes(proceedingMinutes)
                    : TramTime.of(0,0);
        }
        return between(startOfInterval, time);
    }


    @FunctionalInterface
    public interface ToTramTimeFunction<T> {
        TramTime applyAsTramTime(T value);
    }

    private static class Factory {
        private static final TramTime invalidTime = new TramTime();
        private final TramTime[][][] tramTimes = new TramTime[2][24][60];

        private Factory() {
            for (int day = 0; day < 2; day++) {
                for(int hour=0; hour<24; hour++) {
                    for(int minute=0; minute<60; minute++) {
                        tramTimes[day][hour][minute] = new TramTime(hour, minute, day);
                    }
                }
            }
        }

        private TramTime of(LocalTime time) {
            final TramTime result = tramTimes[0][time.getHour()][time.getMinute()];
            if (result.asLocalTime().compareTo(time)!=0) {
                // right now only represent times to a one minute accuracy
                throw new RuntimeException("Accuracy lost converting " + time + " to a tram time " + result);
            }
            return result;
        }

        private TramTime midnight() {
            return tramTimes[1][0][0];
        }

        private TramTime of(int hours, int minutes, int offsetDays) {
            return tramTimes[offsetDays][hours][minutes];
        }

        private TramTime parse(String text) {
            int offsetDays = 0;

            if (text.endsWith(nextDaySuffix)) {
                offsetDays = 1;
                text = text.replace(nextDaySuffix,"");
            }

            // Note: indexed parse faster than using String.split

            int firstDivider = text.indexOf(':');

            int hour = Integer.parseUnsignedInt(text,0, firstDivider ,10);
            // gtfs standard represents service next day by time > 24:00:00
            if (hour>=24) {
                hour = hour - 24;
                offsetDays = offsetDays + 1;
            }
            if (hour>23) {
                // spanning 2 days, cannot handle yet, TODO very long ferry or train >2 days???
                return invalidTime;
            }

            int minutes = Integer.parseUnsignedInt(text, firstDivider+1, firstDivider+3, 10);
            if (minutes > 59) {
                return invalidTime;
            }
            return TramTime.of(hour, minutes, offsetDays);
        }

        public static TramTime Invalid() {
            return invalidTime;
        }
    }
}
