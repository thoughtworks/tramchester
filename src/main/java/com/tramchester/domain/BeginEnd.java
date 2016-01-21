package com.tramchester.domain;


public class BeginEnd {
    private Stop begin;
    private Stop end;

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
