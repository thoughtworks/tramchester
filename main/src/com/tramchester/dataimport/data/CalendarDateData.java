package com.tramchester.dataimport.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.Service;

import java.time.LocalDate;

// holds exceptions to main calendar
public class CalendarDateData extends ParsesDate {

    // TODO into Enum
    // https://developers.google.com/transit/gtfs/reference#calendar_datestxt
    public static final int ADDED = 1;
    public static final int REMOVED = 2;

    @JsonProperty("service_id")
    private String serviceId;
    private LocalDate date;

    @JsonProperty("exception_type")
    private int exceptionType;

    public CalendarDateData() {
        // deserialization
    }

    @JsonProperty("date")
    private void setDate(String text) {
        date = parseDate(text);
    }

    public StringIdFor<Service> getServiceId() {
        return StringIdFor.createId(serviceId);
    }

    public LocalDate getDate() {
        return date;
    }

    public int getExceptionType() {
        return exceptionType;
    }


    @Override
    public String toString() {
        return "CalendarDateData{" +
                "serviceId='" + serviceId + '\'' +
                ", date=" + date +
                ", exceptionType=" + exceptionType +
                "} ";
    }
}
