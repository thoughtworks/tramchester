package com.tramchester.dataimport.data;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;

import java.time.LocalDate;

@SuppressWarnings("unused")
public class CalendarData extends ParsesDate {

    @JsonProperty("service_id")
    private String serviceId;
    private String monday;
    private String tuesday;
    private String wednesday;
    private String thursday;
    private String friday;
    private String saturday;
    private String sunday;

    private TramDate start;
    private TramDate end;

    public CalendarData() {
        // CSV deserialization
    }

    public IdFor<Service> getServiceId() {
        return StringIdFor.createId(serviceId, Service.class);
    }

    @JsonProperty("start_date")
    private void setStartDate(String text) {
        this.start = parseTramDate(text);
    }

    @JsonProperty("end_date")
    private void setEndDate(String text) {
        this.end = parseTramDate(text);
    }

    private boolean isFlagSet(String text) {
        return "1".equals(text);
    }

    public boolean isMonday() {
        return isFlagSet(monday);
    }

    public boolean isTuesday() {
        return isFlagSet(tuesday);
    }

    public boolean isWednesday() {
        return isFlagSet(wednesday);
    }

    public boolean isThursday() {
        return isFlagSet(thursday);
    }

    public boolean isFriday() {
        return isFlagSet(friday);
    }

    public boolean isSaturday() {
        return isFlagSet(saturday);
    }

    public boolean isSunday() {
        return isFlagSet(sunday);
    }

    public DateRange getDateRange() {
        return new DateRange(start, end);
    }


}
