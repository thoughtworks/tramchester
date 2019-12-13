package com.tramchester.graph;

import com.tramchester.domain.TramTime;
import com.tramchester.domain.exceptions.TramchesterException;

import java.time.LocalTime;

public interface ElapsedTime {
    TramTime getElapsedTime() throws TramchesterException;
    boolean startNotSet();
    void setJourneyStart(TramTime startTime) throws TramchesterException;
}
