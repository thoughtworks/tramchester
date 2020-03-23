package com.tramchester.domain;

import java.util.*;

public class Agency {
    private final Set<Route> routes;
    private final String agencyName;

    public Agency(String agencyName) {
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
                "routes=" + HasId.asIds(routes) +
                ", agencyName='" + agencyName + '\'' +
                '}';
    }

    public String getName() {
        return agencyName;
    }
}
