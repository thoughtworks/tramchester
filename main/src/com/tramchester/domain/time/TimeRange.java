package com.tramchester.domain.time;

import java.time.Duration;

import static java.lang.String.format;

public class TimeRange {
    private final TramTime begin;
    private final TramTime end;

    public TimeRange(TramTime begin, TramTime end) {
        this.begin = begin;
        this.end = end;
        if (end.isBefore(begin)) {
            throw new RuntimeException(format("End time %s is before begin %s", end, begin));
        }
    }

    public static TimeRange of(TramTime time, Duration before, Duration after) {
        TramTime begin = time.minus(before);
        TramTime end = time.plus(after);
        return new TimeRange(begin, end);
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
}
