package com.tramchester.graph;

import com.tramchester.domain.exceptions.TramchesterException;

import java.time.LocalTime;

public interface ElapsedTime {
    LocalTime getElapsedTime() throws TramchesterException;
    boolean startNotSet();
    void setJourneyStart(LocalTime startTime) throws TramchesterException;
}
