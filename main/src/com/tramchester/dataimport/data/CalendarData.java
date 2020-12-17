package com.tramchester.dataimport.data;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Service;

import java.time.LocalDate;

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

    private LocalDate start;
    private LocalDate end;

    public CalendarData() {
        // CSV deserialization
    }

    public IdFor<Service> getServiceId() {
        return IdFor.createId(serviceId);
    }

    @JsonProperty("start_date")
    private void setStartDate(String text) {
        this.start = parseDate(text);
    }

    @JsonProperty("end_date")
    private void setEndDate(String text) {
        this.end = parseDate(text);
    }

    public boolean isMonday() {
        return isSet(monday);
    }

    private boolean isSet(String text) {
        return "1".equals(text);
    }

    public boolean isTuesday() {
        return isSet(tuesday);
    }

    public boolean isWednesday() {
        return isSet(wednesday);
    }

    public boolean isThursday() {
        return isSet(thursday);
    }

    public boolean isFriday() {
        return isSet(friday);
    }

    public boolean isSaturday() {
        return isSet(saturday);
    }

    public boolean isSunday() {
        return isSet(sunday);
    }

    public LocalDate getEndDate() {
        return end;
    }

    public LocalDate getStartDate() {
        return start;
    }


}
