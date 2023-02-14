package com.tramchester.domain.time;

import java.time.Duration;

import static java.lang.String.format;

public class TimeRange {
    private TramTime begin;
    private TramTime end;

    public TimeRange(TramTime begin, TramTime end) {
        this.begin = begin;
        this.end = end;
        if (end.isBefore(begin)) {
            throw new RuntimeException(format("End time %s is before begin %s", end, begin));
        }
        if (begin.isNextDay() && !end.isNextDay()) {
            throw new RuntimeException(format("Begin time %s is next day but end is not %s", begin, end));
        }
    }

    public TimeRange(TramTime tramTime) {
        begin = tramTime;
        end = tramTime;
    }

    public static TimeRange of(TramTime time, Duration before, Duration after) {
        if (time.getHourOfDay()==0 && !time.isNextDay()) {
            Duration currentMinsOfDay = Duration.ofMinutes(time.getMinuteOfHour());
            if (Durations.greaterThan(before, currentMinsOfDay)) {
                before = currentMinsOfDay;
            }
        }
        TramTime begin = time.minus(before);
        TramTime end = time.plus(after);
        return new TimeRange(begin, end);
    }

    public static TimeRange of(TramTime time) {
        return new TimeRange(time);
    }

    public static TimeRange of(TramTime first, TramTime second) {
        return new TimeRange(first, second);
    }

    public boolean contains(TramTime time) {
        return time.between(begin, end);
    }


    @Override
    public String toString() {
        return "TimeRange{" +
                "begin=" + begin +
                ", end=" + end +
                '}';
    }

    public void updateToInclude(TramTime callingTime) {
        if (callingTime.isBefore(begin)) {
            begin = callingTime;
            return;
        }
        if (callingTime.isAfter(end)) {
            end = callingTime;
        }
    }

    public boolean anyOverlap(TimeRange other) {
        return contains(other.begin) || contains(other.end) || other.contains(begin) || other.contains(end);
    }

    public boolean intoNextDay() {
        return end.isNextDay();
    }

    public TimeRange forFollowingDay() {
        if (!end.isNextDay()) {
            throw new RuntimeException("Does not contain a next day element: " + this);
        }
        TramTime endTime = TramTime.of(end.getHourOfDay(), end.getMinuteOfHour());
        return TimeRange.of(TramTime.of(0,0), endTime);
    }
}
