package com.tramchester.domain.presentation;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.TimeAsMinutes;
import com.tramchester.mappers.TimeJsonSerializer;
import com.tramchester.services.DateTimeService;

import java.time.LocalTime;

public class ServiceTime extends TimeAsMinutes implements Comparable<ServiceTime> {
    private final LocalTime leaveBegin;
    private final LocalTime arrivesEnd;
    private final String serviceId;
    private final String headSign;
    private final String tripId;

    public ServiceTime(LocalTime leaveBegin, LocalTime arrivesEnd, String serviceId, String headSign, String tripId) {
        this.leaveBegin = leaveBegin;
        this.arrivesEnd = arrivesEnd;
        this.serviceId = serviceId;
        this.headSign = headSign;
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

    // used on front end
    public String getHeadSign() {
        return headSign;
    }

    public String getServiceId() {
        return serviceId;
    }

    public int getFromMidnightLeaves() {
        return getMinutes(leaveBegin);
    }

    public int getFromMidnightArrives() {
        return getMinutes(arrivesEnd);
    }

    @Override
    public String toString() {
        return "ServiceTime{" +
                "(leaves start) departureTime=" + DateTimeService.formatTime(leaveBegin) +
                ",(arrives end) arrivalTime=" + DateTimeService.formatTime(arrivesEnd) +
                ", serviceId='" + serviceId + '\'' +
                ", tripId='" + tripId + '\'' +
                ", headSign='" + headSign + '\'' +
                '}';
    }

    @Override
    public int compareTo(ServiceTime other) {
        return TimeAsMinutes.compare(arrivesEnd,other.arrivesEnd);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceTime that = (ServiceTime) o;

        if (leaveBegin != null ? !leaveBegin.equals(that.leaveBegin) : that.leaveBegin != null) return false;
        if (arrivesEnd != null ? !arrivesEnd.equals(that.arrivesEnd) : that.arrivesEnd != null) return false;
        if (serviceId != null ? !serviceId.equals(that.serviceId) : that.serviceId != null) return false;
        return tripId != null ? tripId.equals(that.tripId) : that.tripId == null;

    }

    @Override
    public int hashCode() {
        int result = leaveBegin != null ? leaveBegin.hashCode() : 0;
        result = 31 * result + (arrivesEnd != null ? arrivesEnd.hashCode() : 0);
        result = 31 * result + (serviceId != null ? serviceId.hashCode() : 0);
        result = 31 * result + (tripId != null ? tripId.hashCode() : 0);
        return result;
    }
}

