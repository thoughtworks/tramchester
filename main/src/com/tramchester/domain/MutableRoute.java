package com.tramchester.domain;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
    public boolean isDateOverlap(Route otherRoute) {
        if (services.isEmpty()) {
            throw new RuntimeException("Route has no services");
        }
        MutableRoute otherMutableRoute = (MutableRoute) otherRoute;
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
    public boolean isAvailableOn(LocalDate date) {
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
        private final Cache<IdFor<Route>, Boolean> overlaps; // for thread safety
        private final IdFor<Route> parentId;
        private final Route parent;
        private AggregateServiceCalendar serviceCalendar;

        private boolean loaded;

        RouteCalendar(Route parent) {
            this.parent = parent;
            this.parentId = parent.getId();
            loaded = false;
            overlaps = Caffeine.newBuilder().maximumSize(5000).
                    expireAfterAccess(10, TimeUnit.MINUTES).
                    initialCapacity(400).
                    recordStats().build();
        }

        public boolean isAvailableOn(LocalDate date) {
            loadFromParent();

            return serviceCalendar.operatesOn(date);
        }

        private void loadFromParent() {
            if (loaded) {
                return;
            }
            Set<ServiceCalendar> calendars = parent.getServices().stream().map(Service::getCalendar).collect(Collectors.toSet());
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

        public boolean anyOverlapInRunning(RouteCalendar otherCalendar) {
            loadFromParent();
            otherCalendar.loadFromParent();

            return overlaps.get(otherCalendar.parentId, item -> anyDateOverlaps(otherCalendar.serviceCalendar));

        }

        private boolean anyDateOverlaps(ServiceCalendar otherCalendar) {
            if (otherCalendar.isCancelled() || serviceCalendar.isCancelled()) {
                return false;
            }

            if (otherCalendar.operatesNoDays() || serviceCalendar.operatesNoDays()) {
                return false;
            }

            // working assumption, any additional dates are withing the overall specified range for a service
            if (!otherCalendar.getDateRange().overlapsWith(getDateRange())) {
                return false;
            }

            // additions

            if (operatesOnAny(serviceCalendar.getAdditions(), otherCalendar)) {
                return true;
            }

            if (operatesOnAny(otherCalendar.getAdditions(), serviceCalendar)) {
                return true;
            }

            // removed

            if (operatesNoneOf(serviceCalendar.getRemoved(), otherCalendar)) {
                return false;
            }

            if (operatesNoneOf(otherCalendar.getRemoved(), serviceCalendar)) {
                return false;
            }

            // operating days, any overlap?

            EnumSet<DayOfWeek> otherDays = EnumSet.copyOf(otherCalendar.getOperatingDays());
            return otherDays.removeAll(getOperatingDays()); // will be true only if any overlap
        }

        private boolean operatesNoneOf(Set<LocalDate> dates, ServiceCalendar calendar) {
            if (dates.isEmpty()) {
                return false;
            }
            return dates.stream().noneMatch(calendar::operatesOn);
        }

        private boolean operatesOnAny(Set<LocalDate> dates, ServiceCalendar calendar) {
            if (dates.isEmpty()) {
                return false;
            }
            return dates.stream().anyMatch(calendar::operatesOn);
        }
    }
}
