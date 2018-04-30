package com.tramchester.dataimport.data;

import org.joda.time.LocalDate;

public class CalendarData {
    private final String serviceId;
    private final boolean monday;
    private final boolean tuesday;
    private final boolean wednesday;
    private final boolean thursday;
    private final boolean friday;
    private final boolean saturday;
    private final boolean sunday;
    private final LocalDate start;
    private final LocalDate end;

    public CalendarData(String serviceId, boolean monday, boolean tuesday, boolean wednesday, boolean thursday,
                        boolean friday, boolean saturday, boolean sunday, LocalDate start, LocalDate end) {

        this.serviceId = serviceId.intern();
        this.monday = monday;
        this.tuesday = tuesday;
        this.wednesday = wednesday;
        this.thursday = thursday;
        this.friday = friday;
        this.saturday = saturday;
        this.sunday = sunday;
        this.start = start;
        this.end = end;
    }

    public String getServiceId() {
        return serviceId;
    }

    public boolean isMonday() {
        return monday;
    }

    public boolean isTuesday() {
        return tuesday;
    }

    public boolean isWednesday() {
        return wednesday;
    }

    public boolean isThursday() {
        return thursday;
    }

    public boolean isFriday() {
        return friday;
    }

    public boolean isSaturday() {
        return saturday;
    }

    public boolean isSunday() {
        return sunday;
    }

    public LocalDate getEndDate() {
        return end;
    }

    public LocalDate getStartDate() {
        return start;
    }

}
