package com.tramchester.dataimport.data;


import com.tramchester.domain.HasId;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Service;

import java.time.LocalDate;

public class CalendarData {
    private final IdFor<Service> serviceId;
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

        this.serviceId = IdFor.createId(serviceId);
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

    public IdFor<Service> getServiceId() {
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
