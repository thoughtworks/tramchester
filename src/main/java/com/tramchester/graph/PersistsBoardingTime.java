package com.tramchester.graph;

import java.time.LocalTime;

public interface PersistsBoardingTime {
    void save(LocalTime boardingTime);
    boolean isPresent();
    LocalTime get();
}
