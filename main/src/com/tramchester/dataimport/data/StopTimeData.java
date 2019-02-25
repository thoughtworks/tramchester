package com.tramchester.dataimport.data;

import com.tramchester.domain.TramTime;

import java.util.Optional;

public class StopTimeData {
    private final String tripId;
    private final Optional<TramTime> arrivalTime ;
    private final Optional<TramTime> departureTime;
    private final String stopId;
    private final String stopSequence;
    private final String pickupType;
    private final String dropOffType;

    public StopTimeData(String tripId, Optional<TramTime> arrivalTime, Optional<TramTime> departureTime, String stopId,
                        String stopSequence, String pickupType, String dropOffType) {
        if (arrivalTime==null || departureTime==null) {
            throw new RuntimeException("Constrain violation");
        }
        this.tripId = tripId.intern();
        this.stopId = stopId.intern();
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.stopSequence = stopSequence;
        this.pickupType = pickupType;
        this.dropOffType = dropOffType;
    }

    @Override
    public String toString() {
        return "StopTimeData{" +
                "tripId='" + tripId + '\'' +
                ", arrivalTime=" + arrivalTime +
                ", departureTime=" + departureTime +
                ", stopId='" + stopId + '\'' +
                ", stopSequence='" + stopSequence + '\'' +
                ", pickupType='" + pickupType + '\'' +
                ", dropOffType='" + dropOffType + '\'' +
                '}';
    }

    public String getTripId() {
        return tripId;
    }

    public TramTime getArrivalTime() {
        return arrivalTime.orElse(null);
    }

    public TramTime getDepartureTime() {
        return departureTime.orElse(null);
    }

    public String getStopId() {
        return stopId;
    }

    public String getStopSequence() {
        return stopSequence;
    }

    public String getPickupType() {
        return pickupType;
    }

    public String getDropOffType() {
        return dropOffType;
    }

    public boolean isInError() {
        if (!arrivalTime.isPresent()) {
            return true;
        }
        if (!departureTime.isPresent()) {
            return true;
        }
        return false;
    }
}
