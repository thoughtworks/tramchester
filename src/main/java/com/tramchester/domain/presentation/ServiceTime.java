package com.tramchester.domain.presentation;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.TramTime;
import com.tramchester.mappers.TramTimeJsonSerializer;

public class ServiceTime implements Comparable<ServiceTime> {
    private final TramTime leaveBegin;
    private final TramTime arrivesEnd;
    private final String serviceId;
    private final String headSign;
    private final String tripId;

    public ServiceTime(TramTime leaveBegin, TramTime arrivesEnd, String serviceId, String headSign, String tripId) {
        this.leaveBegin = leaveBegin;
        this.arrivesEnd = arrivesEnd;
        this.serviceId = serviceId;
        this.headSign = headSign;
        this.tripId = tripId;
    }

    @JsonSerialize(using = TramTimeJsonSerializer.class)
    public TramTime getDepartureTime() {
        return leaveBegin;
    }

    @JsonSerialize(using = TramTimeJsonSerializer.class)
    public TramTime getArrivalTime() {
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
        return leaveBegin.minutesOfDay();
    }

    @Override
    public String toString() {
        return "ServiceTime{" +
                "(leaves start) departureTime=" + leaveBegin.toPattern() +
                ",(arrives end) arrivalTime=" + arrivesEnd.toPattern() +
                ", serviceId='" + serviceId + '\'' +
                ", tripId='" + tripId + '\'' +
                ", headSign='" + headSign + '\'' +
                '}';
    }

    // for sorting of results
    @Override
    public int compareTo(ServiceTime other) {
        return arrivesEnd.compareTo(other.arrivesEnd);
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

