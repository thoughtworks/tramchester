package com.tramchester.domain.presentation;

import com.tramchester.domain.*;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;

public interface TransportStage<FROM extends Location<?>, DEST extends Location<?>> {
    String getHeadSign();
    Route getRoute();

    Location<?> getActionStation(); // place where action happens, i.e. Board At X or Walk To X
    FROM getFirstStation();
    DEST getLastStation();

    TramTime getFirstDepartureTime();
    TramTime getExpectedArrivalTime();

    int getDuration();

    Platform getBoardingPlatform();

    boolean hasBoardingPlatform();

    TransportMode getMode();
    int getPassedStops();

}
