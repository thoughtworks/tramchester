package com.tramchester.graph;

import com.tramchester.domain.exceptions.TramchesterException;

public interface ElapsedTime {
    int getElapsedTime() throws TramchesterException;
    boolean startNotSet();
    void setJourneyStart(int minutesPastMidnight);
}
