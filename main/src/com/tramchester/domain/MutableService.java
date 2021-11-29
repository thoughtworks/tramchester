package com.tramchester.domain;


import com.tramchester.domain.id.IdFor;
import com.tramchester.graph.GraphPropertyKey;

import java.io.PrintStream;
import java.util.Objects;

public class MutableService implements Service {

    private final IdFor<Service> serviceId;
    private MutableServiceCalendar calendar;

    public MutableService(IdFor<Service> serviceId) {
        this.serviceId = serviceId;
        calendar = null;
    }

    // test support
    public static Service build(IdFor<Service> serviceId) {
        return new MutableService(serviceId);
    }

    @Override
    public IdFor<Service> getId() {
        return serviceId;
    }

    @Override
    public String toString() {
        return "Service{" +
                "serviceId=" + serviceId +
                ", calendar=" + calendar +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MutableService service = (MutableService) o;
        return serviceId.equals(service.serviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId);
    }

    @Override
    public void summariseDates(PrintStream printStream) {
        calendar.summariseDates(printStream);
    }

    @Override
    public GraphPropertyKey getProp() {
        return GraphPropertyKey.SERVICE_ID;
    }

    public void setCalendar(MutableServiceCalendar serviceCalendar) {
        if (calendar!=null) {
            throw new RuntimeException("Attempt to overwrite calendar for service " + this.serviceId + " overwrite was " + serviceCalendar);
        }
        this.calendar = serviceCalendar;
    }

    @Override
    public ServiceCalendar getCalendar() {
        return calendar;
    }

    @Override
    public boolean hasCalendar() {
        return calendar!=null;
    }

    public MutableServiceCalendar getMutableCalendar() {
        return calendar;
    }

    public void markCancelled() {
        calendar.cancel();
    }
}
