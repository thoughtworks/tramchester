package com.tramchester.graph;

import com.tramchester.domain.TramchesterException;

public interface ElapsedTime {
    int getElapsedTime() throws TramchesterException;
    boolean startNotSet();
    void setJourneyStart(int minutesPastMidnight);
}
