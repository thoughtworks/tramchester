package com.tramchester.integration.graph;

import com.tramchester.domain.exceptions.TramchesterException;

public interface ElapsedTime {
    int getElapsedTime() throws TramchesterException;
    boolean startNotSet();
    void setJourneyStart(int minutesPastMidnight) throws TramchesterException;
}
