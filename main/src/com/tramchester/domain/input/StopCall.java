package com.tramchester.domain.input;

import com.tramchester.domain.Platform;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;

public abstract class StopCall {
    protected final Station station;
    private final Trip trip;
    private final int sequenceNumber;

    private final TramTime arrivalTime;
    private final GTFSPickupDropoffType pickupType;
    private final GTFSPickupDropoffType dropoffType;
    private final int dwellTime;
    private final boolean intoNextDay;

    protected StopCall(Station station, TramTime arrivalTime, TramTime departureTime, int sequenceNumber, GTFSPickupDropoffType pickupType,
                       GTFSPickupDropoffType dropoffType, Trip trip) {
        this.station = station;
        this.arrivalTime = arrivalTime;
        this.sequenceNumber = sequenceNumber;
        this.pickupType = pickupType;
        this.dropoffType = dropoffType;
        this.trip = trip;

        // small optimisations
        if (arrivalTime.equals(departureTime)) {
            dwellTime = 0;
        } else {
            dwellTime = TramTime.diffenceAsMinutes(arrivalTime, departureTime);
        }
        intoNextDay = arrivalTime.isNextDay() || departureTime.isNextDay();
    }

    public TramTime getArrivalTime() {
        return arrivalTime;
    }

    public TramTime getDepartureTime() {
        if (dwellTime==0) {
            return arrivalTime;
        }
        return arrivalTime.plusMinutes(dwellTime);
    }

    public Station getStation() {
        return station;
    }

    public IdFor<Station> getStationId() {
        return station.getId();
    }

    public int getGetSequenceNumber() {
        return sequenceNumber;
    }

    public abstract Platform getPlatform();

    public GTFSPickupDropoffType getPickupType() {
        return pickupType;
    }

    public GTFSPickupDropoffType getDropoffType() {
        return dropoffType;
    }

    @Override
    public String toString() {
        return "StopCall{" +
                "station=" + HasId.asId(station) +
                ", arrivalTime=" + arrivalTime +
                ", dwellTime=" + dwellTime +
                ", sequenceNumber=" + sequenceNumber +
                ", pickupType=" + pickupType +
                ", dropoffType=" + dropoffType +
                ", trip=" + HasId.asId(trip) +
                '}';
    }

    public abstract boolean hasPlatfrom();

    public boolean callsAtStation() {
        return dropoffType.isDropOff() || pickupType.isPickup();
    }

    public Trip getTrip() {
        return trip;
    }

    public boolean intoNextDay() {
        return intoNextDay;
    }

    public Service getService() {
        return trip.getService();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StopCall stopCall = (StopCall) o;

        if (sequenceNumber != stopCall.sequenceNumber) return false;
        if (!station.equals(stopCall.station)) return false;
        return trip.equals(stopCall.trip);
    }

    @Override
    public int hashCode() {
        int result = station.hashCode();
        result = 31 * result + trip.hashCode();
        result = 31 * result + sequenceNumber;
        return result;
    }

    public TransportMode getTransportMode() {
        return trip.getTransportMode();
    }

    public boolean same(StopCall stopCall) {
        if (sequenceNumber != stopCall.sequenceNumber) return false;
        if (dwellTime!=stopCall.dwellTime) return false;
        if (pickupType!=stopCall.pickupType) return false;
        if (dropoffType!=stopCall.dropoffType) return false;
        if (intoNextDay!=stopCall.intoNextDay) return false;

        if (!station.equals(stopCall.station)) return false;
        if (!trip.equals(stopCall.trip)) return false;
        if (!arrivalTime.equals(stopCall.arrivalTime)) return false;

        return true;
    }
}
