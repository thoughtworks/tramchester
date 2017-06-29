package com.tramchester.integration.graph;

public interface PersistsBoardingTime {
    void save(int boardingTime);
    boolean isPresent();
    int get();
    void clear();
}
