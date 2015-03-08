package com.tramchester.domain;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.mappers.TimeJsonSerializer;
import com.tramchester.services.DateTimeService;
import org.joda.time.DateTime;
import org.joda.time.Minutes;

public class ServiceTime {
    private final DateTime departureTime;
    private final DateTime arrivalTime;
    private final String serviceId;
    private final String headSign;
    private final int fromMidnight;

    public ServiceTime(DateTime departureTime, DateTime arrivalTime, String serviceId, String headSign, int fromMidnight) {
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.serviceId = serviceId;
        this.headSign = headSign;
        this.fromMidnight = fromMidnight;
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public DateTime getDepartureTime() {
        return departureTime;
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public DateTime getArrivalTime() {
        return arrivalTime;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getHeadSign() {
        return headSign;
    }

    public int getDuration(){
        if (this.departureTime.isAfter(this.arrivalTime)){
            return Minutes.minutesBetween(this.departureTime, this.arrivalTime.plusDays(1)).getMinutes();
        }

        return Minutes.minutesBetween(this.departureTime, this.arrivalTime).getMinutes();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%s %s %s %s %s]",
                serviceId,
                headSign,
                DateTimeService.formatTime(departureTime),
                DateTimeService.formatTime(arrivalTime), fromMidnight));
        return sb.toString();
    }

    public int getFromMidnight() {
        return fromMidnight;
    }
}

