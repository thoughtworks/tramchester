package com.tramchester.domain.presentation;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.tramchester.domain.Location;
import com.tramchester.domain.TransportStage;
import com.tramchester.domain.exceptions.TramchesterException;
import org.joda.time.LocalTime;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="class")
public interface PresentableStage extends TransportStage {
    String getSummary() throws TramchesterException;
    String getPrompt() throws TramchesterException;
    String getHeadSign();

    Location getActionStation(); // place where action happens, i.e. Board At X or Walk To X
    Location getLastStation();
    Location getFirstStation();

    LocalTime getFirstDepartureTime();
    LocalTime getExpectedArrivalTime();

    int getDuration();
}
