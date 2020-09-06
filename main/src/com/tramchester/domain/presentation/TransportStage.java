package com.tramchester.domain.presentation;

import com.tramchester.domain.*;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;

import java.util.Optional;

public interface TransportStage {
    String getHeadSign();
    String getRouteName();
    String getRouteShortName();

    Location<Station> getActionStation(); // place where action happens, i.e. Board At X or Walk To X
    Location<Station> getLastStation();
    Location<Station> getFirstStation();

    TramTime getFirstDepartureTime();
    TramTime getExpectedArrivalTime();

    int getDuration();

    Optional<Platform> getBoardingPlatform();

    TransportMode getMode();
    int getPassedStops();

}
