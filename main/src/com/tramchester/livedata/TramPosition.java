package com.tramchester.livedata;

import com.tramchester.domain.Station;
import com.tramchester.domain.liveUpdates.DueTram;

import java.util.Set;

public class TramPosition {
    private final Station first;
    private final Station second;
    private Set<DueTram> trams;

    public TramPosition(Station first, Station second, Set<DueTram> trams) {
        this.first = first;
        this.second = second;
        this.trams = trams;
    }

    public Station getFirst() {
        return first;
    }

    public Station getSecond() {
        return second;
    }

    public Set<DueTram> getTrams() {
        return trams;
    }
}
