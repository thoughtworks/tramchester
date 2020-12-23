package com.tramchester.domain;


import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Service implements HasId<Service>, GraphProperty {

    private final IdFor<Service> serviceId;
    private final Set<Route> routes;
    private final Set<Trip> trips;

    private ServiceCalendar calendar;

    private TramTime earliestDepart;
    private TramTime latestDepart;

    public Service(String serviceId, Route route) {
        this(IdFor.createId(serviceId), route);
    }

    public Service(IdFor<Service> serviceId, Route route) {
        this.serviceId = serviceId;
        this.routes = new HashSet<>();
        this.routes.add(route);
        this.trips = new LinkedHashSet<>();
        earliestDepart = null;
        latestDepart = null;
        calendar = null;
    }

    public IdFor<Service> getId() {
        return serviceId;
    }

    public Set<Trip> getAllTrips() {
        return trips;
    }

    public void addTrip(Trip trip) {
        if (!routes.contains(trip.getRoute())) {
            throw new RuntimeException("Service does not contain route " + trip.getRoute());
        }
        trips.add(trip); // stop population not done at this stage, see updateTimings
    }

    public void addRoute(Route route) {
        routes.add(route);
    }

    public void updateTimings() {
        trips.forEach(this::updateEarliestAndLatest);
    }

    private void updateEarliestAndLatest(Trip trip) {
        TramTime tripEarliest = trip.earliestDepartTime();
        if (earliestDepart==null) {
            earliestDepart = tripEarliest;
        } else if (tripEarliest.isBefore(earliestDepart)) {
            earliestDepart = tripEarliest;
        }

        TramTime tripLatest = trip.latestDepartTime();
        if (latestDepart==null) {
            latestDepart = tripLatest;
        } else if (tripLatest.isAfter(latestDepart)) {
            latestDepart = tripLatest;
        }
    }

    @Override
    public String toString() {
        return "Service{" +
                "serviceId=" + serviceId +
                ", routes=" + routes +
                ", trips=" + trips +
                ", calendar=" + calendar +
                ", earliestDepart=" + earliestDepart +
                ", latestDepart=" + latestDepart +
                '}';
    }

    public Set<Route> getRoutes() {
        return routes;
    }

    public Set<Trip> getTripsFor(Route route) {
        return trips.stream().filter(trip->trip.getRoute().equals(route)).collect(Collectors.toSet());
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

    public TramTime earliestDepartTime() {
        return earliestDepart;
    }

    public TramTime latestDepartTime() {
        return latestDepart;
    }

    public void summariseDates(PrintStream printStream) {
        printStream.printf("Earliest: %s Latest: %s%n", earliestDepartTime().toPattern(), latestDepartTime().toPattern());
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
