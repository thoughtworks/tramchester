package com.tramchester.domain;

import java.util.HashSet;
import java.util.Set;

public class Route {
    private String id;
    private String code;
    private String name;
    private Set<Service> services = new HashSet<>();

    public Route(String id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
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
}
