package com.tramchester.domain;

public class NumberOfChanges {
    private final int min;
    private final int max;

    public NumberOfChanges(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public int getMax() {
        return max;
    }

    public int getMin() {
        return min;
    }

    @Override
    public String toString() {
        return "NumberOfChanges{" +
                "min=" + min +
                ", max=" + max +
                '}';
    }
}
