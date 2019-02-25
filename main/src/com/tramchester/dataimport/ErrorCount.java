package com.tramchester.dataimport;

public class ErrorCount {
    private int count = 0;

    public boolean noErrors() {
        return count == 0;
    }

    @Override
    public String toString() {
        return "ErrorCount{" +
                "count=" + count +
                '}';
    }

    public void inc() {
        count++;
    }
}
