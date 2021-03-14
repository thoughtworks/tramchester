package com.tramchester.domain;


import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.graph.GraphPropertyKey;

import java.io.PrintStream;
import java.util.Objects;

public class Service implements HasId<Service>, GraphProperty {

    private final IdFor<Service> serviceId;
    private ServiceCalendar calendar;

    public Service(String serviceId) {
        this(StringIdFor.createId(serviceId));
    }

    public Service(IdFor<Service> serviceId) {
        this.serviceId = serviceId;
        calendar = null;
    }

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
        Service service = (Service) o;
        return serviceId.equals(service.serviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId);
    }

    public void summariseDates(PrintStream printStream) {
        calendar.summariseDates(printStream);
    }

    @Override
    public GraphPropertyKey getProp() {
        return GraphPropertyKey.SERVICE_ID;
    }

    public void setCalendar(ServiceCalendar serviceCalendar) {
        if (this.calendar!=null) {
            throw new RuntimeException("Attempt to overwrite calendar for service " + this.serviceId + " overwrite was " + serviceCalendar);
        }
        this.calendar = serviceCalendar;
    }

    public ServiceCalendar getCalendar() {
        return calendar;
    }

    public boolean hasCalendar() {
        return calendar!=null;
    }
}
