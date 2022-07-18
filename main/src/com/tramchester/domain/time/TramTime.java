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
    public static final int MINS_IN_HOUR = 60;
    public static final int HOURS_IN_DAY = 24;

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

//    public static TramTime midnight() {
//        return factory.midnight();
//    }

    public static TramTime parse(String text) {
        return factory.parse(text);
    }

    public static TramTime nextDay(int hour, int minute) {
        return factory.of(hour, minute, 1);
    }

    public static TramTime nextDay(TramTime other) {
        if (!other.isValid()) {
            throw new RuntimeException("Called nextDay on invalid time");
        }
        return factory.of(other.hour, other.minute, 1);
    }

    public static Duration difference(TramTime first, TramTime second) {
        // todo seconds resolution
        return Duration.ofMinutes(diffenceAsMinutes(first, second));
    }

    /***
     * TODO Use seconds, or change to use Duration
     */
    private static int diffenceAsMinutes(TramTime first, TramTime second) {
        if (first.isAfterBasic(second)) {
            return diffenceAsMinutesOverMidnight(second, first);
        } else {
            return diffenceAsMinutesOverMidnight(first, second);
        }
    }

    private static int diffenceAsMinutesOverMidnight(TramTime earlier, TramTime later) {
        if (nextday(earlier) && today(later)) {
            int untilMidnight = (HOURS_IN_DAY * MINS_IN_HOUR) - later.minutesOfDay();
            return untilMidnight + earlier.minutesOfDay();
        } else {
            return later.minutesOfDay() - earlier.minutesOfDay();
        }
    }

    private static boolean today(TramTime tramTime) {
        return tramTime.offsetDays==0;
    }

    private static boolean nextday(TramTime tramTime) {
        return tramTime.offsetDays==1;
    }

    private int minutesOfDay() {
        return (hour * MINS_IN_HOUR) + minute;
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
        if (!this.isValid()) {
            return "TramTime{INVALID}";
        }

        String daysString = offsetDays>0 ? "d=" + offsetDays + " " : "";
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
//        if (hour==0 && minute==0) {
//            return true;
//        }
//        if (other.hour==0 && other.minute==0) {
//            return false;
//        }
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
        return other.offsetDays > this.offsetDays;
    }

    public boolean isAfter(TramTime other) {
        if (this.equals(other)) {
            return false;
        }
        if (this.offsetDays==other.offsetDays) {
            return isAfterBasic(other);
        }
        return offsetDays>other.offsetDays;
    }

    public boolean isAfterOrSame(TramTime other) {
        if (other.equals(this)) {
            return true;
        }
        return isAfter(other);
    }

    public TramTime minusMinutes(int amount) {
        if (!this.isValid()) {
            throw new RuntimeException("Attempt to subtract " + amount + " from an invalid time");
        }

        if (amount<0) {
            throw new RuntimeException("Subtract negative number " + amount);
        }

        int daysToSubtract = Integer.divideUnsigned(amount, HOURS_IN_DAY * MINS_IN_HOUR);

        int hoursToSubtract = Integer.divideUnsigned(amount, MINS_IN_HOUR);
        int minutesToSubtract = amount - ( hoursToSubtract * MINS_IN_HOUR);

        int newMins = minute - minutesToSubtract;
        if (newMins<0) {
            hoursToSubtract = hoursToSubtract+1;
            newMins = MINS_IN_HOUR + newMins;
        }

        int newHours = hour - hoursToSubtract;
        if (newHours<0) {
            daysToSubtract = daysToSubtract + 1;
            newHours = HOURS_IN_DAY + newHours;
        }

        int newOffsetDays = offsetDays - daysToSubtract;
        if (newOffsetDays<0) {
            throw new RuntimeException(format("Offset days (%S) is negative for %s minus %s", newOffsetDays, this, amount ));
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
        int mod = Math.floorMod(seconds, MINS_IN_HOUR);
        if (mod!=0) {
            throw new RuntimeException("Accuracy lost attempting to convert " + duration + " to minutes");
        }
        return (int) Math.floorDiv(seconds, MINS_IN_HOUR);
    }

    public TramTime plusMinutes(int minsToAdd) {
        if (minsToAdd==0) {
            return sameTime();
        }

        // calc amount to add as mins and hours
        int hoursToAdd = Integer.divideUnsigned(minsToAdd, MINS_IN_HOUR);
        final int remainder = minsToAdd - (hoursToAdd * MINS_IN_HOUR);

        // new total minutes
        int newMins = minute + remainder;

        // adjust new mins if > 1 hour
        if (newMins >= MINS_IN_HOUR) {
            hoursToAdd = hoursToAdd + 1;
            newMins = newMins - MINS_IN_HOUR;
        }

        // new total hours
        //final int currentHour = (hour==0 && minute==0) ? 24 : hour;
        int newHours = hour + hoursToAdd;

        int daysToAdd = 0;
        // adjust new hour if > 1 days
        if (newHours >= HOURS_IN_DAY) {
            daysToAdd = Integer.divideUnsigned(newHours, HOURS_IN_DAY);
            newHours = newHours - (daysToAdd * HOURS_IN_DAY);
        }

        // adjust for midnight being day before
//        if (newHours==0 && newMins==0 && daysToAdd>0) {
//            daysToAdd = daysToAdd -1;
//        }

        final int newOffsetDays = offsetDays + daysToAdd;

        return TramTime.of(newHours, newMins, newOffsetDays);

    }

    private TramTime sameTime() {
        return factory.of(hour, minute, offsetDays);
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
     * Is the Time within the proceeding minutes, or equal to, time.
     * OR between Midnight and time iff proceedingMinutes > minutes in day
     * USE TimeRange instead
     * @param proceedingMinutes interval to consider prior to time
     * @param other end of period
     * @return true if current time between (time - minutesBeforeTime) and time, inclusive
     */
    @Deprecated
    public boolean withinInterval(int proceedingMinutes, TramTime other) {
        TramTime startOfInterval;
        if (other.isNextDay()) {
            startOfInterval = other.minusMinutes(proceedingMinutes);
        } else {
            startOfInterval = (other.minutesOfDay() > proceedingMinutes) ? other.minusMinutes(proceedingMinutes)
                    : TramTime.of(0,1);
        }
        return between(startOfInterval, other);
    }

    @FunctionalInterface
    public interface ToTramTimeFunction<T> {
        TramTime applyAsTramTime(T value);
    }

    private static class Factory {
        private static final int NUM_DAYS = 2;
        private static final TramTime invalidTime = new TramTime();
        private final TramTime[][][] tramTimes = new TramTime[NUM_DAYS][HOURS_IN_DAY][MINS_IN_HOUR];

        private Factory() {
            for (int day = 0; day < 2; day++) {
                for(int hour = 0; hour< HOURS_IN_DAY; hour++) {
                    for(int minute = 0; minute< MINS_IN_HOUR; minute++) {
                        tramTimes[day][hour][minute] = new TramTime(hour, minute, day);
                    }
                }
            }
        }

//        private TramTime midnight() {
//            return tramTimes[0][0][0];
//        }

        private TramTime of(int hours, int minutes, int offsetDays) {
            if (offsetDays>=NUM_DAYS) {
                throw new RuntimeException(String.format("offsetdays is too large, got %s max %s hour: %s mins: %s",
                        offsetDays, NUM_DAYS, hours, minutes));
            }
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
            if (hour>= HOURS_IN_DAY) {
                hour = hour - HOURS_IN_DAY;
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
