package com.tramchester.domain.input;

import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.time.TramTime;

public class RailPlatformStopCall extends StopCall {
    private final Platform callingPlatform;

    public RailPlatformStopCall(Station station, TramTime arrivalTime, TramTime departureTime, int sequenceNumber,
                                GTFSPickupDropoffType pickupType, GTFSPickupDropoffType dropoffType, Trip trip, Platform callingPlatform) {
        super(station, arrivalTime, departureTime, sequenceNumber, pickupType, dropoffType, trip);
        this.callingPlatform = callingPlatform;
    }

    @Override
    public Platform getPlatform() {
        return callingPlatform;
    }

    @Override
    public boolean hasPlatfrom() {
        return true;
    }
}
