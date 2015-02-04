package com.tramchester.domain;

public class RouteData {
    private String id;
    private String code;
    private String name;

    public RouteData(String id, String code, String name) {

        this.id = id;
        this.code = code;
        this.name = name;
    }

    private RouteData() {
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
}
