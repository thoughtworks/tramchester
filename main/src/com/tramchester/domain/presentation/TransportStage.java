package com.tramchester.domain.presentation;

import com.tramchester.domain.Location;
import com.tramchester.domain.Platform;
import com.tramchester.domain.RawStage;
import com.tramchester.domain.TramTime;

import java.util.Optional;

public interface TransportStage extends RawStage {
    String getHeadSign();
    String getRouteName();

//    TravelAction getAction();
    Location getActionStation(); // place where action happens, i.e. Board At X or Walk To X

    Location getLastStation();
    Location getFirstStation();

    TramTime getFirstDepartureTime();
    TramTime getExpectedArrivalTime();

    int getDuration();
    String getDisplayClass();

    Optional<Platform> getBoardingPlatform();

}
