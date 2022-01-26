package com.tramchester.domain.input;

import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.time.TramTime;

public class NoPlatformStopCall extends StopCall {

    public NoPlatformStopCall(Station station, TramTime arrivalTime, TramTime departureTime, int sequenceNumber,
                              GTFSPickupDropoffType pickupType, GTFSPickupDropoffType dropoffType, Trip trip) {
        super(station, arrivalTime, departureTime, sequenceNumber, pickupType, dropoffType, trip);
    }

    @Override
    public Platform getPlatform() {
        throw new RuntimeException(station + "  does not have platforms");
    }

    @Override
    public String toString() {
        return "NoPlatformStopCall{" +
                "} " + super.toString();
    }

    @Override
    public boolean hasPlatfrom() {
        return false;
    }
}
