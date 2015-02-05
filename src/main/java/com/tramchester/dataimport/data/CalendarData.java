package com.tramchester.dataimport.data;

public class CalendarData {
    private final String serviceId;
    private final boolean monday;
    private final boolean tuesday;
    private final boolean wednesday;
    private final boolean thursday;
    private final boolean friday;
    private final boolean saturday;
    private final boolean sunday;

    public CalendarData(String serviceId, boolean monday, boolean tuesday, boolean wednesday, boolean thursday, boolean friday, boolean saturday, boolean sunday) {

        this.serviceId = serviceId;
        this.monday = monday;
        this.tuesday = tuesday;
        this.wednesday = wednesday;
        this.thursday = thursday;
        this.friday = friday;
        this.saturday = saturday;
        this.sunday = sunday;
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
}
