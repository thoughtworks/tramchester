package com.tramchester.domain;


import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

public class Service implements HasId<Service>, GraphProperty {
    private static final Logger logger = LoggerFactory.getLogger(Service.class);

    // TODO make a composite ID
    private final IdFor<Service> serviceId;
    private final Set<Trip> trips;
    private final Set<Route> tripRoutes;
    private final Agency initialAgency;

    private ServiceCalendar calendar;

    private TramTime earliestDepart;
    private TramTime latestDepart;

    /***
     * Use version with agency
     */
    @Deprecated
    public Service(String serviceId, Route route) {
        this(StringIdFor.createId(serviceId), route);
    }

    public Service(String serviceId, Agency agency) {
        this(StringIdFor.createId(serviceId), agency);
    }

    /***
     * Use version with agency
     */
    @Deprecated
    public Service(IdFor<Service> serviceId, Route route) {
        this(serviceId, route.getAgency());
    }

    public Service(IdFor<Service> serviceId, Agency initialAgency) {
        this.serviceId = serviceId;
        this.trips = new LinkedHashSet<>();
        this.tripRoutes = new HashSet<>();
        this.initialAgency = initialAgency;

        earliestDepart = null;
        latestDepart = null;
        calendar = null;
    }

    public IdFor<Service> getId() {
        return serviceId;
    }

    @Deprecated
    public Set<Trip> getTrips() {
        return Collections.unmodifiableSet(trips);
    }

    @Deprecated
    public void addTrip(Trip trip) {
        Route tripRoute = trip.getRoute();
        Agency tripAgency = tripRoute.getAgency();
        if (!tripAgency.equals(initialAgency)) {
            String message = "TripId: " + trip.getId() + " Trip Agency " + tripAgency.getId() + " does not match route: " + initialAgency;
            logger.error(message);
            throw new RuntimeException(message);
        }
        tripRoutes.add(tripRoute);
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
                ", tripRoutes=" + HasId.asIds(tripRoutes) +
                ", trips=" + HasId.asIds(trips) +
                ", initialAgency=" + initialAgency +
                ", calendar=" + calendar +
                ", earliestDepart=" + earliestDepart +
                ", latestDepart=" + latestDepart +
                '}';
    }

    public Set<Route> getRoutes() {
        return tripRoutes;
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
