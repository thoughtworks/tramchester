package com.tramchester.livedata.tfgm;

import com.tramchester.domain.StationPair;
import com.tramchester.domain.places.Station;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;

import java.time.Duration;
import java.util.Set;

public class TramPosition {
    private final StationPair stationPair;
    private final Set<UpcomingDeparture> trams;
    private final Duration cost;

    public TramPosition(StationPair stationPair, Set<UpcomingDeparture> trams, Duration cost) {
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

    public Set<UpcomingDeparture> getTrams() {
        return trams;
    }

    public Duration getCost() {
        return cost;
    }


}
