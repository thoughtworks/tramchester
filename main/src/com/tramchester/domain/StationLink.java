package com.tramchester.domain;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;

import java.util.Set;

public class StationLink {
    private final Station begin;
    private final Station end;
    private final Set<TransportMode> modes;

    public StationLink(Station begin, Station end, Set<TransportMode> modes) {
        this.begin = begin;
        this.end = end;
        this.modes = modes;
    }

    public Station getBegin() {
        return begin;
    }

    public Station getEnd() {
        return end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StationLink that = (StationLink) o;

        if (!begin.equals(that.begin)) return false;
        if (!end.equals(that.end)) return false;
        return modes.equals(that.modes);
    }

    @Override
    public int hashCode() {
        int result = begin.hashCode();
        result = 31 * result + end.hashCode();
        result = 31 * result + modes.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "StationLink{" +
                "begin=" + begin +
                ", end=" + end +
                ", modes=" + modes +
                '}';
    }

    public Set<TransportMode> getModes() {
        return modes;
    }
}
