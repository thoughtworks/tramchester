package com.tramchester.graph.search;

import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.exceptions.TramchesterException;

public interface ElapsedTime {
    TramTime getElapsedTime() throws TramchesterException;
    boolean startNotSet();
    void setJourneyStart(TramTime startTime) throws TramchesterException;
}
