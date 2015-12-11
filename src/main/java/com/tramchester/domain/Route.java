package com.tramchester.domain;

import java.util.HashSet;
import java.util.Set;

public class Route {
    public static final String METROLINK = "MET";

    private String id;
    private String code;
    private String name;
    private String agency;
    private Set<Service> services = new HashSet<>();

    public Route(String id, String code, String name, String agency) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.agency = agency;
    }

    private Route() {
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
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

    public String getAgency() {
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
}
