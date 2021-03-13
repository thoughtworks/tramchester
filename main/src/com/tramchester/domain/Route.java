package com.tramchester.domain;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.reference.RouteDirection;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphPropertyKey;

import java.util.HashSet;
import java.util.Set;

public class Route implements HasId<Route>, HasTransportMode, GraphProperty {

    private final IdFor<Route> id;
    private final String shortName;
    private final String name;
    private final Agency agency;
    private final TransportMode transportMode;
    private final Set<Service> services;
    private final Set<String> headsigns;

    public static final Route Walking;
    static {
            Walking = new Route(StringIdFor.createId("Walk"), "Walk", "Walk", Agency.Walking,
                    TransportMode.Walk);
    }

    public Route(String id, String shortName, String name, Agency agency, TransportMode transportMode) {
        this(StringIdFor.createId(id), shortName, name, agency, transportMode);
    }

    public Route(IdFor<Route> id, String shortName, String name, Agency agency, TransportMode transportMode) {
        this.id = id;
        this.shortName = shortName.intern();
        this.name = name.intern();

        this.agency = agency;
        this.transportMode = transportMode;
        services = new HashSet<>();
        headsigns = new HashSet<>();
    }

    public IdFor<Route> getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<Service> getServices() {
        return services;
    }

    public void addService(Service service) {
        services.add(service);
    }

    public Agency getAgency() {
        return agency;
    }

    public Set<String> getHeadsigns() {
        return headsigns;
    }

    public void addHeadsign(String headsign) {
        headsigns.add(headsign);
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

    public String getShortName() {
        return shortName;
    }

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
                ", headsigns=" + headsigns +
                '}';
    }

    @Override
    public GraphPropertyKey getProp() {
        return GraphPropertyKey.ROUTE_ID;
    }

}
