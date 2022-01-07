package com.tramchester.domain;


import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class MutableService implements Service {

    private final IdFor<Service> serviceId;
    private final Set<Trip> trips;
    private MutableServiceCalendar calendar;
    private TramTime startTime;
    private TramTime finishTime;

    public MutableService(IdFor<Service> serviceId) {
        this.serviceId = serviceId;
        calendar = null;
        startTime = null;
        finishTime = null;
        trips = new HashSet<>();
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

    public void addTrip(Trip trip) {
        trips.add(trip);
    }

    private void computeStartTime() {
        Optional<TramTime> firstDepartForTrips = trips.stream().map(Trip::departTime).min(TramTime::compareTo);
        if (firstDepartForTrips.isEmpty()) {
            throw new RuntimeException("Missing first depart for " + trips);
        }
        startTime = firstDepartForTrips.get();
    }


    private void computeFinishTime() {
        Optional<TramTime> finalArrivalForTrips = trips.stream().map(Trip::arrivalTime).max(TramTime::compareTo);
        if (finalArrivalForTrips.isEmpty()) {
            throw new RuntimeException("Missing last arrival for " + trips);
        }
        finishTime = finalArrivalForTrips.get();
    }

    @Override
    public TramTime getStartTime() {
        if (startTime==null) {
            computeStartTime();
        }
        return startTime;
    }

    @Override
    public TramTime getFinishTime() {
        if (finishTime==null) {
            computeFinishTime();
        }
        return finishTime;
    }

}
