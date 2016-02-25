package com.tramchester.domain.presentation;

import com.tramchester.domain.Location;
import com.tramchester.domain.TransportStage;
import com.tramchester.domain.exceptions.TramchesterException;

import java.time.LocalTime;

public interface PresentableStage extends TransportStage {
    String getSummary() throws TramchesterException;
    String getPrompt() throws TramchesterException;

    int getNumberOfServiceTimes();

    Location getLastStation();
    Location getFirstStation();

    LocalTime getFirstDepartureTime();
    LocalTime getExpectedArrivalTime();

    int getDuration();
}
