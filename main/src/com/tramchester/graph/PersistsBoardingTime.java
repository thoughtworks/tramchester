package com.tramchester.graph;

import com.tramchester.domain.TramTime;


public interface PersistsBoardingTime {
    void save(TramTime boardingTime);
    boolean isPresent();
    TramTime get();
}
