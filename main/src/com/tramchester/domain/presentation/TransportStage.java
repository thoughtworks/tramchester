package com.tramchester.domain.presentation;

import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;

import java.util.List;

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

    /***
     * Stops passed, might not stop here, see getCallingPoints() for vehicles
     */
    int getPassedStopsCount();

    /***
     * Stops where actually vehicle actually stops
     * @return
     */
    List<StopCall> getCallingPoints();

    IdFor<Trip> getTripId();
}
