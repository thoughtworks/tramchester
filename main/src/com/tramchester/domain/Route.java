package com.tramchester.domain;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphPropertyKey;

import java.util.HashSet;
import java.util.Set;

public class Route implements RouteReadOnly {

    private final IdFor<RouteReadOnly> id;
    private final String shortName;
    private final String name;
    private final Agency agency;
    private final TransportMode transportMode;
    private final Set<Service> services;
    private final Set<Trip> trips;

    public static final RouteReadOnly Walking;
    static {
            Walking = new Route(StringIdFor.createId("Walk"), "Walk", "Walk", Agency.Walking,
                    TransportMode.Walk);
    }

    @Deprecated
    public Route(String id, String shortName, String name, Agency agency, TransportMode transportMode) {
        this(StringIdFor.createId(id), shortName, name, agency, transportMode);
    }

    public Route(IdFor<RouteReadOnly> id, String shortName, String name, Agency agency, TransportMode transportMode) {
        this.id = id;
        this.shortName = shortName.intern();
        this.name = name.intern();

        this.agency = agency;
        this.transportMode = transportMode;
        services = new HashSet<>();
        trips  = new HashSet<>();
    }

    @Override
    public IdFor<RouteReadOnly> getId() {
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
    }

    @Override
    public Agency getAgency() {
        return agency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Route route = (Route) o;

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
}
