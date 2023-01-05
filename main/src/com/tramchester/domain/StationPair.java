package com.tramchester.domain;

import com.tramchester.domain.collections.DomainPair;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;

public class StationPair extends DomainPair<Station> {

    private StationPair(Station first, Station second) {
        super(first, second);
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
                super.toString() +
                '}';
    }

    public Station getBegin() {
        return first();
    }

    public Station getEnd() {
        return second();
    }

    public boolean bothServeMode(TransportMode mode) {
        return first().servesMode(mode) && second().servesMode(mode);
    }
}
