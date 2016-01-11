package com.tramchester.domain.presentation;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.mappers.TimeJsonSerializer;
import com.tramchester.services.DateTimeService;

import java.time.LocalTime;

public class ServiceTime {
    private final LocalTime departureTime;
    private final LocalTime arrivalTime;
    private final String serviceId;
    private final String headSign;
    private final int fromMidnight;
    private final String tripId;

    public ServiceTime(LocalTime departureTime, LocalTime arrivalTime, String serviceId, String headSign, int fromMidnight, String tripId) {
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.serviceId = serviceId;
        this.headSign = headSign;
        this.fromMidnight = fromMidnight;
        this.tripId = tripId;
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getDepartureTime() {
        return departureTime;
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public String getServiceId() {
        return serviceId;
    }

    public int getFromMidnight() {
        return fromMidnight;
    }

    @Override
    public String toString() {
        return "ServiceTime{" +
                "departureTime=" + DateTimeService.formatTime(departureTime) +
                ", arrivalTime=" + DateTimeService.formatTime(arrivalTime) +
                ", serviceId='" + serviceId + '\'' +
                ", tripId='" + tripId + '\'' +
                ", headSign='" + headSign + '\'' +
                ", fromMidnight=" + fromMidnight +
                '}';
    }
}

