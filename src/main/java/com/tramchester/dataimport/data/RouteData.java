package com.tramchester.dataimport.data;

import java.util.Objects;

public class RouteData {
    private String id;
    private String code;
    private String name;
    private String agency;

    public RouteData(String id, String code, String name, String agency) {
        this.id = id.intern();
        this.code = code;
        this.name = name;
        this.agency = agency;
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

    public String getAgency() {
        return agency;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteData routeData = (RouteData) o;
        return Objects.equals(id, routeData.id);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id);
    }
}
