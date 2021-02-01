package com.tramchester.domain;


import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.rmi.ServerError;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class Service implements HasId<Service>, GraphProperty {
    private static final Logger logger = LoggerFactory.getLogger(Service.class);

    private final IdFor<Service> serviceId;
    private final Route route;
    private final Set<Trip> trips;
    private final Agency initialAgency;

    private ServiceCalendar calendar;

    private TramTime earliestDepart;
    private TramTime latestDepart;

    public Service(String serviceId, Route route) {
        this(IdFor.createId(serviceId), route);
    }

    public Service(IdFor<Service> serviceId, Route route) {
        this.serviceId = serviceId;
        this.route = route;
        this.trips = new LinkedHashSet<>();
        this.initialAgency = route.getAgency();

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
        if (!route.equals(trip.getRoute())) {
            String message = "Service route " + route+ " does not match trip route: " + trip.getRoute();
            logger.error(message);
            throw new RuntimeException(message);
        }
        trips.add(trip); // stop population not done at this stage, see updateTimings
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
                ", route=" + route.getId() +
                ", trips=" + HasId.asIds(trips) +
                ", initialAgency=" + initialAgency +
                ", calendar=" + calendar +
                ", earliestDepart=" + earliestDepart +
                ", latestDepart=" + latestDepart +
                '}';
    }

    /***
     * use getRoute()
     */
    @Deprecated
    public Set<Route> getRoutes() {
        return Collections.singleton(route);
    }

    public Route getRoute() {
        return route;
    }

    /***
     * use getTrips()
     */
    @Deprecated
    public Set<Trip> getTripsFor(Route route) {
        return trips.stream().filter(trip->trip.getRoute().equals(route)).collect(Collectors.toSet());
    }

    public Set<Trip> getTrips() {
        return Collections.unmodifiableSet(trips);
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
