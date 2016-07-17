package com.tramchester.dataimport.data;

import com.amazonaws.services.devicefarm.model.Run;
import com.tramchester.domain.exceptions.TramchesterException;

import java.time.LocalTime;
import java.util.Optional;

public class StopTimeData {
    private String tripId;
    private Optional<LocalTime> arrivalTime = Optional.empty();
    private Optional<LocalTime> departureTime = Optional.empty();
    private String stopId;
    private String stopSequence;
    private String pickupType;
    private String dropOffType;

    public StopTimeData(String tripId, Optional<LocalTime> arrivalTime, Optional<LocalTime> departureTime, String stopId,
                        String stopSequence, String pickupType, String dropOffType) {
        if (arrivalTime==null || departureTime==null) {
            throw new RuntimeException("Constrain violation");
        }
        this.tripId = tripId;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.stopId = stopId;
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

    public LocalTime getArrivalTime() {
        return arrivalTime.orElse(null);
    }

    public LocalTime getDepartureTime() {
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
