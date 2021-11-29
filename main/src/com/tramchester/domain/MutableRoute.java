package com.tramchester.domain;

import com.google.common.collect.Sets;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.DateRange;
import com.tramchester.graph.GraphPropertyKey;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.TUESDAY;

public class MutableRoute implements Route {

    private final IdFor<Route> id;
    private final String shortName;
    private final String name;
    private final Agency agency;
    private final TransportMode transportMode;
    private final Set<Service> services;
    private final Set<Trip> trips;

    private DateRange dateRange;
    private EnumSet<DayOfWeek> operatingDays;

    public static final Route Walking;
    static {
            Walking = new MutableRoute(StringIdFor.createId("Walk"), "Walk", "Walk", MutableAgency.Walking,
                    TransportMode.Walk);
    }

    public MutableRoute(IdFor<Route> id, String shortName, String name, Agency agency, TransportMode transportMode) {
        this.id = id;
        this.shortName = shortName.intern();
        this.name = name.intern();

        this.agency = agency;
        this.transportMode = transportMode;
        services = new HashSet<>();
        trips  = new HashSet<>();

        dateRange = new DateRange(LocalDate.MAX, LocalDate.MIN);
        operatingDays = EnumSet.noneOf(DayOfWeek.class);
    }

    // test support
    public static Route getRoute(IdFor<Route> id, String shortName, String name, Agency agency, TransportMode transportMode) {
        return new MutableRoute(id, shortName, name, agency, transportMode);
    }

    @Override
    public IdFor<Route> getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<Service> getServices() {
        return services;
    }

    public void addTrip(Trip trip) {
        trips.add(trip);
    }

    public void addService(Service service) {
        services.add(service);
        if (!service.hasCalendar()) {
            throw new RuntimeException("Service must have calendar to add service");
        }
        ServiceCalendar calendar = service.getCalendar();
        this.operatingDays = EnumSet.copyOf(Sets.union(operatingDays, calendar.getOperatingDays()));
        DateRange otherRange = calendar.getDateRange();
        this.dateRange = DateRange.broadest(this.dateRange, otherRange);
    }

    @Override
    public Agency getAgency() {
        return agency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MutableRoute route = (MutableRoute) o;

        return !(id != null ? !id.equals(route.id) : route.id != null);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String getShortName() {
        return shortName;
    }

    @Override
    public TransportMode getTransportMode() {
        return transportMode;
    }

    @Override
    public String toString() {
        return "Route{" +
                "id=" + id +
                ", shortName='" + shortName + '\'' +
                ", name='" + name + '\'' +
                ", agency=" + agency.getName() +
                ", transportMode=" + transportMode +
                ", services=" + HasId.asIds(services) +
                ", trips=" + HasId.asIds(trips) +
                '}';
    }

    @Override
    public GraphPropertyKey getProp() {
        return GraphPropertyKey.ROUTE_ID;
    }

    @Override
    public Set<Trip> getTrips() {
        return trips;
    }

    @Override
    public boolean isDateOverlap(Route otherRoute) {
        if (services.isEmpty()) {
            throw new RuntimeException("Route has no services");
        }
        if (Sets.intersection(operatingDays, otherRoute.getOperatingDays()).isEmpty()) {
            return false;
        }
        return dateRange.overlapsWith(otherRoute.getDateRange());
    }

    @Override
    public EnumSet<DayOfWeek> getOperatingDays() {
        return operatingDays;
    }

    @Override
    public DateRange getDateRange() {
        return dateRange;
    }
}
