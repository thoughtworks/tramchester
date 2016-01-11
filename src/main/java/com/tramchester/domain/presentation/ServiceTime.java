package com.tramchester.domain.presentation;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.mappers.TimeJsonSerializer;
import com.tramchester.services.DateTimeService;

import java.time.LocalTime;

public class ServiceTime {
    private final LocalTime leaveBegin;
    private final LocalTime arrivesEnd;
    private final String serviceId;
    private final String headSign;
    private final int fromMidnight;
    private final String tripId;

    public ServiceTime(LocalTime leaveBegin, LocalTime arrivesEnd, String serviceId, String headSign, int fromMidnight, String tripId) {
        this.leaveBegin = leaveBegin;
        this.arrivesEnd = arrivesEnd;
        this.serviceId = serviceId;
        this.headSign = headSign;
        this.fromMidnight = fromMidnight;
        this.tripId = tripId;
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getDepartureTime() {
        return leaveBegin;
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getArrivalTime() {
        return arrivesEnd;
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
                "(leaves start) departureTime=" + DateTimeService.formatTime(leaveBegin) +
                ",(arrives end) arrivalTime=" + DateTimeService.formatTime(arrivesEnd) +
                ", serviceId='" + serviceId + '\'' +
                ", tripId='" + tripId + '\'' +
                ", headSign='" + headSign + '\'' +
                ", fromMidnight=" + fromMidnight +
                '}';
    }
}

