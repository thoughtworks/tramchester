package com.tramchester.domain;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;

import java.util.Set;

public class StationLink {
    private final StationPair pair;
    private final Set<TransportMode> modes;

    public StationLink(Station begin, Station end, Set<TransportMode> modes) {
        this.pair = StationPair.of(begin, end);
        this.modes = modes;
    }

    public Station getBegin() {
        return pair.getBegin();
    }

    public Station getEnd() {
        return pair.getEnd();
    }

    @Override
    public String toString() {
        return "StationLink{" +
                "pair=" + pair +
                ", modes=" + modes +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StationLink that = (StationLink) o;

        if (!pair.equals(that.pair)) return false;
        return modes.equals(that.modes);
    }

    @Override
    public int hashCode() {
        int result = pair.hashCode();
        result = 31 * result + modes.hashCode();
        return result;
    }

    /***
     * The transport modes that link these two stations
     * NOT the modes of the stations themselves which might be subset of linking modes
     * @return The transport modes that link these two stations i.e. Walk
     */
    public Set<TransportMode> getLinkingModes() {
        return modes;
    }

    public boolean hasValidLatlongs() {
        return pair.getBegin().getLatLong().isValid() && pair.getEnd().getLatLong().isValid();
    }

}
