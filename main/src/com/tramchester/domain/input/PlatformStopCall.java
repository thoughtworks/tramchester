package com.tramchester.domain.input;

import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.time.TramTime;

public class PlatformStopCall extends StopCall {
    private final Platform callingPlatform;

    public PlatformStopCall(Trip trip, Platform platform, Station station, StopTimeData stopTimeData) {
        super(station, stopTimeData, trip);
        this.callingPlatform = platform;
    }

    public PlatformStopCall(Platform platform, Station station, TramTime arrivalTime, TramTime departureTime, int sequenceNumber,
                               GTFSPickupDropoffType pickupType, GTFSPickupDropoffType dropoffType, Trip trip) {
        super(station, arrivalTime, departureTime, sequenceNumber, pickupType, dropoffType, trip);
        this.callingPlatform = platform;
    }

    @Override
    public Platform getPlatform() {
        return callingPlatform;
    }

    @Override
    public String toString() {
        return "PlatformStopCall{" +
                "callingPlatform=" + callingPlatform.getId() +
                "} " + super.toString();
    }

    @Override
    public boolean hasPlatfrom() {
        return true;
    }
}
