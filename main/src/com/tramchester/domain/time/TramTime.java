package com.tramchester.domain.time;

import com.tramchester.domain.dates.TramDate;
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
    private static final CharSequence nextDaySuffix = "+24";
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

    /***
     * Parse string with format HH:MM[+24] i.e. 11:45 03:26+24 23:55
     * @param text string to parse
     * @return TramTram version or TramTime.Invalid if unable to parse
     */
    public static TramTime parse(final CharSequence text) {
        return factory.parse(text);
    }

    /***
     * Parse string with format HHMM i.e. 1145 0326 2355
     * @param text the string to parse
     * @param offset to HHMM part of the string
     * @return TramTime or TramTime.Invalid
     */
    public static TramTime parseBasicFormat(final CharSequence text, final int offset) {
        final int hour = Factory.parseHour(text, offset);
        if (hour<0) {
            return invalid();
        }
        final int minute = Factory.parseMinute(text, offset);
        if (minute<0) {
            return invalid();
        }
        return TramTime.of(hour, minute);
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

    public LocalDateTime toDate(TramDate startDate) {
        LocalDateTime base = LocalDateTime.of(startDate.toLocalDate(), asLocalTime());
        return base.plusDays(offsetDays);
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

        private TramTime of(int hours, int minutes, int offsetDays) {
            if (offsetDays>=NUM_DAYS) {
                throw new RuntimeException(String.format("offsetdays is too large, got %s max %s hour: %s mins: %s",
                        offsetDays, NUM_DAYS, hours, minutes));
            }
            return tramTimes[offsetDays][hours][minutes];
        }

        /***
         * Parse text in format HH:MM[+24]
         * @param text to parse
         * @return the TramTime
         */
        private TramTime parse(final CharSequence text) {
            int offsetDays = 0;

            final int length = text.length();
            if (length < 5) {
                throw new RuntimeException("Provided text is too short '" + text + "'");
            }

            if (length > 5) {
                CharSequence suffix = text.subSequence(length - 3, length);
                if (suffix.equals(nextDaySuffix)) {
                    offsetDays = 1;
                }
            }

            // Note: indexed parse faster than using String.split

            int hour = parseHour(text,0);
            if (hour<0) {
                return invalidTime;
            }

            // gtfs standard represents service next day by time > 24:00:00
            if (hour>= HOURS_IN_DAY) {
                hour = hour - HOURS_IN_DAY;
                offsetDays = offsetDays + 1;
            }
            if (hour>23) {
                // spanning 2 days, cannot handle yet, TODO very long ferry or train >2 days???
                return invalidTime;
            }

            final int minutes = parseMinute(text, 1); // offset 1, account for ':'
            if (minutes > 59 || minutes < 0) {
                return invalidTime;
            }

            if (offsetDays>=NUM_DAYS) {
                throw new RuntimeException(format("Resulting offset days (%s) is too large, hour: %s minute: %s text '%s",
                        offsetDays, hour, minutes, text));
            }
            return TramTime.of(hour, minutes, offsetDays);
        }

        /***
         * Parse hours part of a 4 character string
         * @param text HHMM i.e. 1145 1656
         * @param offset to start of HHMM part of the string
         * @return hours part i.e. 11 16
         */
        private static int parseHour(final CharSequence text, int offset) {
            if (text.length() < offset+4) {
                throw new NumberFormatException(String.format("String too short for given offset (%s), got %s", offset, text));
            }
            final char firstDigit = text.charAt(offset);
            final char secondDigit = text.charAt(1+offset);

            if (firstDigit > '2') {
                return -1;
            }

            final int hourTenDigit = Character.digit(firstDigit, 10);
            final int hourUnitsDigit = Character.digit(secondDigit, 10);

            return (hourTenDigit*10) + hourUnitsDigit;

        }

        /***
         * Parse minutes part of a 4 character string
         * @param text HHMM i.e. 1145 1656
         * @param offset to start of HHMM part of the string
         * @return minutes part i.e. 45 56
         */
        private static int parseMinute(final CharSequence text, final int offset) {
            if (text.length() < offset+4) {
                throw new NumberFormatException(String.format("String too short for given offset (%s), got %s", offset, text));
            }
            final char firstDigit = text.charAt(2+offset);
            final char secondDigit = text.charAt(3+offset);

            if (firstDigit > '5') {
                return -1;
            }

            final int minsTenDigit = Character.digit(firstDigit, 10);
            final int minsUnitsDigit = Character.digit(secondDigit, 10);

            return (minsTenDigit*10) + minsUnitsDigit;

        }

        public static TramTime Invalid() {
            return invalidTime;
        }
    }
}
