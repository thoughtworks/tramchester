package com.tramchester.domain;

import java.util.*;

public class Agency implements HasId<Agency> {
    private final Set<Route> routes;
    private final IdFor<Agency> agencyId;
    private final String agencyName;

    public Agency(String agencyId, String agencyName) {
        this.agencyId =  IdFor.createId(agencyId);
        this.agencyName = agencyName;
        routes = new HashSet<>();
    }

    public void addRoute(Route route) {
        routes.add(route);
    }

    public Collection<Route> getRoutes() {
        return routes;
    }

    @Override
    public String toString() {
        return "Agency{" +
                "agencyId='" + agencyId + '\'' +
                ", agencyName='" + agencyName + '\'' +
                ", routes=" + HasId.asIds(routes) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Agency agency = (Agency) o;
        return agencyId.equals(agency.agencyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agencyId);
    }

    public IdFor<Agency> getId() {
        return agencyId;
    }

    public String getName() {
        return agencyName;
    }
}
