package com.tramchester.testSupport;

import com.tramchester.config.NeighbourConfig;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;

import java.util.ArrayList;
import java.util.List;

public class NeighbourTestConfig implements NeighbourConfig {

    private final double distance;
    private final int maxNeighbours;
    private final List<StationIdPair> additional;

    public NeighbourTestConfig(double distance, int maxNeighbours) {
        this.distance = distance;
        this.maxNeighbours = maxNeighbours;
        additional = new ArrayList<>();
    }

    @Override
    public double getDistanceToNeighboursKM() {
        return distance;
    }

    @Override
    public int getMaxNeighbourConnections() {
        return maxNeighbours;
    }

    @Override
    public List<StationIdPair> getAdditional() {
        return additional;
    }

    public void addNeighbours(IdFor<Station> first, IdFor<Station> second) {
        additional.add(StationIdPair.of(first, second));

    }
}
