package com.tramchester.config;

import com.tramchester.domain.StationIdPair;

import java.util.List;

public interface NeighbourConfig {

    // distance for neighbouring stations, in KM
    double getDistanceToNeighboursKM() ;

    // number of direct walks between stations
    int getMaxNeighbourConnections();

    // station pairs to add explicity as neighbours
    List<StationIdPair> getAdditional();

}
