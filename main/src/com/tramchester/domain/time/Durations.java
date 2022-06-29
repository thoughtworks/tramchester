package com.tramchester.domain.time;

import java.time.Duration;

public class Durations {

    private static final ComparisonFunction GreaterThanOrEquals = result -> result>=0;
    private static final ComparisonFunction GreaterThan = result -> result>0;
    private static final ComparisonFunction LessThan = result -> result<0;

    private static boolean compare(ComparisonFunction comparisonFunction, Duration durationA, Duration durationB) {
        return comparisonFunction.compare(durationA.compareTo(durationB));
    }

    public static boolean greaterOrEquals(Duration durationA, Duration durationB) {
        return compare(GreaterThanOrEquals, durationA, durationB);
    }

    public static boolean greaterThan(Duration durationA, Duration durationB) {
        return compare(GreaterThan, durationA, durationB);
    }

    public static boolean lessThan(Duration durationA, Duration durationB) {
        return compare(LessThan, durationA, durationB);
    }

    private interface ComparisonFunction {

        boolean compare(int result);
    }


}
