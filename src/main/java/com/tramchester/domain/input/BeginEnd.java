package com.tramchester.domain.input;


public class BeginEnd {
    private final Stop begin;
    private final Stop end;

    public BeginEnd(Stop begin, Stop end) {
        this.begin = begin;
        this.end = end;
    }

    public Stop begin() {
        return begin;
    }

    public Stop end() {
        return end;
    }
}
