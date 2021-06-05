package com.tramchester.domain;

import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;

import java.util.Set;
import java.util.stream.Stream;

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

    public static Stream<StationPair> combinationsOf(Set<Station> starts, Set<Station> ends) {
        return starts.stream().
                flatMap(start -> ends.stream().
                        filter(end -> !end.equals(start)).map(end -> of(start,end))
                );
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
        return begin.getTransportModes().contains(mode) && end.getTransportModes().contains(mode);
    }
}
