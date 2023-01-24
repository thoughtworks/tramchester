package com.tramchester.domain;

import com.tramchester.domain.dates.AggregateServiceCalendar;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.ServiceCalendar;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphPropertyKey;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class MutableRoute implements Route {

    private final IdFor<Route> id;
    private final String shortName;
    private final String name;
    private final Agency agency;
    private final TransportMode transportMode;
    private final Set<Service> services;
    private final Set<Trip> trips;

    private final RouteCalendar routeCalendar;
    private Boolean intoNextDay;

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

        routeCalendar = new RouteCalendar(this);
        intoNextDay = null;
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
        // can't check this due to data load order
//        if (!service.hasCalendar()) {
//            throw new RuntimeException("Service must have calendar to add service");
//        }
    }

    @Override
    public Agency getAgency() {
        return agency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MutableRoute that = (MutableRoute) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
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
        return "MutableRoute{" +
                "id=" + id +
                ", shortName='" + shortName + '\'' +
                ", name='" + name + '\'' +
                ", agency=" + agency.getId() +
                ", transportMode=" + transportMode +
                ", services=" + HasId.asIds(services) +
                ", trips=" +  HasId.asIds(trips) +
                ", serviceDateCache=" + routeCalendar +
                ", intoNextDay=" + intoNextDay() +
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
    public boolean isDateOverlap(final Route otherRoute) {
        if (services.isEmpty()) {
            throw new RuntimeException("Route has no services");
        }
        final MutableRoute otherMutableRoute = (MutableRoute) otherRoute;
        return routeCalendar.anyOverlapInRunning(otherMutableRoute.routeCalendar);
    }

    /***
     * Use with Caution: remember does no include Additional or Excluded days, use isAvailableOn() to validate a specific date
     * @return Days of week this route Normally operates (not including exclusions, or additional days)
     */
    @Override
    public EnumSet<DayOfWeek> getOperatingDays() {
        return routeCalendar.getOperatingDays();
    }

    @Override
    public DateRange getDateRange() {
        return routeCalendar.getDateRange();
    }

    @Override
    public boolean isAvailableOn(TramDate date) {
        return routeCalendar.isAvailableOn(date);
    }

    @Override
    public boolean intoNextDay() {
        if (intoNextDay==null) {
            intoNextDay = trips.stream().anyMatch(Trip::intoNextDay);
        }
        return intoNextDay;
    }

    private static class RouteCalendar {
        private final Route parent;
        private AggregateServiceCalendar serviceCalendar;

        private boolean loaded;

        RouteCalendar(Route parent) {
            this.parent = parent;
            loaded = false;
        }

        public boolean isAvailableOn(TramDate date) {
            loadFromParent();

            return serviceCalendar.operatesOn(date);
        }

        private void loadFromParent() {
            if (loaded) {
                return;
            }
            final Set<ServiceCalendar> calendars = parent.getServices().stream().
                    map(Service::getCalendar).
                    collect(Collectors.toSet());
            serviceCalendar = new AggregateServiceCalendar(calendars);
            loaded = true;
        }

        public EnumSet<DayOfWeek> getOperatingDays() {
            loadFromParent();
            return serviceCalendar.getOperatingDays();
        }

        public DateRange getDateRange() {
            loadFromParent();
            return serviceCalendar.getDateRange();
        }

        public boolean anyOverlapInRunning(final RouteCalendar otherCalendar) {
            loadFromParent();
            otherCalendar.loadFromParent();

            return serviceCalendar.anyDateOverlaps(otherCalendar.serviceCalendar);
        }

        @Override
        public String toString() {
            return "RouteCalendar{" +
                    "parent=" + parent.getId() +
                    ", serviceCalendar=" + serviceCalendar +
                    ", loaded=" + loaded +
                    '}';
        }
    }
}
