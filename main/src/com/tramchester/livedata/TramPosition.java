package com.tramchester.livedata;

import com.tramchester.domain.Station;
import com.tramchester.domain.liveUpdates.DueTram;

import java.util.Set;

public class TramPosition {
    private Station first;
    private Station second;
    private Set<DueTram> trams;
    private final int cost;

    public TramPosition(Station first, Station second, Set<DueTram> trams, int cost) {
        this.first = first;
        this.second = second;
        this.trams = trams;
        this.cost = cost;
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

    public int getCost() {
        return cost;
    }
}
