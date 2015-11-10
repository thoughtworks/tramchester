package com.tramchester.domain;

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

    public ServiceTime(LocalTime departureTime, LocalTime arrivalTime, String serviceId, String headSign, int fromMidnight) {
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.serviceId = serviceId;
        this.headSign = headSign;
        this.fromMidnight = fromMidnight;
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

//    public String getHeadSign() {
//        return headSign;
//    }
//
//    public int getDuration(){
//        if (this.departureTime.isAfter(this.arrivalTime)){
//            return Minutes.minutesBetween(this.departureTime, this.arrivalTime.plusDays(1)).getMinutes();
//        }
//
//        return Minutes.minutesBetween(departureTime, arrivalTime).getMinutes();
//    }

    public int getFromMidnight() {
        return fromMidnight;
    }

    @Override
    public String toString() {
        return "ServiceTime{" +
                "departureTime=" + DateTimeService.formatTime(departureTime) +
                ", arrivalTime=" + DateTimeService.formatTime(arrivalTime) +
                ", serviceId='" + serviceId + '\'' +
                ", headSign='" + headSign + '\'' +
                ", fromMidnight=" + fromMidnight +
                '}';
    }
}

