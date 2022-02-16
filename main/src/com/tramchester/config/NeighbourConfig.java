package com.tramchester.config;

public interface NeighbourConfig {

    // distance for neighbouring stations, in KM
    double getDistanceToNeighboursKM() ;

    // number of direct walks between stations
    public abstract int getMaxNeighbourConnections();

}
