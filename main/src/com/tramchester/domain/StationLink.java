package com.tramchester.domain;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.mappers.Geography;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import java.time.Duration;
import java.util.Set;

public class StationLink {
    private final StationPair pair;
    private final Set<TransportMode> modes;
    private final Quantity<Length> distanceBetweenInMeters;
    private final Duration walkingTime;

    public StationLink(Station begin, Station end, Set<TransportMode> modes, Quantity<Length> distanceBetweenInMeters,
                       Duration walkingTime) {
        this.distanceBetweenInMeters = distanceBetweenInMeters;
        this.walkingTime = walkingTime;
        this.pair = StationPair.of(begin, end);
        this.modes = modes;
    }

    public static StationLink create(Station begin, Station end, Set<TransportMode> modes, Geography geography) {
        Quantity<Length> distance = geography.getDistanceBetweenInMeters(begin, end);
        final Duration walkingDuration = geography.getWalkingDuration(begin, end);
        return new StationLink(begin, end, modes, distance, walkingDuration);
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
                ", distanceBetweenInMeters=" + distanceBetweenInMeters +
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

    public Quantity<Length> getDistanceInMeters() {
        return distanceBetweenInMeters;
    }

    public Duration getWalkingTime() {
        return walkingTime;
    }
}
