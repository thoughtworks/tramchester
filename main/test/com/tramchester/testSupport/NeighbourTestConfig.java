package com.tramchester.testSupport;

import com.tramchester.config.NeighbourConfig;

public class NeighbourTestConfig implements NeighbourConfig {

    private final double distance;
    private final int maxNeighbours;

    public NeighbourTestConfig(double distance, int maxNeighbours) {
        this.distance = distance;
        this.maxNeighbours = maxNeighbours;
    }

    @Override
    public double getDistanceToNeighboursKM() {
        return distance;
    }

    @Override
    public int getMaxNeighbourConnections() {
        return maxNeighbours;
    }

}
