package com.tramchester.domain;

import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;

public class StationPair {
    private final Station begin;
    private final Station end;

    private StationPair(Station begin, Station end) {
        this.begin = begin;
        this.end = end;
    }

    public static StationPair of(Station begin, Station end) {
        return new StationPair(begin, end);
    }

    public static StationPair of(StopCalls.StopLeg leg) {
        return new StationPair(leg.getFirstStation(), leg.getSecondStation());
    }

    @Override
    public String toString() {
        return "StationPair{" +
                "begin=" + begin +
                ", end=" + end +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StationPair that = (StationPair) o;

        if (!begin.equals(that.begin)) return false;
        return end.equals(that.end);
    }

    @Override
    public int hashCode() {
        int result = begin.hashCode();
        result = 31 * result + end.hashCode();
        return result;
    }

    public Station getBegin() {
        return begin;
    }

    public Station getEnd() {
        return end;
    }

    public boolean both(TransportMode mode) {
        return begin.serves(mode) && end.serves(mode);
    }
}
