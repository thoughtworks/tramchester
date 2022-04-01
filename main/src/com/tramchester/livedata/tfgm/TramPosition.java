package com.tramchester.livedata.tfgm;

import com.tramchester.domain.StationPair;
import com.tramchester.domain.places.Station;
import com.tramchester.livedata.domain.liveUpdates.DueTram;

import java.time.Duration;
import java.util.Set;

public class TramPosition {
    private final StationPair stationPair;
    private final Set<DueTram> trams;
    private final Duration cost;

    public TramPosition(StationPair stationPair, Set<DueTram> trams, Duration cost) {
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

    public Duration getCost() {
        return cost;
    }


}
