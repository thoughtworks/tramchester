package com.tramchester.livedata;

import com.tramchester.domain.StationPair;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.liveUpdates.DueTram;

import java.util.Set;

public class TramPosition {
    private final StationPair stationPair;
    private final Set<DueTram> trams;
    private final int cost;

    public TramPosition(StationPair stationPair, Set<DueTram> trams, int cost) {
        this.stationPair = stationPair;
        this.trams = trams;
        this.cost = cost;
    }

    public Station getFirst() {
        return stationPair.getBegin();
    }

    public Station getSecond() {
        return stationPair.getEnd();
    }

    public Set<DueTram> getTrams() {
        return trams;
    }

    public int getCost() {
        return cost;
    }


}
