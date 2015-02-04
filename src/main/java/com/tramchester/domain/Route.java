package com.tramchester.domain;

import java.util.ArrayList;
import java.util.List;

public class Route {
    private String id;
    private String code;
    private String name;
    private List<Service> services = new ArrayList<>();

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

    public List<Service> getServices() {
        return services;
    }

    public void addService(Service service) {
        services.add(service);
    }
}
