package com.tramchester.domain.presentation;

import com.tramchester.domain.*;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.time.TramTime;

import java.util.Optional;

public interface TransportStage<FROM extends Location<?>, DEST extends Location<?>> {
    String getHeadSign();
    String getRouteName();
    String getRouteShortName();

    Location<?> getActionStation(); // place where action happens, i.e. Board At X or Walk To X
    FROM getFirstStation();
    DEST getLastStation();

    TramTime getFirstDepartureTime();
    TramTime getExpectedArrivalTime();

    int getDuration();

    Optional<Platform> getBoardingPlatform();

    TransportMode getMode();
    int getPassedStops();

}
