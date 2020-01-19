package com.tramchester.domain.presentation;

import com.tramchester.domain.*;

import java.util.Optional;

public interface TransportStage {
    String getHeadSign();
    String getRouteName();

    Location getActionStation(); // place where action happens, i.e. Board At X or Walk To X
    Location getLastStation();
    Location getFirstStation();

    TramTime getFirstDepartureTime();
    TramTime getExpectedArrivalTime();

    int getDuration();
    String getDisplayClass();

    Optional<Platform> getBoardingPlatform();

    TransportMode getMode();
    int getPassedStops();

}
